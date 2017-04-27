/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.bvi;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.LoopbackCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.GbpSubnetEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispConfigCommandFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.GbpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Shakib Ahmed on 4/26/17.
 */
public class BviManager {
    private static final Logger LOG = LoggerFactory.getLogger(BviManager.class);

    private ConfigManagerHelper managerHelper;

    private HashMap<String, GbpSubnet> subnetInformation;

    private BviHostSpecificInfo bviHostSpecificInfo;
    private NeutronTenantToVniMapper neutronTenantToVniMapper;

    public BviManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        managerHelper = new ConfigManagerHelper(mountedDataBrokerProvider);
        subnetInformation = new HashMap<>();
        bviHostSpecificInfo = new BviHostSpecificInfo();
        neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    }

    @Subscribe
    public synchronized void gbpSubnetChanged(GbpSubnetEvent event) {
        final GbpSubnet oldGbpSubnet = event.getBefore().orNull();
        final GbpSubnet newGbpSubnet = event.getAfter().orNull();
        switch (event.getDtoModificationType()) {
            case CREATED:
                Preconditions.checkNotNull(newGbpSubnet);
                processSubnetCreated(newGbpSubnet.getId(), newGbpSubnet);
                break;
            case UPDATED:
                Preconditions.checkNotNull(oldGbpSubnet);
                Preconditions.checkNotNull(newGbpSubnet);
                processSubnetDeleted(oldGbpSubnet.getId());
                processSubnetCreated(newGbpSubnet.getId(), newGbpSubnet);
                break;
            case DELETED:
                Preconditions.checkNotNull(oldGbpSubnet);
                processSubnetDeleted(oldGbpSubnet.getId());
                break;
        }
    }

    private void processSubnetCreated(String subnetUuid, GbpSubnet subnetInfo) {
        subnetInformation.put(subnetUuid, subnetInfo);
    }

    private void processSubnetDeleted(String subnetUuid) {
        subnetInformation.remove(subnetUuid);

        deleteBviIfExists(subnetUuid);
    }

    public GbpSubnet getSubnetInfo(String subnetUuid) {
        return subnetInformation.get(subnetUuid);
    }

    public void createBviIfNecessary(AddressEndpointWithLocation addressEp,
                                     String bridgeDomainName) {
        try {
            DataBroker vppDataBroker = managerHelper.getPotentialExternalDataBroker(addressEp).get();
            String hostName = managerHelper.getHostName(addressEp).get();
            String subnetUuid = managerHelper.getSubnet(addressEp);

            if (bviHostSpecificInfo.bviAlreadyExists(hostName, subnetUuid)) {
                return;
            }

            GbpSubnet gbpSubnetInfo = Preconditions.checkNotNull(getSubnetInfo(subnetUuid),
                    "Subnet UUID {} hasn't been created yet!", subnetUuid);

            LoopbackCommand.LoopbackCommandBuilder loopbackCommandBuilder =
                    new LoopbackCommand.LoopbackCommandBuilder();
            loopbackCommandBuilder.setInterfaceName("loop"
                    + bviHostSpecificInfo.getBviCount(hostName));
            loopbackCommandBuilder.setBridgeDomain(bridgeDomainName);
            loopbackCommandBuilder.setBvi(true);
            loopbackCommandBuilder.setEnabled(true);
            loopbackCommandBuilder.setVrfId(getVni(addressEp.getTenant().getValue()));
            loopbackCommandBuilder.setIpAddress(gbpSubnetInfo.getGatewayIp());
            loopbackCommandBuilder.setIpPrefix(gbpSubnetInfo.getCidr());
            loopbackCommandBuilder.setOperation(General.Operations.PUT);
            createBviInterface(hostName, vppDataBroker, loopbackCommandBuilder);
        } catch (LispConfigCommandFailedException e) {
            LOG.debug("LISP loopback command failed for {}", e.getMessage());
        }
    }

    public void createBviInterface(String hostName, DataBroker vppDataBroker,
                                   LoopbackCommand.LoopbackCommandBuilder commandBuilder) throws LispConfigCommandFailedException {

        if (GbpNetconfTransaction.netconfSyncedWrite(vppDataBroker,
                                                     commandBuilder.build(),
                                                     GbpNetconfTransaction.RETRY_COUNT)) {
            bviHostSpecificInfo.addBviForHost(hostName,
                                              commandBuilder.getBridgeDomain(),
                                              commandBuilder.getInterfaceName());
        } else {
            throw new LispConfigCommandFailedException("BVI could not be created for "
                    + hostName + " and bridge domain " + commandBuilder.getBridgeDomain());
        }
    }

    public void deleteBviIfExists(String subnetUuid) {

        List<String> hostsWithSubnet = bviHostSpecificInfo.getHostsWithSubnet(subnetUuid);

        hostsWithSubnet.forEach(host -> {
            DataBroker vppDataBroker = managerHelper.getPotentialExternalDataBroker(host).get();
            String bviInterfaceName = bviHostSpecificInfo.getInterfaceNameForBviInHost(host, subnetUuid);
            GbpNetconfTransaction.netconfSyncedDelete(vppDataBroker,
                    VppIidFactory.getInterfaceIID(new InterfaceKey(bviInterfaceName)), GbpNetconfTransaction.RETRY_COUNT);
        });
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }
}
