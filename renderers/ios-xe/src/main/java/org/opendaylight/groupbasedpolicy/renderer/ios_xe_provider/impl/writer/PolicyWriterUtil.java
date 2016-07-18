/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import java.util.List;
import java.util.Set;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.ClassNameType;
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
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: Util class for every policy writer
 */
class PolicyWriterUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWriterUtil.class);

    static boolean writeClassMaps(final Set<ClassMap> classMapEntries, final NodeId nodeId, final DataBroker mountpoint) {
        boolean result = true;
        if (classMapEntries == null || classMapEntries.isEmpty()) {
            return true;
        }
        for (ClassMap entry : classMapEntries) {
            final InstanceIdentifier<ClassMap> classMapIid = classMapInstanceIdentifier(entry);
            netconfWrite(mountpoint, classMapIid, entry);
            // Check
            final java.util.Optional<ClassMap> checkCreated = java.util.Optional.ofNullable(netconfRead(mountpoint, classMapIid));
            if (checkCreated.isPresent()) {
                LOG.trace("Created class-map {} on node {}", entry.getName(), nodeId.getValue());
            }
            else {
                LOG.warn("Failed to create class-map {} on node {}", entry.getName(), nodeId.getValue());
                result = false;
            }
        }
        return result;
    }

    static boolean removeClassMaps(final Set<ClassMap> classMapEntries, final NodeId nodeId, final DataBroker mountpoint) {
        boolean result = true;
        if (classMapEntries == null || classMapEntries.isEmpty()) {
            return true;
        }
        for (ClassMap entry : classMapEntries) {
            final InstanceIdentifier<ClassMap> classMapIid = classMapInstanceIdentifier(entry);
            netconfDeleteIfPresent(mountpoint, classMapIid);
            // Check
            final java.util.Optional<ClassMap> checkCreated = java.util.Optional.ofNullable(netconfRead(mountpoint, classMapIid));
            if (checkCreated.isPresent()) {
                LOG.warn("Failed to remove class-map {} on node {}", entry.getName(), nodeId.getValue());
                result = false;
            }
            else {
                LOG.trace("Class-map {} removed from node {}", entry.getName(), nodeId.getValue());
            }
        }
        return result;
    }

    static boolean writePolicyMap(final String policyMapName, final Set<Class> policyMapEntries, NodeId nodeId,
                                  final DataBroker mountpoint) {
        final PolicyMap policyMap = PolicyManagerUtil.createPolicyMap(policyMapName, policyMapEntries);
        final InstanceIdentifier<PolicyMap> policyMapIid = policyMapInstanceIdentifier(policyMapName);
        netconfWrite(mountpoint, policyMapIid, policyMap);
        // Check
        if (netconfRead(mountpoint, policyMapIid) == null) {
            LOG.warn("Failed to create policy-map {} on node {}", policyMap.getName(), nodeId.getValue());
            return false;
        }
        LOG.trace("Created policy-map {} on node {}", policyMap.getName(), nodeId.getValue());
        return true;
    }

    static boolean removePolicyMapEntries(final String policyMapName, final Set<Class> policyMapEntries,
                                          final NodeId nodeId, final DataBroker mountpoint) {
        if (policyMapEntries == null || policyMapEntries.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (Class entry : policyMapEntries) {
            final InstanceIdentifier policyMapEntryIid = policyMapEntryInstanceIdentifier(policyMapName, entry.getName());
            if (netconfDeleteIfPresent(mountpoint, policyMapEntryIid)) {
                LOG.trace("Policy-map entry {} removed from node {}", entry.getName(), nodeId.getValue());
            }
            else {
                LOG.warn("Failed to remove policy-map entry {} from node {}", entry.getName(), nodeId.getValue());
                result = false;
            }
        }
        return result;
    }

    static boolean writeInterface(final String policyMapName, final String interfaceName, final NodeId nodeId,
                                  final DataBroker mountpoint) {
        final ServicePolicy servicePolicy = PolicyManagerUtil.createServicePolicy(policyMapName, Direction.Input);
        final InstanceIdentifier<ServicePolicy> servicePolicyIid = interfaceInstanceIdentifier(interfaceName);
        if (netconfWrite(mountpoint, servicePolicyIid, servicePolicy)) {
            LOG.trace("Service-policy interface {}, bound to policy-map {} created on  node {}",
                    interfaceName, policyMapName, nodeId.getValue());
            return true;
        }
        else {
            LOG.warn("Failed to write service-policy interface {} to policy-map {} on  node {}",
                    interfaceName, policyMapName, nodeId.getValue());
            return false;
        }
    }

    static boolean writeRemote(final Set<ServiceFfName> remoteForwarders, final NodeId nodeId,
                               final DataBroker mountpoint) {
        if (remoteForwarders == null || remoteForwarders.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (ServiceFfName forwarder : remoteForwarders) {
            final InstanceIdentifier<ServiceFfName> forwarderIid = remoteSffInstanceIdentifier(forwarder);
            if (netconfWrite(mountpoint, forwarderIid, forwarder)) {
                LOG.trace("Remote forwarder {} created on node {}", forwarder.getName(), nodeId.getValue());
            }
            else {
                LOG.warn("Failed to create remote forwarder {} on node {}", forwarder.getName(), nodeId.getValue());
                result = false;
            }
        }
        return result;
    }

    static boolean removeRemote(final Set<ServiceFfName> remoteForwarders, final NodeId nodeId,
                                final DataBroker mountpoint) {
        if (remoteForwarders == null || remoteForwarders.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (ServiceFfName forwarder : remoteForwarders) {
            final InstanceIdentifier<ServiceFfName> forwarderIid = remoteSffInstanceIdentifier(forwarder);
            if (netconfDeleteIfPresent(mountpoint, forwarderIid)) {
                LOG.trace("Remote forwarder {} removed from node {}", forwarder.getName(), nodeId.getValue());
            }
            else {
                LOG.warn("Failed to remove forwarder {} from node {}", forwarder.getName(), nodeId.getValue());
                result = false;
            }
        }
        return result;
    }

    static boolean writeServicePaths(final Set<ServiceChain> serviceChains, final NodeId nodeId,
                                     final DataBroker mountpoint) {
        boolean result = true;
        for (org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain serviceChain : serviceChains) {
            for (ServicePath entry : serviceChain.getServicePath()) {
                final InstanceIdentifier<ServicePath> servicePathIid = servicePathInstanceIdentifier(entry.getKey());
                if (netconfWrite(mountpoint, servicePathIid, entry)) {
                    LOG.trace("Service-path with ID: {} created on node {}", entry.getServicePathId(), nodeId.getValue());
                }
                else {
                    LOG.warn("Failed to create service-path with ID: {} on node {}", entry.getServicePathId(), nodeId.getValue());
                    result = false;
                }
            }
        }
        return result;
    }

    static boolean removeServicePaths(final Set<ServiceChain> serviceChains, final NodeId nodeId,
                                      final DataBroker mountpoint) {
        if (serviceChains == null || serviceChains.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (ServiceChain chain : serviceChains) {
            List<ServicePath> servicePaths = chain.getServicePath();
            if (servicePaths == null || servicePaths.isEmpty()) {
                continue;
            }
            for (ServicePath servicePath : servicePaths) {
                final InstanceIdentifier<ServicePath> servicePathIid = servicePathInstanceIdentifier(servicePath.getKey());
                if (netconfDeleteIfPresent(mountpoint, servicePathIid)) {
                    LOG.trace("Service-path with ID: {} removed from node {}", servicePath.getServicePathId(),
                            nodeId.getValue());
                }
                else {
                    LOG.warn("Failed to remove service-path with ID: {} from node {}", servicePath.getServicePathId(),
                            nodeId.getValue());
                    result = false;
                }
            }
        }
        return result;
    }

    private static InstanceIdentifier<ClassMap> classMapInstanceIdentifier(final ClassMap classMap) {
        return InstanceIdentifier.builder(Native.class)
                .child(ClassMap.class, new ClassMapKey(classMap.getName())).build();
    }

    private static InstanceIdentifier<PolicyMap> policyMapInstanceIdentifier(final String policyMapName) {
        return InstanceIdentifier.builder(Native.class)
                .child(PolicyMap.class, new PolicyMapKey(policyMapName)).build();
    }

    private static InstanceIdentifier<Class> policyMapEntryInstanceIdentifier(final String policyMapName,
                                                                              final ClassNameType classNameType) {
        return InstanceIdentifier.builder(Native.class)
                .child(PolicyMap.class, new PolicyMapKey(policyMapName))
                .child(Class.class, new ClassKey(classNameType)).build();
    }

    private static InstanceIdentifier<ServicePolicy> interfaceInstanceIdentifier(final String ethernetName) {
        return InstanceIdentifier.builder(Native.class)
                .child(Interface.class)
                .child(GigabitEthernet.class, new GigabitEthernetKey(ethernetName))
                .child(ServicePolicy.class)
                .build();
    }

    private static InstanceIdentifier<ServiceFfName> remoteSffInstanceIdentifier(final ServiceFfName sffName) {
        return InstanceIdentifier.builder(Native.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(ServiceFfName.class, new ServiceFfNameKey(sffName.getName())).build();
    }

    private static InstanceIdentifier<ServicePath> servicePathInstanceIdentifier(final ServicePathKey key) {
        return InstanceIdentifier.builder(Native.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain.class)
                .child(ServicePath.class, key).build();
    }

    private static <U extends DataObject> boolean netconfWrite(final DataBroker mountpoint,
                                                            final InstanceIdentifier<U> addIID,
                                                            final U data) {
        final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
        if (!optionalWriteTransaction.isPresent()) {
            LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
            return false;
        }
        final WriteTransaction transaction = optionalWriteTransaction.get();
        try {
            transaction.merge(LogicalDatastoreType.CONFIGURATION, addIID, data);
            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = transaction.submit();
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
        return false;
    }

    private static <U extends DataObject> boolean netconfDeleteIfPresent(final DataBroker mountpoint,
                                                                         final InstanceIdentifier<U> deleteIID) {
        if (netconfRead(mountpoint, deleteIID) == null) {
            LOG.trace("Remove action called on non-existing element, skipping. Iid was: {}, data provider: {} ",
                    deleteIID, mountpoint);
            return true;
        }
        final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
        if (!optionalWriteTransaction.isPresent()) {
            LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
            return false;
        }
        final WriteTransaction transaction = optionalWriteTransaction.get();
        try {
            transaction.delete(LogicalDatastoreType.CONFIGURATION, deleteIID);
            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = transaction.submit();
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
        return false;
    }

    private static <U extends DataObject> U netconfRead(final DataBroker mountpoint,
                                                        final InstanceIdentifier<U> readIID) {
        final java.util.Optional<ReadOnlyTransaction> optionalReadTransaction =
                NetconfTransactionCreator.netconfReadOnlyTransaction(mountpoint);
        if (!optionalReadTransaction.isPresent()) {
            LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
            return null;
        }
        final ReadOnlyTransaction transaction = optionalReadTransaction.get();
        try {
            final CheckedFuture<Optional<U>, ReadFailedException> submitFuture =
                    transaction.read(LogicalDatastoreType.CONFIGURATION, readIID);
            final Optional<U> optional = submitFuture.checkedGet();
            if (optional != null && optional.isPresent()) {
                transaction.close(); // Release lock
                return optional.get();
            }
        } catch (ReadFailedException e) {
            LOG.warn("Read transaction failed to {} ", e);
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }

        return null;
    }
}
