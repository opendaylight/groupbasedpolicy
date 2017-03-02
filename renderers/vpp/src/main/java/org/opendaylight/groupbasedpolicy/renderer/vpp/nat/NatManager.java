/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.nat;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class NatManager {

    private static final Logger LOG = LoggerFactory.getLogger(NatManager.class);

    private final Long id;
    private final DataBroker dataBroker;
    private final MountedDataBrokerProvider mountDataProvider;

    public NatManager(DataBroker dataBroker, MountedDataBrokerProvider mountDataProvider) {
        this.id = 0L;
        this.dataBroker = dataBroker;
        this.mountDataProvider = mountDataProvider;
    }

    public Optional<InstanceIdentifier<PhysicalInterface>> resolvePhysicalInterface(IpPrefix extSubnetPrefix) {
        ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<RendererNodes> readFromDs = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                VppIidFactory.getRendererNodesIid(), rTx);
        rTx.close();
        if (!readFromDs.isPresent() || readFromDs.get().getRendererNode() == null) {
            return Optional.absent();
        }
        RendererNodes rendererNodes = readFromDs.get();
        List<RendererNode> vppNodes = rendererNodes.getRendererNode()
            .stream()
            .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class) != null)
            .filter(rn -> rn.getAugmentation(VppInterfaceAugmentation.class).getPhysicalInterface() != null)
            .collect(Collectors.toList());
        for (RendererNode rn : vppNodes) {
            java.util.Optional<PhysicalInterface> optResolvedIface = rn.getAugmentation(VppInterfaceAugmentation.class)
                .getPhysicalInterface()
                .stream()
                .filter(phIface -> phIface.getAddress() != null)
                .filter(phIface -> phIface.getAddress()
                    .stream()
                    .anyMatch(ipAddr -> NetUtils.isInRange(extSubnetPrefix, String.valueOf(ipAddr.getValue()))))
                .findFirst();
            if (optResolvedIface.isPresent()) {
                return Optional.of(VppIidFactory.getRendererNodeIid(rn)
                    .builder()
                    .augmentation(VppInterfaceAugmentation.class)
                    .child(PhysicalInterface.class, new PhysicalInterfaceKey(optResolvedIface.get().getKey()))
                    .build());
            }
        }
        return Optional.absent();
    }

    public Optional<MappingEntryBuilder> resolveSnatEntry(String internal, Ipv4Address external) {
        IpAddress internalIp = null;
        try {
            InetAddress inetAddr = InetAddress.getByName(internal);
            if (inetAddr instanceof Inet4Address) {
                internalIp = new IpAddress(new Ipv4Address(internal));
            } else if (inetAddr instanceof Inet6Address) {
                internalIp = new IpAddress(new Ipv6Address(internal));
            }
        } catch (UnknownHostException e) {
            LOG.error("Cannot resolve host IP {}. {}", internal, e.getMessage());
            return Optional.absent();
        }
        return Optional.of(new MappingEntryBuilder().setInternalSrcAddress(internalIp).setExternalSrcAddress(external));
    }

    public ListenableFuture<Void> submitNatChanges(List<InstanceIdentifier<PhysicalInterface>> physIfacesIid,
            List<MappingEntryBuilder> natEntries, boolean add) {
        List<ListenableFuture<Void>> submitedFutures = new ArrayList<>();
        for (InstanceIdentifier<PhysicalInterface> iidPhysIface : physIfacesIid) {
            InstanceIdentifier<?> nodeIid = iidPhysIface.firstKeyOf(RendererNode.class).getNodePath();
            Optional<DataBroker> mountPointDataBroker = mountDataProvider.getDataBrokerForMountPoint(nodeIid);
            if (!mountPointDataBroker.isPresent()) {
                throw new IllegalStateException("Cannot find data broker for mount point " + nodeIid);
            }
            String phInterfaceName = iidPhysIface.firstKeyOf(PhysicalInterface.class).getInterfaceName();
            ReadWriteTransaction rwTx = mountPointDataBroker.get().newReadWriteTransaction();
            InstanceIdentifier<Interface> interfaceIID = VppIidFactory.getInterfaceIID(new InterfaceKey(phInterfaceName));
            submitedFutures.add(Futures.transform(rwTx.read(LogicalDatastoreType.CONFIGURATION, interfaceIID),
                    new AsyncFunction<Optional<Interface>, Void>() {

                        @Override
                        public ListenableFuture<Void> apply(Optional<Interface> readIface) throws Exception {
                            if (!readIface.isPresent()) {
                                LOG.error("Interface {} not found on mount point {}", phInterfaceName, nodeIid);
                                return Futures.immediateFuture(null);
                            }
                            if (add) {
                                LOG.trace("Setting outbound NAT on interface {}.", iidPhysIface.getPathArguments());
                                NatUtil.setOutboundInterface(readIface.get(), rwTx);
                                rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getNatInstanceIid(id),
                                        buildNatInstance(natEntries));
                            } else {
                                LOG.trace("UNsetting outbound NAT on interface {}.", iidPhysIface.getPathArguments());
                                NatUtil.unsetOutboundInterface(readIface.get(), rwTx);
                                if (rwTx.read(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getNatInstanceIid(id))
                                    .get()
                                    .isPresent()) {
                                    rwTx.delete(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getNatInstanceIid(id));
                                }
                            }
                            return rwTx.submit();
                        }
                    }));
        }
        try {
            Futures.allAsList(submitedFutures).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to sync NAT. {}", e.getMessage());
            e.printStackTrace();
        }
        return Futures.immediateFuture(null);
    }

    private NatInstance buildNatInstance(List<MappingEntryBuilder> natEntries) {
        AtomicInteger ai = new AtomicInteger();
         List<MappingEntry> mappingEntries = natEntries.stream().map(me -> {
            int value = ai.get();
            ai.incrementAndGet();
            return me.setIndex((long) value).build();
        }).collect(Collectors.toList());
        MappingTable mappingTable = new MappingTableBuilder().setMappingEntry(mappingEntries).build();
        return new NatInstanceBuilder().setId(id).setMappingTable(mappingTable).build();
    }
}
