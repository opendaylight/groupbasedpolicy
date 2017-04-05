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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.GbpNetconfTransaction;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.renderers.renderer.renderer.nodes.renderer.node.PhysicalInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import javax.annotation.Nullable;

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

    public Optional<MappingEntryBuilder> resolveSnatEntry(String internal, Ipv4Address external) {
        IpAddress internalIp = null;
        LOG.trace("Resolving SNAT entry for internal: {}, external: {}", internal, external);
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
        SubnetUtils subnet = new SubnetUtils(internal + "/32" );
        Long index = Integer.toUnsignedLong(subnet.getInfo().asInteger(internal));
        MappingEntryBuilder mappingEntryBuilder =
            new MappingEntryBuilder().setType(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.MappingEntry.Type.Static)
                .setIndex(index)
                .setInternalSrcAddress(internalIp)
                .setExternalSrcAddress(external);
        LOG.trace("Resolved SNAT mapping: {}", mappingEntryBuilder.build().toString());
        return Optional.of(mappingEntryBuilder);
    }

    public void submitNatChanges(final List<InstanceIdentifier<PhysicalInterface>> physIfacesIid,
                                 final @Nullable List<MappingEntryBuilder> sNatEntries,
                                 final PolicyContext policyCtx,
                                 final boolean add) {
        if (sNatEntries == null) {
            LOG.trace("No static NAT entries to submit");
        } else{
            LOG.trace("Preparing to submit NAT changes {} on physical interfaces", sNatEntries.toArray(), physIfacesIid);
        }
        for (InstanceIdentifier<PhysicalInterface> iidPhysIface : physIfacesIid) {
            InstanceIdentifier<?> nodeIid = iidPhysIface.firstKeyOf(RendererNode.class).getNodePath();
            Optional<DataBroker> mountPointDataBroker = mountDataProvider.getDataBrokerForMountPoint(nodeIid);
            if (!mountPointDataBroker.isPresent()) {
                throw new IllegalStateException("Cannot find data broker for mount point " + nodeIid);
            }
            String phInterfaceName = iidPhysIface.firstKeyOf(PhysicalInterface.class).getInterfaceName();
            InstanceIdentifier<Interface> interfaceIID =
                VppIidFactory.getInterfaceIID(new InterfaceKey(phInterfaceName));

            Optional<Interface> readIface =
                GbpNetconfTransaction.read(mountPointDataBroker.get(), LogicalDatastoreType.CONFIGURATION, interfaceIID,
                    GbpNetconfTransaction.RETRY_COUNT);

            if (!readIface.isPresent()) {
                LOG.error("Interface {} not found on mount point {}", phInterfaceName, nodeIid);
                continue;
            }
            if (add) {
                NatInstance natInstance =
                    buildNatInstance(sNatEntries, NatUtil.resolveDynamicNat(policyCtx, sNatEntries));
                GbpNetconfTransaction.netconfSyncedWrite(mountPointDataBroker.get(),
                    VppIidFactory.getNatInstanceIid(id), natInstance, GbpNetconfTransaction.RETRY_COUNT);
            } else {
                if (GbpNetconfTransaction.read(mountPointDataBroker.get(), LogicalDatastoreType.CONFIGURATION,
                    VppIidFactory.getNatInstanceIid(id), GbpNetconfTransaction.RETRY_COUNT).isPresent()) {
                    GbpNetconfTransaction.netconfSyncedDelete(mountPointDataBroker.get(),
                        VppIidFactory.getNatInstanceIid(id), GbpNetconfTransaction.RETRY_COUNT);
                }
            }
        }
    }

    private NatInstance buildNatInstance(List<MappingEntryBuilder> natEntries,
        List<ExternalIpAddressPool> poolEntries) {
        AtomicInteger ai = new AtomicInteger();
         List<MappingEntry> mappingEntries = natEntries.stream().map(me -> {
            int value = ai.get();
            ai.incrementAndGet();
            return me.setIndex((long) value).build();
        }).collect(Collectors.toList());
        MappingTable mappingTable = new MappingTableBuilder().setMappingEntry(mappingEntries).build();
        return new NatInstanceBuilder()
            .setId(id)
            .setExternalIpAddressPool(poolEntries)
            .setMappingTable(mappingTable)
            .build();
    }
}
