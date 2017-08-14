/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.LispCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispConfigCommandFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispNotFoundException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.EndpointHost;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.LispState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.Constants;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.servers.grouping.map.servers.MapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Created by Shakib Ahmed on 3/29/17.
 */
public class LispStateManager {
    private static final Logger LOG = LoggerFactory.getLogger(LispStateManager.class);

    private HostRelatedInfoContainer hostRelatedInfoContainer;
    private MountedDataBrokerProvider mountedDataBrokerProvider;
    private ConfigManagerHelper lispStateHelper;

    private NeutronTenantToVniMapper neutronTenantToVniMapper;

    private static final short DEFAULT_PRIORITY = 1;
    private static final short DEFAULT_WEIGHT = 1;
    public static final String DEFAULT_XTR_KEY = "admin";
    public static final String DEFAULT_LOCATOR_SET_NAME_PREFIX = "LS";
    public static final String DEFAULT_MAPPINGRECORD_NAME_PREFIX = "MR_";

    public LispStateManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        Preconditions.checkNotNull(mountedDataBrokerProvider,
                "MountedDataBrokerProvider found to be null!");
        hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();
        this.mountedDataBrokerProvider= mountedDataBrokerProvider;
        this.lispStateHelper = new ConfigManagerHelper(this.mountedDataBrokerProvider);
        neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    }

    public synchronized void processCreateEndPoint(AddressEndpointWithLocation addressEp) {
        try {
            if (lispStateHelper.isMetadataPort(addressEp)) {
                return;
            }
            EndpointHost endpointHost = lispStateHelper.getEndpointHostInformation(addressEp);
            LispState lispStateOfHost = configureHostIfNeeded(endpointHost);

            long vni = getVni(addressEp.getTenant().getValue());
            long vrf = vni;

            Eid eid = lispStateHelper.getEid(addressEp, vni);
            String eidMappingName = lispStateHelper.constructEidMappingName(addressEp);

            addVniToVrfMappingIfNeeded(endpointHost, lispStateOfHost, vni, vrf);

            if (lispStateHelper.getInterfaceIp(addressEp).getValue().equals(Constants.METADATA_IP)) {
                return;
            }

            addEidOnHostIfNeeded(endpointHost, lispStateOfHost, eid, eidMappingName);
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("Lisp endpoint configuration failed for address endpoint: {}", addressEp);
        }
    }

    private void addEidOnHostIfNeeded(EndpointHost endpointHost, LispState lispStateOfNode, Eid eid,
                                      String eidMappingName)
            throws LispConfigCommandFailedException {
        if(!lispStateOfNode.eidSetContains(eid)) {
            addEidInEidTable(endpointHost, lispStateOfNode, eid, eidMappingName);
        }
    }

    private synchronized LispState configureHostIfNeeded(EndpointHost endpointHost)
            throws LispConfigCommandFailedException {
        LispState lispStateOfHost = hostRelatedInfoContainer.getLispStateOfHost(endpointHost.getHostName());

        if (lispStateOfHost == null) {
            LOG.debug("Configuring host {} for LISP", endpointHost.getHostName());
            lispStateOfHost = new LispState();
            try {
                enableLispOnHost(endpointHost, lispStateOfHost);
                enableGpeOnHostIfNeeded(endpointHost, lispStateOfHost);
                addLocatorSetOnHost(endpointHost, lispStateOfHost);
                addMapResolverOnHost(endpointHost, lispStateOfHost);
                enableMapRegistrationOnHostIfNeeded(endpointHost, lispStateOfHost);

                hostRelatedInfoContainer.setLispStateOfHost(endpointHost.getHostName(), lispStateOfHost);
            } catch (LispNotFoundException e) {
                LOG.warn("Lisp host configuration failed: ", e.getMessage());
                throw new LispConfigCommandFailedException("Failed LISP configuration!");
            }
        }
        return lispStateOfHost;
    }

    private void enableGpeOnHostIfNeeded(EndpointHost endpointHost, LispState lispStateOfHost)
            throws LispConfigCommandFailedException {
        if (ConfigUtil.getInstance().isL3FlatEnabled()) {
            enableGpeForHost(endpointHost, lispStateOfHost);
        }
    }

    private void enableMapRegistrationOnHostIfNeeded(EndpointHost endpointHost, LispState lispStateOfHost)
            throws LispConfigCommandFailedException {
        if (ConfigUtil.getInstance().isLispMapRegisterEnabled()) {
            enableMapRegister(endpointHost);
            addMapServer(endpointHost, lispStateOfHost);
        }
    }

    private void enableLispOnHost(EndpointHost endpointHost, LispState lispState)
            throws LispConfigCommandFailedException {
        AbstractLispCommand<Lisp> lispEnableCommand = LispCommandWrapper.enableLisp();
        if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), lispEnableCommand)) {
            lispState.setLispEnabled(true);
        } else {
            throw new LispConfigCommandFailedException("Lisp Enable Command failed execution!");
        }
    }

    private void enableGpeForHost(EndpointHost endpointHost, LispState lispState)
            throws LispConfigCommandFailedException {
        AbstractLispCommand<GpeFeatureData> gpeEnableCommand = LispCommandWrapper.enableGpe();
        if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), gpeEnableCommand)) {
            lispState.setGpeEnabled(true);
        } else {
            throw new LispConfigCommandFailedException("GPE Enable Command failed execution!");
        }
    }

    private void addLocatorSetOnHost(EndpointHost endpointHost, LispState lispState)
            throws LispNotFoundException, LispConfigCommandFailedException {
        try {
            String locatorSetName = lispStateHelper.constructLocatorSetName(lispState.getLocatorCount());
            String lispDataInterfaceName = lispStateHelper
                    .getLispDataRlocInterfaceName(endpointHost.getHostName()).get();
            AbstractLispCommand<LocatorSet> addLocatorSetCommand = LispCommandWrapper.addLocatorSet(locatorSetName,
                    lispDataInterfaceName, DEFAULT_PRIORITY, DEFAULT_WEIGHT);
            if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addLocatorSetCommand)) {
                lispState.setLocIntfToLocSetNameMapping(lispDataInterfaceName, locatorSetName);
            } else {
                throw new LispConfigCommandFailedException("Lisp add locator set failed for host "
                        + endpointHost.getHostName() + " and locator interface " + lispDataInterfaceName);
            }

            addExtraItrRlocLocatorSetIfNeeded(endpointHost, lispDataInterfaceName);
        } catch (InterruptedException | ExecutionException e) {
            throw new LispNotFoundException("No interface with Ip Address found!");
        }
    }

    private void addExtraItrRlocLocatorSetIfNeeded(EndpointHost endpointHost, String lispDataInterfaceName)
            throws LispNotFoundException, LispConfigCommandFailedException {
        String lispCpRlocInterfaceName = lispStateHelper.getLispCpRlocInterfaceName(endpointHost);
        if (lispCpRlocInterfaceName == null
                || lispCpRlocInterfaceName.isEmpty()
                || lispCpRlocInterfaceName.equals(lispDataInterfaceName)) {
            return;
        }

        addItrLocatorSet(endpointHost, lispCpRlocInterfaceName);
    }

    private void addItrLocatorSet(EndpointHost endpointHost, String lispCpInterfaceName)
            throws LispNotFoundException, LispConfigCommandFailedException {
        String locatorSetName = lispStateHelper.constructLocatorSetNameForItrRloc();
        AbstractLispCommand<LocatorSet> addLocatorSetCommand = LispCommandWrapper.addLocatorSet(locatorSetName,
                lispCpInterfaceName, DEFAULT_PRIORITY, DEFAULT_WEIGHT);
        if (!LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addLocatorSetCommand)) {
            throw new LispConfigCommandFailedException("Lisp add locator set failed for host "
                    + endpointHost.getHostName() + " and locator interface " + lispCpInterfaceName);
        }

        AbstractLispCommand<ItrRemoteLocatorSet> addItrRlocCommand = LispCommandWrapper.addItrRloc(locatorSetName);
        if (!LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addItrRlocCommand)) {
            throw new LispConfigCommandFailedException("Lisp add Itr Rloc command failed for host "
                    + endpointHost.getHostName() + " and locator set " + locatorSetName);
        }
    }

    private void addMapResolverOnHost(EndpointHost endpointHost, LispState lispState)
            throws LispConfigCommandFailedException {
        IpAddress mapResolverIpAddress = ConfigUtil.getInstance().getOdlIp();
        Preconditions.checkNotNull(mapResolverIpAddress, "Map Resolver ip not properly configured!");

        AbstractLispCommand<MapResolver> addMapResolverCommand = LispCommandWrapper.
                addMapResolver(mapResolverIpAddress);
        if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addMapResolverCommand)) {
            lispState.addInMapResolverSet(mapResolverIpAddress);
        } else {
            throw new LispConfigCommandFailedException("Lisp add map resolver for host " + endpointHost.getHostName()
                    + " failed for ODL ip " + mapResolverIpAddress);
        }
    }

    private void enableMapRegister(EndpointHost endpointHost)
            throws LispConfigCommandFailedException {
        AbstractLispCommand<MapRegister> enableMapRegisterCommand = LispCommandWrapper.enableMapRegister();

        if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), enableMapRegisterCommand)) {
        } else {
            throw new LispConfigCommandFailedException("Lisp enable mapregistration for host "
                    + endpointHost.getHostName() + " failed!");
        }

    }

    private void addMapServer(EndpointHost endpointHost, LispState lispState) throws LispConfigCommandFailedException {
        IpAddress mapServerIpAddress = ConfigUtil.getInstance().getOdlIp();
        Preconditions.checkNotNull(mapServerIpAddress, "Mapserver ip not properly configured!");
        AbstractLispCommand<MapServer> addMapServerCommand = LispCommandWrapper.addMapServer(mapServerIpAddress);

        if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addMapServerCommand)) {
            lispState.addInMapServerSet(mapServerIpAddress);
        } else {
            throw new LispConfigCommandFailedException("Lisp add map server for host " + endpointHost.getHostName()
                    + " failed for ODL ip " + mapServerIpAddress);
        }
    }

    private void addVniToVrfMappingIfNeeded(EndpointHost endpointHost,
                                            LispState lispState,
                                            long vni, long vrf) throws LispConfigCommandFailedException {
        if (!lispState.isVniConfigured(vni)) {
            AbstractLispCommand<VniTable> addVniToVrfMapping = LispCommandWrapper.mapVniToVrf(vni, vrf);
            addVniToVrfMapping.setOptions(General.Operations.PUT);
            if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addVniToVrfMapping)) {
                lispState.addInVniSet(vni);
            } else {
                throw new LispConfigCommandFailedException("Lisp add vrf " + vrf +" for vni " +vni
                        + " command failed!");
            }
        }
    }

    private void addEidInEidTable(EndpointHost endpointHost,
                                  LispState lispState,
                                  Eid eid,
                                  String eidMappingName) throws LispConfigCommandFailedException {
        AbstractLispCommand<LocalMapping> addLocalMappingInEidTableCommand = LispCommandWrapper
                .addLocalMappingInEidTable(eidMappingName,
                        eid,
                        lispStateHelper.getFirstLocatorSetName(lispState),
                        lispStateHelper.getDefaultHmacKey());
        if (LispStateCommandExecutor.executePutCommand(endpointHost.getHostName(), addLocalMappingInEidTableCommand)) {
            lispState.addEidInEidSet(eid);
        } else {
            throw new LispConfigCommandFailedException("Lisp add local mapping for eid " + eid + "failed!");
        }
    }

    public synchronized void processDeleteEndpoint(AddressEndpointWithLocation addressEp) {
        try {

            if (lispStateHelper.isMetadataPort(addressEp)) {
                return;
            }

            EndpointHost endpointHost = lispStateHelper.getEndpointHostInformation(addressEp);

            LispState lispState = hostRelatedInfoContainer.getLispStateOfHost(endpointHost.getHostName());

            if (lispState == null) {
                LOG.debug("Endpoint not configured for LISP. EndPoint: {}", addressEp);
            } else {
                long vni = getVni(addressEp.getTenant().getValue());
                Eid eid = lispStateHelper.getEid(addressEp, vni);
                String eidMappingName = lispStateHelper.constructEidMappingName(addressEp);

                deleteEidFromLocalEidTableOfHostIfNeeded(endpointHost, lispState, eid, eidMappingName);

                if (lispState.eidCount() == 0) {
                    deleteLispStatesFromHost(endpointHost);
                    deleteNativeForwardPathsTables(endpointHost);
                }
            }
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("Lisp command execution failed: {}", e.getMessage());
        }
    }

    private void deleteEidFromLocalEidTableOfHostIfNeeded(EndpointHost endpointHost, LispState lispState, Eid eid, String eidMappingName) throws LispConfigCommandFailedException {
        if (lispState.eidSetContains(eid)) {
            deleteEidFromLocalEidTableOfHost(endpointHost, lispState, eid, eidMappingName);
        }
    }

    private void deleteEidFromLocalEidTableOfHost(EndpointHost endpointHost,
                                       LispState lispState,
                                       Eid eid,
                                       String eidMappingName) throws LispConfigCommandFailedException {
        long value = eid.getVirtualNetworkId().getValue();

        AbstractLispCommand<LocalMapping> deleteLocalMappingCommand = LispCommandWrapper
                .deleteLocalMappingFromEidTable(eidMappingName, value);

        if (LispStateCommandExecutor
                .executeDeleteCommand(endpointHost.getHostName(), deleteLocalMappingCommand)) {
            LOG.debug("Successfully deleted eid {} from host {}", eid, endpointHost.getHostName());
            lispState.deleteEid(eid);
        } else {
            throw new LispConfigCommandFailedException("Lisp delete local mapping command failed!");
        }
    }


    private void deleteLispStatesFromHost(EndpointHost endpointHost) throws LispConfigCommandFailedException {
        AbstractLispCommand<LispFeatureData> deleteLispFeatureData = LispCommandWrapper.deleteLispFeatureData();

        if (LispStateCommandExecutor.executeDeleteCommand(endpointHost.getHostName(), deleteLispFeatureData)) {
            hostRelatedInfoContainer.deleteLispStateOfHost(endpointHost.getHostName());
            LOG.debug("Deleted all lisp data for host {}", endpointHost.getHostName());
        } else {
            throw new LispConfigCommandFailedException("Lisp delete feature data command failed!");
        }
    }

    private void deleteNativeForwardPathsTables(EndpointHost endpointHost)
            throws LispConfigCommandFailedException {
        AbstractLispCommand<NativeForwardPathsTables> deleteNativeForwardPathsTables = LispCommandWrapper
                .deleteNativeForwardPathsTables();

        if (!LispStateCommandExecutor.executeDeleteCommand(endpointHost.getHostName(),
                deleteNativeForwardPathsTables)) {
            throw new LispConfigCommandFailedException("Delete Native Forward Paths Tables command failed!");
        }
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }
}
