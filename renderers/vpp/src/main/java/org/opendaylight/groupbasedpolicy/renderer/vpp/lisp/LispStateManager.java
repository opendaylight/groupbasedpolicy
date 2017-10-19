/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.AbstractLispCommand;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.LispCommandWrapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.config.ConfigUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispConfigCommandFailedException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception.LispNotFoundException;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay.FlatOverlayManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.flat.overlay.StaticRoutingHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.EndpointHost;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.HostRelatedInfoContainer;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.LispState;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.info.container.states.PhysicalInterfaces;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers.NeutronTenantToVniMapper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.ConfigManagerHelper;
import org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util.Constants;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.LispUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.Gpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String DEFAULT_MAPPING_RECORD_NAME_PREFIX = "MR_";

    // Node ID, VRF ID, route count
    private Map<String, List<Long>> vnisByHostname = new HashMap<>();

    public LispStateManager(@Nonnull MountedDataBrokerProvider mountedDataBrokerProvider) {
        Preconditions.checkNotNull(mountedDataBrokerProvider,
                "MountedDataBrokerProvider found to be null!");
        hostRelatedInfoContainer = HostRelatedInfoContainer.getInstance();
        this.mountedDataBrokerProvider= mountedDataBrokerProvider;
        this.lispStateHelper = new ConfigManagerHelper(this.mountedDataBrokerProvider);
        neutronTenantToVniMapper = NeutronTenantToVniMapper.getInstance();
    }

    public synchronized void processCreateEndPoint(AddressEndpointWithLocation addressEp) {
        if (!addressEp.getAddressType().equals(IpPrefixType.class)) {
            return;
        }
        Map<String, String> intfcsByHostname = FlatOverlayManager.resolveIntfcsByHosts(addressEp);

        intfcsByHostname.forEach((hostname, interfaceName) -> {
            try {
                configureHostIfNeeded(hostname);

                long vni = getVni(addressEp.getTenant().getValue());
                long vrf = vni;

                Eid eid = lispStateHelper.getEid(addressEp, vni);
                String eidMappingName = lispStateHelper.constructEidMappingName(addressEp, interfaceName);

                addVniSpecificConfigurationsIfNeeded(hostname, vni, vrf);
                if (!ConfigManagerHelper.isMetadataPort(addressEp)) {
                    if (!addEidInEidTable(hostname, eid, eidMappingName)) {
                        LOG.warn("Failed to add Eid: {}, eidMappingName: {} to table on host: {}", eid, eidMappingName,
                            hostname);
                    }
                }
            } catch (LispConfigCommandFailedException e) {
                LOG.warn("Lisp endpoint configuration failed for address endpoint: {}", addressEp);
            }
        });


    }

    private synchronized void configureHostIfNeeded(String hostName)
            throws LispConfigCommandFailedException {

        if ((vnisByHostname.get(hostName) == null)) {

            LOG.debug("Configuring host {} for LISP", hostName);

            try {
                boolean lispEnabled = enableLispOnHost(hostName);
                Optional<GpeFeatureData> gpeFeatureDataOptional = GbpNetconfTransaction.read(VppIidFactory.getNetconfNodeIid(new NodeId(hostName)),
                        LogicalDatastoreType.CONFIGURATION, VppIidFactory.getGpeFeatureDataIid(),
                        GbpNetconfTransaction.RETRY_COUNT);

                LOG.trace("configureHostIfNeeded -> GpeOnHostFeatureData: {}", gpeFeatureDataOptional);

                if (!gpeFeatureDataOptional.isPresent() || !gpeFeatureDataOptional.get().isEnable()) {
                    enableGpeOnHostIfNeeded(hostName);
                    LOG.trace("configureHostIfNeeded -> GpeOnHostFeatureData were cleared");
                }

                if (lispEnabled) {
                    addLocatorSetOnHost(hostName);
                    if (!addMapResolverOnHost(hostName)) {
                        LOG.warn("Failed to add MAP resolver for host: {}", hostName);
                    }
                    enableMapRegistrationOnHostIfNeeded(hostName);
                    vnisByHostname.computeIfAbsent(hostName, k -> Lists.newArrayList());
                } else {
                    LOG.warn("Failed to enable LISP or GPE on host: {}", hostName);
                }

            } catch (LispNotFoundException e) {
                LOG.warn("Lisp host configuration failed: ", e.getMessage());
                throw new LispConfigCommandFailedException("Failed LISP configuration!");
            }
        }
    }

    private boolean enableGpeOnHostIfNeeded(String hostName) {
        return enableGpeForHost(hostName);
    }

    private void enableMapRegistrationOnHostIfNeeded(String hostName)
            throws LispConfigCommandFailedException {
        if (ConfigUtil.getInstance().isLispMapRegisterEnabled()) {
            enableMapRegister(hostName);
            if (!addMapServer(hostName)) {
                LOG.warn("Failed to add Map server for host: {}", hostName);
            }
        }
    }

    private boolean enableLispOnHost(String hostName) {
        LOG.debug("Enabling LISP on host {}", hostName);
        AbstractLispCommand<Lisp> lispEnableCommand = LispCommandWrapper.enableLisp();
        return LispStateCommandExecutor.executePutCommand(hostName, lispEnableCommand);
    }

    private boolean enableGpeForHost(String hostName) {
        AbstractLispCommand<GpeFeatureData> gpeEnableCommand = LispCommandWrapper.enableGpe();
        return LispStateCommandExecutor.executeMergeCommand(hostName, gpeEnableCommand);
    }

    private void addLocatorSetOnHost(String hostName) throws LispNotFoundException, LispConfigCommandFailedException {
        try {
            //TODO locator is set to constant value, it has to be investigated further
            String locatorSetName = lispStateHelper.constructLocatorSetName(1);
            String lispDataInterfaceName = lispStateHelper
                    .getLispDataRlocInterfaceName(hostName).get();
            AbstractLispCommand<LocatorSet> addLocatorSetCommand = LispCommandWrapper.addLocatorSet(locatorSetName,
                    lispDataInterfaceName, DEFAULT_PRIORITY, DEFAULT_WEIGHT);

            if (LispStateCommandExecutor.executePutCommand(hostName, addLocatorSetCommand)) {
                addExtraItrRlocLocatorSetIfNeeded(hostName, lispDataInterfaceName);
            } else {
                LOG.warn("Failed to write locator set: {} -> {} to host: {}", locatorSetName, lispDataInterfaceName, hostName);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new LispNotFoundException("No interface with Ip Address found!");
        }
    }

    private void addExtraItrRlocLocatorSetIfNeeded(String hostName, String lispDataInterfaceName)
            throws LispNotFoundException, LispConfigCommandFailedException {
        String lispCpRlocInterfaceName = lispStateHelper.getLispCpRlocInterfaceName(hostName);
        if (lispCpRlocInterfaceName == null || lispCpRlocInterfaceName.isEmpty()
                || lispCpRlocInterfaceName.equals(lispDataInterfaceName)) {
            return;
        }

        addItrLocatorSet(hostName, lispCpRlocInterfaceName);
    }

    private void addItrLocatorSet(String hostName, String lispCpInterfaceName)
            throws LispNotFoundException, LispConfigCommandFailedException {
        String locatorSetName = lispStateHelper.constructLocatorSetNameForItrRloc();
        AbstractLispCommand<LocatorSet> addLocatorSetCommand = LispCommandWrapper.addLocatorSet(locatorSetName,
                lispCpInterfaceName, DEFAULT_PRIORITY, DEFAULT_WEIGHT);
        if (!LispStateCommandExecutor.executePutCommand(hostName, addLocatorSetCommand)) {
            throw new LispConfigCommandFailedException("Lisp add locator set failed for host "
                    + hostName + " and locator interface " + lispCpInterfaceName);
        } else {
            AbstractLispCommand<ItrRemoteLocatorSet> addItrRlocCommand = LispCommandWrapper.addItrRloc(locatorSetName);
            if (!LispStateCommandExecutor.executePutCommand(hostName, addItrRlocCommand)) {
                throw new LispConfigCommandFailedException(
                    "Lisp add Itr Rloc command failed for host " + hostName + " and locator set " + locatorSetName);
            }
        }
    }

    private boolean addMapResolverOnHost(String hostname) {
        IpAddress mapResolverIpAddress = ConfigUtil.getInstance().getOdlIp();
        Preconditions.checkNotNull(mapResolverIpAddress, "Map Resolver ip not properly configured!");

        AbstractLispCommand<MapResolver> addMapResolverCommand = LispCommandWrapper.
                addMapResolver(mapResolverIpAddress);
        return LispStateCommandExecutor.executePutCommand(hostname, addMapResolverCommand);
    }

    private void enableMapRegister(String hostName)
            throws LispConfigCommandFailedException {
        AbstractLispCommand<MapRegister> enableMapRegisterCommand = LispCommandWrapper.enableMapRegister();

        if (!LispStateCommandExecutor.executePutCommand(hostName, enableMapRegisterCommand)) {
            throw new LispConfigCommandFailedException("Lisp enable map registration for host "
                    + hostName + " failed!");
        }

    }

    private boolean addMapServer(String hostName) throws LispConfigCommandFailedException {
        IpAddress mapServerIpAddress = ConfigUtil.getInstance().getOdlIp();
        Preconditions.checkNotNull(mapServerIpAddress, "Mapserver ip not properly configured!");
        AbstractLispCommand<MapServer> addMapServerCommand = LispCommandWrapper.addMapServer(mapServerIpAddress);

        return LispStateCommandExecutor.executePutCommand(hostName, addMapServerCommand);
    }

    private void addVniSpecificConfigurationsIfNeeded(String hostName, long vni, long vrf) {

        if (vnisByHostname.get(hostName) != null && !vnisByHostname.get(hostName).contains(Long.valueOf(vni))) {
            if (addVniToVrfMapping(hostName, vni, vrf)) {
                if (!addGpeNativeForwardPath(hostName, vrf, hostRelatedInfoContainer.getPhysicalInterfaceState(hostName)
                        .getIp(PhysicalInterfaces.PhysicalInterfaceType.PUBLIC))) {
                    LOG.warn("Configure GPE native forward failed for host: {} and vni: {}", hostName, vni);
                }
                if (vnisByHostname.get(hostName) != null) {
                    vnisByHostname.get(hostName).add(vni);
                }
            }
        }
    }

    private boolean addVniToVrfMapping(String hostName, long vni, long vrf) {
        AbstractLispCommand<VniTable> addVniToVrfMapping = LispCommandWrapper.mapVniToVrf(vni, vrf);
        return (LispStateCommandExecutor.executePutCommand(hostName, addVniToVrfMapping));
    }

    private boolean addGpeNativeForwardPath(String hostname, long vrf, IpAddress nativeForwardIp){
        AbstractLispCommand<NativeForwardPath> addNativeForwardingIp =
                LispCommandWrapper.addNativeForwardEntry(vrf, nativeForwardIp);
        return LispStateCommandExecutor.executePutCommand(hostname, addNativeForwardingIp);
    }

    private boolean addEidInEidTable(String hostName, Eid eid, String eidMappingName)
        throws LispConfigCommandFailedException {
        AbstractLispCommand<LocalMapping> addLocalMappingInEidTableCommand = LispCommandWrapper
                .addLocalMappingInEidTable(eidMappingName,
                        eid,
                        lispStateHelper.constructLocatorSetName(1),
                        lispStateHelper.getDefaultHmacKey());
        return LispStateCommandExecutor.executePutCommand(hostName, addLocalMappingInEidTableCommand);
    }

    public synchronized void processDeleteEndpoint(AddressEndpointWithLocation addressEp) {
        if (!addressEp.getAddressType().equals(IpPrefixType.class)) {
            return;
        }
        Map<String, String> intfcsByHostname = FlatOverlayManager.resolveIntfcsByHosts(addressEp);

        intfcsByHostname.forEach((hostname, interfaceName) -> {
            try {

                long vni = getVni(addressEp.getTenant().getValue());
                Eid eid = lispStateHelper.getEid(addressEp, vni);
                String eidMappingName = lispStateHelper.constructEidMappingName(addressEp, interfaceName);
                if (!ConfigManagerHelper.isMetadataPort(addressEp)) {
                    if (!deleteEidFromLocalEidTableOfHost(hostname, eid, eidMappingName)) {
                        LOG.warn("Failed to delete Eid : {}, eidMappingName: {} on host: {}", eid, eidMappingName,
                            hostname);
                    }
                }

                Optional<LocalMappings> localMappingsOptional =
                    GbpNetconfTransaction.read(VppIidFactory.getNetconfNodeIid(new NodeId(hostname)),
                        LogicalDatastoreType.CONFIGURATION, VppIidFactory.getLocalMappings(new VniTableKey(vni)),
                        GbpNetconfTransaction.RETRY_COUNT);

                if (!localMappingsOptional.isPresent() || localMappingsOptional.get().getLocalMapping() == null
                    || localMappingsOptional.get().getLocalMapping().size() == 0) {

                    //remove mapping table for VNI
                    if (GbpNetconfTransaction.netconfSyncedDelete(VppIidFactory.getNetconfNodeIid(new NodeId(hostname)),
                        VppIidFactory.getVniTableIid(new VniTableKey(vni)), GbpNetconfTransaction.RETRY_COUNT)) {
                        Preconditions.checkNotNull(hostname);
                        if (vnisByHostname.get(hostname) != null) {
                            vnisByHostname.get(hostname).remove(vni);
                        }
                    }
                }
                if (vnisByHostname.get(hostname)!= null && vnisByHostname.get(hostname).size() == 0) {
                    //safe to delete lisp
                    deleteLispStatesFromHost(hostname);
                    deleteNativeForwardPathsTables(hostname);
                    vnisByHostname.remove(hostname);

                }
            } catch (LispConfigCommandFailedException e) {
                LOG.warn("Lisp command execution failed: {}", e.getMessage());
            }
        });
    }



    private boolean deleteEidFromLocalEidTableOfHost(String hostName, Eid eid, String eidMappingName)
        throws LispConfigCommandFailedException {
        long value = eid.getVirtualNetworkId().getValue();

        AbstractLispCommand<LocalMapping> deleteLocalMappingCommand = LispCommandWrapper
                .deleteLocalMappingFromEidTable(eidMappingName, value);

        return (LispStateCommandExecutor.executeDeleteCommand(hostName, deleteLocalMappingCommand));
    }


    private void deleteLispStatesFromHost(String hostname) throws LispConfigCommandFailedException {
        /*AbstractLispCommand<LispFeatureData> deleteLispFeatureData = LispCommandWrapper.deleteLispFeatureData();

        if (LispStateCommandExecutor.executeDeleteCommand(endpointHost.getHostName(), deleteLispFeatureData)) {
            hostRelatedInfoContainer.deleteLispStateOfHost(endpointHost.getHostName());
            LOG.debug("Deleted all lisp data {}, for host {}",
                hostRelatedInfoContainer.getLispStateOfHost(endpointHost.getHostName()), endpointHost.getHostName());
        } else {
            throw new LispConfigCommandFailedException("Lisp delete feature data command failed!");
        }
        */

        //Todo workaround to delete only inside data not whole lisp-feature-data
        // (causes VPP to crash https://jira.fd.io/browse/HC2VPP-242) remove when fixed
        InstanceIdentifier<Node> nodeIid = LispUtil.HOSTNAME_TO_IID.apply(hostname);
        Optional<Lisp> lispOptional =
            GbpNetconfTransaction.read(nodeIid, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Lisp.class), GbpNetconfTransaction.RETRY_COUNT);
        if (lispOptional.isPresent()) {
            LispFeatureData lispFeatureData = lispOptional.get().getLispFeatureData();

            if (lispFeatureData == null || nodeIid == null) {
                return;
            }
            LOG.trace("Removing all Eids from host: {}", hostname);
            if (lispFeatureData.getEidTable() != null && lispFeatureData.getEidTable().getVniTable() != null) {

                lispFeatureData.getEidTable().getVniTable().forEach(vniTable -> {
                    if (vniTable.getVrfSubtable() != null && vniTable.getVrfSubtable().getLocalMappings() != null
                        && vniTable.getVrfSubtable().getLocalMappings().getLocalMapping() != null)
                        //remove all mapping from vni table
                    vniTable.getVrfSubtable().getLocalMappings().getLocalMapping().forEach(localMapping -> {
                        GbpNetconfTransaction.netconfSyncedDelete(nodeIid,
                            InstanceIdentifier.builder(Lisp.class)
                                .child(LispFeatureData.class)
                                .child(EidTable.class)
                                .child(VniTable.class, vniTable.getKey())
                                .child(VrfSubtable.class)
                                .child(LocalMappings.class)
                                .child(LocalMapping.class, localMapping.getKey())
                                .build(),
                            GbpNetconfTransaction.RETRY_COUNT);
                    });
                    //remove EID VNI table
                    GbpNetconfTransaction.netconfSyncedDelete(nodeIid, InstanceIdentifier.builder(Lisp.class)
                        .child(LispFeatureData.class)
                        .child(EidTable.class)
                        .child(VniTable.class, vniTable.getKey())
                        .build(), GbpNetconfTransaction.RETRY_COUNT);
                });
                //remove EID table
                GbpNetconfTransaction.netconfSyncedDelete(nodeIid,
                    InstanceIdentifier.builder(Lisp.class).child(LispFeatureData.class).child(EidTable.class).build(),
                    GbpNetconfTransaction.RETRY_COUNT);

            }
            LOG.trace("Removing ItrRemoteLocatorSet from host: {}", hostname);
            if (lispFeatureData.getItrRemoteLocatorSet() != null) {
                GbpNetconfTransaction.netconfSyncedDelete(nodeIid, InstanceIdentifier.builder(Lisp.class)
                    .child(LispFeatureData.class)
                    .child(ItrRemoteLocatorSet.class)
                    .build(), GbpNetconfTransaction.RETRY_COUNT);
            }
            LOG.trace("Removing all locators from host: {}", hostname);
            if (lispFeatureData.getLocatorSets() != null) {

                List<LocatorSet> locatorSetList = lispFeatureData.getLocatorSets().getLocatorSet();
                if (locatorSetList == null || locatorSetList.isEmpty()) {
                    return;
                }

                for (LocatorSet locatorSet : locatorSetList) {
                        GbpNetconfTransaction.netconfSyncedDelete(nodeIid, InstanceIdentifier.builder(Lisp.class)
                            .child(LispFeatureData.class)
                            .child(LocatorSets.class)
                            .child(LocatorSet.class, locatorSet.getKey())
                            .build(), GbpNetconfTransaction.RETRY_COUNT);
                }

            }
            LOG.trace("Removing MapResolvers from host: {}", hostname);
            if (lispFeatureData.getMapResolvers() != null) {
                GbpNetconfTransaction.netconfSyncedDelete(nodeIid, InstanceIdentifier.builder(Lisp.class)
                    .child(LispFeatureData.class)
                    .child(MapResolvers.class)
                    .build(), GbpNetconfTransaction.RETRY_COUNT);
            }
            LOG.trace("Removing MapServers from host: {}", hostname);
            if (lispFeatureData.getMapServers() != null) {
                GbpNetconfTransaction.netconfSyncedDelete(nodeIid, InstanceIdentifier.builder(Lisp.class)
                    .child(LispFeatureData.class)
                    .child(MapServers.class)
                    .build(), GbpNetconfTransaction.RETRY_COUNT);
            }
            LOG.trace("Removing MapServers from host: {}", hostname);
            if (lispFeatureData.getMapRegister() != null) {
                GbpNetconfTransaction.netconfSyncedDelete(nodeIid, InstanceIdentifier.builder(Lisp.class)
                    .child(LispFeatureData.class)
                    .child(MapRegister.class)
                    .build(), GbpNetconfTransaction.RETRY_COUNT);
            }

            LOG.trace("Removing all locators from host: {}", hostname);

            cleanLisp(hostname);
        }

    }

    private boolean deleteNativeForwardPathsTables(String hostname)
            throws LispConfigCommandFailedException {
        AbstractLispCommand<NativeForwardPathsTables> deleteNativeForwardPathsTables = LispCommandWrapper
                .deleteNativeForwardPathsTables();

        return LispStateCommandExecutor.executeDeleteCommand(hostname, deleteNativeForwardPathsTables);
    }

    private long getVni(String tenantUuid) {
        return neutronTenantToVniMapper.getVni(tenantUuid);
    }

    public static void cleanLisp(String hostName)
        throws LispConfigCommandFailedException {
        if (LispStateCommandExecutor.executeDeleteCommand(hostName, LispCommandWrapper.deleteLispFeatureData())) {
            LOG.debug("Deleted all lisp data for host {}",hostName);
        } else {
            throw new LispConfigCommandFailedException("Lisp delete feature data command failed!");
        }
    }
}
