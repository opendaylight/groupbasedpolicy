/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.ServiceChainingUtil;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.ServicePolicy;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChain.Direction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.Interface;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._interface.GigabitEthernet;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._interface.GigabitEthernetKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PolicyWriter {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWriter.class);

    private final DataBroker mountpoint;
    // Local cache
    private final List<ClassMap> classMapEntries;
    private final List<Class> policyMapEntries;
    private final List<ServiceFfName> remoteForwarders;
    private final List<ServiceChain> serviceChains;
    private final NodeId nodeId;
    private final String interfaceName;
    private final String policyMapName;
    private final String managementIpAddress;
    private Local localForwarder;

    public PolicyWriter(final DataBroker dataBroker, final String interfaceName, final String ipAddress,
                        final String policyMapName, final NodeId nodeId) {
        classMapEntries = new ArrayList<>();
        policyMapEntries = new ArrayList<>();
        remoteForwarders = new ArrayList<>();
        serviceChains = new ArrayList<>();

        this.nodeId = Preconditions.checkNotNull(nodeId);
        mountpoint = Preconditions.checkNotNull(dataBroker);
        managementIpAddress = Preconditions.checkNotNull(ipAddress);
        this.interfaceName = Preconditions.checkNotNull(interfaceName);
        this.policyMapName = Preconditions.checkNotNull(policyMapName);
    }

    public void cache(ClassMap classMap) {
        classMapEntries.add(classMap);
    }

    public void cache(List<Class> policyMapEntries) {
        this.policyMapEntries.addAll(policyMapEntries);
    }

    public void cache(Local localForwarder) {
        this.localForwarder = localForwarder;
    }

    public void cache(ServiceFfName remoteForwarder) {
        remoteForwarders.add(remoteForwarder);
    }

    public void cache(ServiceChain serviceChain) {
        serviceChains.add(serviceChain);
    }

    public CheckedFuture<Void, TransactionCommitFailedException> commitToDatastore() {
        WriteTransaction wtx = mountpoint.newWriteOnlyTransaction();
        // GBP
        // Class maps
        for (ClassMap entry : classMapEntries) {
            InstanceIdentifier<ClassMap> classMapIid = classMapInstanceIdentifier(entry);
            wtx.merge(LogicalDatastoreType.CONFIGURATION, classMapIid, entry);
            LOG.info("Created class-map {} on node {}", entry.getName(), nodeId.getValue());
        }

        // Policy map
        PolicyMap policyMap = PolicyManagerUtil.createPolicyMap(policyMapName, policyMapEntries);
        InstanceIdentifier<PolicyMap> policyMapIid = policyMapInstanceIdentifier();
        wtx.merge(LogicalDatastoreType.CONFIGURATION, policyMapIid, policyMap);
        LOG.info("Created policy-map {} on node {}", policyMap.getName(), nodeId.getValue());

        // Interface
        ServicePolicy servicePolicy = PolicyManagerUtil.createServicePolicy(policyMapName, Direction.Input);
        InstanceIdentifier<ServicePolicy> servicePolicyIid = interfaceInstanceIdentifier(interfaceName);
        wtx.merge(LogicalDatastoreType.CONFIGURATION, servicePolicyIid, servicePolicy);
        LOG.info("Service-policy interface {}, bound to policy-map {} created on  node {}",
                interfaceName, policyMap.getName(), nodeId.getValue());

        //SFC
        // Local forwarder (if some service chain exists, otherwise is useless)
        if (!serviceChains.isEmpty()) {
            InstanceIdentifier<Local> localIid = localSffInstanceIdentifier();
            wtx.merge(LogicalDatastoreType.CONFIGURATION, localIid, localForwarder);
            LOG.info("Local forwarder created on node {}", nodeId.getValue());
        }

        // Remote forwarders
        for (ServiceFfName forwarder : remoteForwarders) {
            InstanceIdentifier<ServiceFfName> forwarderIid = remoteSffInstanceIdentifier(forwarder);
            wtx.merge(LogicalDatastoreType.CONFIGURATION, forwarderIid, forwarder);
            LOG.info("Remote forwarder {} created on node {}", forwarder.getName(), nodeId.getValue());
        }

        // Service paths
        for (ServiceChain serviceChain : serviceChains) {
            for (ServicePath entry : serviceChain.getServicePath()) {
                InstanceIdentifier<ServicePath> servicePathIid = servicePathInstanceIdentifier(entry.getKey());
                wtx.merge(LogicalDatastoreType.CONFIGURATION, servicePathIid, entry);
                LOG.info("Service path with Id {} created on node {}", entry.getServicePathId(), nodeId.getValue());
            }
        }

        return wtx.submit();
    }

    public CheckedFuture<Void, TransactionCommitFailedException> removeFromDatastore() {
        ReadWriteTransaction wtx = mountpoint.newReadWriteTransaction();
        //GBP
        // Interface
        InstanceIdentifier<ServicePolicy> servicePolicyIid = interfaceInstanceIdentifier(interfaceName);
        wtx.delete(LogicalDatastoreType.CONFIGURATION, servicePolicyIid);
        LOG.info("Service-policy removed from interface {} on node {}", interfaceName, nodeId.getValue());

        // Policy map
        InstanceIdentifier<PolicyMap> policyMapIid = policyMapInstanceIdentifier();
        wtx.delete(LogicalDatastoreType.CONFIGURATION, policyMapIid);
        LOG.info("Policy-map removed from node node {}", nodeId.getValue());

        // Class map
        for (ClassMap entry : classMapEntries) {
            InstanceIdentifier<ClassMap> classMapIid = classMapInstanceIdentifier(entry);
            wtx.delete(LogicalDatastoreType.CONFIGURATION, classMapIid);
            LOG.info("Class-map {} removed from node {}", entry.getName(), nodeId.getValue());
        }

        //SFC
        // Service paths
        for (ServiceChain serviceChain : serviceChains) {
            for (ServicePath entry : serviceChain.getServicePath()) {
                InstanceIdentifier<ServicePath> servicePathIid = servicePathInstanceIdentifier(entry.getKey());
                wtx.delete(LogicalDatastoreType.CONFIGURATION, servicePathIid);
                LOG.info("Service path with Id {} removed from node {}", entry.getServicePathId(), nodeId.getValue());
            }
        }

        // Remote forwarders
        for (ServiceFfName forwarder : remoteForwarders) {
            InstanceIdentifier<ServiceFfName> forwarderIid = remoteSffInstanceIdentifier(forwarder);
            wtx.delete(LogicalDatastoreType.CONFIGURATION, forwarderIid);
            LOG.info("Remote forwarder {} removed from node {}", forwarder.getName(), nodeId.getValue());
        }

        // Local forwarder - remove only if there is no more service-paths on device. If paths removed above were last
        // ones, remove local forwarder. If there are still some paths present, they were created by sfc and local
        // forwarder cannot be removed (because it was created by sfc as well)
        if (ServiceChainingUtil.checkServicePathPresence(mountpoint)) {
            InstanceIdentifier<Local> localIid = localSffInstanceIdentifier();
            wtx.delete(LogicalDatastoreType.CONFIGURATION, localIid);
            LOG.info("Local forwarder removed from node {}", nodeId.getValue());
        }

        return wtx.submit();
    }

    private InstanceIdentifier<ClassMap> classMapInstanceIdentifier(ClassMap classMap) {
        return InstanceIdentifier.builder(Native.class)
                .child(ClassMap.class, new ClassMapKey(classMap.getName())).build();
    }

    private InstanceIdentifier<PolicyMap> policyMapInstanceIdentifier() {
        return InstanceIdentifier.builder(Native.class)
                .child(PolicyMap.class, new PolicyMapKey(policyMapName)).build();
    }

    private InstanceIdentifier<ServicePolicy> interfaceInstanceIdentifier(String ethernetName) {
        return InstanceIdentifier.builder(Native.class)
                .child(Interface.class)
                .child(GigabitEthernet.class, new GigabitEthernetKey(ethernetName))
                .child(ServicePolicy.class)
                .build();
    }

    private InstanceIdentifier<Local> localSffInstanceIdentifier() {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(ServiceFunctionForwarder.class)
                .child(Local.class).build();
    }

    private InstanceIdentifier<ServiceFfName> remoteSffInstanceIdentifier(ServiceFfName sffName) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(sffName.getName())).build();
    }

    private InstanceIdentifier<ServicePath> servicePathInstanceIdentifier(ServicePathKey key) {
        return InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(ServicePath.class, key).build();
    }

    public String getManagementIpAddress() {
        return managementIpAddress;
    }

    public DataBroker getCurrentMountpoint() {
        return mountpoint;
    }

    public NodeId getCurrentNodeId() {
        return nodeId;
    }
}
