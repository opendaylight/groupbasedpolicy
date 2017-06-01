/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.LispCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispConfigCommandFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispNotFoundException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by Shakib Ahmed on 3/29/17.
 */
public class LispStateManager {
    private static final Logger LOG = LoggerFactory.getLogger(LispStateManager.class);

    private HashMap<String, LispState> lispStateMapper;
    private MountedDataBrokerProvider mountedDataBrokerProvider;
    private ConfigManagerHelper lispStateHelper;

    private NeutronTenantToVniMapper neutronTenantToVniMapper;

    private static final short DEFAULT_PRIORITY = 1;
    private static final short DEFAULT_WEIGHT = 1;
    public static final String DEFAULT_XTR_KEY = "admin";
    public static final String DEFAULT_LOCATOR_SET_NAME_PREFIX = "LS";
    public static final String DEFAULT_MAPPINGRECORD_NAME_PREFIX = "MR";

    public LispStateManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        Preconditions.checkNotNull(mountedDataBrokerProvider,
                "MountedDataBrokerProvider found to be null!");
        lispStateMapper = new HashMap<>();
        this.mountedDataBrokerProvider= mountedDataBrokerProvider;
        this.lispStateHelper = new ConfigManagerHelper(this.mountedDataBrokerProvider);
        neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    }

    public synchronized void configureEndPoint(AddressEndpointWithLocation addressEp) {
        try {
            DataBroker dataBroker = lispStateHelper.getPotentialExternalDataBroker(addressEp).get();
            String hostName = lispStateHelper.getHostName(addressEp).get();
            LispState lispStateOfNode = configureHostIfNeeded(hostName, dataBroker);

            long vni = getVni(addressEp.getTenant().getValue());
            long vrf = vni;
            addVniToVrfMappingIfNeeded(dataBroker, lispStateOfNode, vni, vrf);

            Eid eid = lispStateHelper.getEid(addressEp, vni);

            if(!lispStateOfNode.eidSetContains(eid)) {
                addEidInEidTable(dataBroker, lispStateOfNode, eid);
            }

        } catch (LispConfigCommandFailedException e) {
            LOG.warn("Lisp endpoint configuration failed for address endpoint: {}", addressEp);
        }
    }

    public synchronized LispState configureHostIfNeeded(String hostName, DataBroker vppDataBroker) throws LispConfigCommandFailedException {
        LispState lispStateOfNode = lispStateMapper.get(hostName);

        if (lispStateOfNode == null) {
            lispStateOfNode = new LispState(hostName);
            try {
                enableLispForNode(vppDataBroker, lispStateOfNode);

                if (ConfigUtil.getInstance().isL3FlatEnabled()) {
                    enableGpeForNode(vppDataBroker, lispStateOfNode);
                }

                addLocatorSet(vppDataBroker, lispStateOfNode);
                addMapResolver(vppDataBroker, lispStateOfNode);
                if (ConfigUtil.getInstance().isLispMapRegisterEnabled()) {
                    enableMapRegister(vppDataBroker, lispStateOfNode);
                    addMapServer(vppDataBroker, lispStateOfNode);
                }
                lispStateMapper.put(hostName, lispStateOfNode);
            } catch (LispNotFoundException e) {
                LOG.warn("Lisp host configuration failed: ", e.getMessage());
                throw new LispConfigCommandFailedException("Failed LISP configuration!");
            }
        }
        return lispStateOfNode;
    }

    public synchronized void deleteLispConfigurationForEndpoint(AddressEndpointWithLocation addressEp) {
        try {
            DataBroker vppDataBroker = lispStateHelper.getPotentialExternalDataBroker(addressEp).get();
            String hostName = lispStateHelper.getHostName(addressEp).get();

            LispState lispState = lispStateMapper.get(hostName);

            if (lispState == null) {
                LOG.debug("Endpoint not configured for LISP. EndPoint: {}", addressEp);
            } else {
                long vni = getVni(addressEp.getTenant().getValue());
                Eid eid = lispStateHelper.getEid(addressEp, vni);

                if (lispState.eidSetContains(eid)) {
                    deleteEidFromEidTable(vppDataBroker, lispState, eid);
                }

                if (lispState.eidCount() == 0) {
                    deleteLispStatesInEndPoints(vppDataBroker, lispState);
                }
            }
        } catch (LispConfigCommandFailedException e) {
            LOG.warn("Lisp command execution failed: {}", e.getMessage());
        }
    }

    private void enableLispForNode(DataBroker vppDataBroker, LispState lispState) throws LispConfigCommandFailedException {
        AbstractLispCommand<Lisp>
                lispEnableCommand = LispCommandWrapper.enableLisp();
        if (LispStateCommandExecutor.executePutCommand(vppDataBroker, lispEnableCommand)) {
            lispState.setLispEnabled(true);
        } else {
            throw new LispConfigCommandFailedException("Lisp Enable Command failed execution!");
        }
    }

    private void enableGpeForNode(DataBroker vppDataBroker, LispState lispState) throws LispConfigCommandFailedException {
        AbstractLispCommand<GpeFeatureData>
                gpeEnableCommand = LispCommandWrapper.enableGpe();
        if (LispStateCommandExecutor.executePutCommand(vppDataBroker, gpeEnableCommand)) {
            lispState.setGpeEnabled(true);
        } else {
            throw new LispConfigCommandFailedException("GPE Enable Command failed execution!");
        }
    }

    private void addLocatorSet(DataBroker vppDataBroker, LispState lispState) throws LispNotFoundException, LispConfigCommandFailedException {
        try {
            String locatorSetName = lispStateHelper.constructLocatorSetName(lispState.getLocatorCount());
            String interfaceName = lispStateHelper.readRlocInterface(lispState.getHostName(), vppDataBroker).get();
            AbstractLispCommand<LocatorSet> addLocatorSetCommand = LispCommandWrapper.addLocatorSet(locatorSetName,
                    interfaceName, DEFAULT_PRIORITY, DEFAULT_WEIGHT);
            if (LispStateCommandExecutor.executePutCommand(vppDataBroker, addLocatorSetCommand)) {
                lispState.setLocIntfToLocSetNameMapping(interfaceName, locatorSetName);
            } else {
                throw new LispConfigCommandFailedException("Lisp add locator set failed for host "
                        + lispState.getHostName() + " and locator interface " + interfaceName);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new LispNotFoundException("No interface with Ip Address found!");
        }

    }

    private void addMapResolver(DataBroker vppDataBroker, LispState lispState) throws LispConfigCommandFailedException {
        IpAddress mapResolverIpAddress = ConfigUtil.getInstance().getOdlTenantIp();
        Preconditions.checkNotNull(mapResolverIpAddress, "Map Resolver ip not properly configured!");

        AbstractLispCommand<MapResolver> addMapResolverCommand = LispCommandWrapper.
                addMapResolver(mapResolverIpAddress);
        if (LispStateCommandExecutor.executePutCommand(vppDataBroker, addMapResolverCommand)) {
            lispState.addInMapResolverSet(mapResolverIpAddress);
        } else {
            throw new LispConfigCommandFailedException("Lisp add map resolver for host " + lispState.getHostName()
                    + " failed for ODL ip " + mapResolverIpAddress);
        }
    }

    private void addMapServer(DataBroker vppDataBroker, LispState lispState) throws LispConfigCommandFailedException {
        IpAddress mapServerIpAddress = ConfigUtil.getInstance().getOdlTenantIp();
        Preconditions.checkNotNull(mapServerIpAddress, "Mapserver ip not properly configured!");
        AbstractLispCommand<MapServer> addMapServerCommand = LispCommandWrapper.addMapServer(mapServerIpAddress);

        if (LispStateCommandExecutor.executePutCommand(vppDataBroker, addMapServerCommand)) {
            lispState.addInMapServerSet(mapServerIpAddress);
        } else {
            throw new LispConfigCommandFailedException("Lisp add map server for host " + lispState.getHostName()
                    + " failed for ODL ip " + mapServerIpAddress);
        }
    }

    private void enableMapRegister(DataBroker vppDataBroker, LispState lispState) throws LispConfigCommandFailedException {
        AbstractLispCommand<MapRegister> enableMapRegisterCommand = LispCommandWrapper.enableMapRegister();

        if (LispStateCommandExecutor.executePutCommand(vppDataBroker, enableMapRegisterCommand)) {
            lispState.setMapRegisteredEnabled(true);
        } else {
            throw new LispConfigCommandFailedException("Lisp enable mapregistration for host "
                    + lispState.getHostName() + " failed!");
        }

    }

    private void addVniToVrfMappingIfNeeded(DataBroker vppDataBroker,
                                            LispState lispState,
                                            long vni, long vrf) throws LispConfigCommandFailedException {
        if (!lispState.vniSetContains(vni)) {
            AbstractLispCommand<VniTable> addVniToVrfMapping = LispCommandWrapper.mapVniToVrf(vni, vrf);
            addVniToVrfMapping.setOptions(General.Operations.PUT);
            if (LispStateCommandExecutor.executePutCommand(vppDataBroker, addVniToVrfMapping)) {
                lispState.addInVniSet(vni);
            } else {
                throw new LispConfigCommandFailedException("Lisp add vrf " + vrf +" for vni " +vni
                        + " command failed!");
            }
        }
    }

    private void addEidInEidTable(DataBroker vppDataBroker,
                                  LispState lispState,
                                  Eid eid) throws LispConfigCommandFailedException {
        String mappingId = lispStateHelper.constructMappingName(lispState.getInterfaceId());
        AbstractLispCommand<LocalMapping> addLocalMappingInEidTableCommand = LispCommandWrapper
                .addLocalMappingInEidTable(mappingId,
                        eid,
                        lispStateHelper.getFirstLocatorSetName(lispState),
                        lispStateHelper.getDefaultHmacKey());
        if (LispStateCommandExecutor.executePutCommand(vppDataBroker, addLocalMappingInEidTableCommand)) {
            lispState.addInEidSet(eid, mappingId);
        } else {
            throw new LispConfigCommandFailedException("Lisp add local mapping for eid " + eid + "failed!");
        }
    }

    private void deleteLispStatesInEndPoints(DataBroker vppDataBroker,
                                             LispState lispState) throws LispConfigCommandFailedException {
        AbstractLispCommand<LispFeatureData> deleteLispFeatureData = LispCommandWrapper.deleteLispFeatureData();

        if (LispStateCommandExecutor.executeDeleteCommand(vppDataBroker, deleteLispFeatureData)) {
            String computeNode = lispState.getHostName();
            lispStateMapper.remove(computeNode);
        } else {
            throw new LispConfigCommandFailedException("Lisp delete feature data command failed!");
        }
    }

    private void deleteEidFromEidTable(DataBroker vppDataBroker,
                                       LispState lispState,
                                       Eid eid) throws LispConfigCommandFailedException {
        String mappingId = lispState.getEidMapping(eid);
        long value = eid.getVirtualNetworkId().getValue();

        AbstractLispCommand<LocalMapping> deleteLocalMappingCommand = LispCommandWrapper
                .deleteLocalMappingFromEidTable(mappingId, value);

        if (LispStateCommandExecutor.executeDeleteCommand(vppDataBroker, deleteLocalMappingCommand)) {
            lispState.deleteEid(eid);
        } else {
            throw new LispConfigCommandFailedException("Lisp delete local mapping command failed!");
        }
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }
}
