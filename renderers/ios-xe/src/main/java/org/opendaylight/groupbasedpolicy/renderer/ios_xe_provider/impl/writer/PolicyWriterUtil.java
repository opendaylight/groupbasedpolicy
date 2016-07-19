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
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
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
        if (classMapEntries == null || classMapEntries.isEmpty()) {
            return true;
        }
        for (ClassMap entry : classMapEntries) {
            final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                    NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
            if (!optionalWriteTransaction.isPresent()) {
                LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final WriteTransaction writeTransaction = optionalWriteTransaction.get();
            final InstanceIdentifier<ClassMap> classMapIid = classMapInstanceIdentifier(entry);
            writeMergeTransaction(writeTransaction, classMapIid, entry);
            // Check
            final java.util.Optional<ReadOnlyTransaction> optionalTransaction =
                    NetconfTransactionCreator.netconfReadOnlyTransaction(mountpoint);
            if (!optionalTransaction.isPresent()) {
                LOG.warn("Failed to create read-only transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final ReadOnlyTransaction readTransaction = optionalTransaction.get();
            if (checkWritten(readTransaction, classMapIid) == null) {
                return false;
            }
            LOG.info("Created class-map {} on node {}", entry.getName(), nodeId.getValue());
        }
        return true;
    }

    static boolean removeClassMaps(final Set<ClassMap> classMapEntries, final NodeId nodeId, final DataBroker mountpoint) {
        boolean result = true;
        if (classMapEntries == null || classMapEntries.isEmpty()) {
            return true;
        }
        for (ClassMap entry : classMapEntries) {
            final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                    NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
            if (!optionalWriteTransaction.isPresent()) {
                LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final WriteTransaction writeTransaction = optionalWriteTransaction.get();
            final InstanceIdentifier<ClassMap> classMapIid = classMapInstanceIdentifier(entry);
            deleteTransaction(writeTransaction, classMapIid);
            // Check
            final java.util.Optional<ReadOnlyTransaction> optionalReadTransaction =
                    NetconfTransactionCreator.netconfReadOnlyTransaction(mountpoint);
            if (!optionalReadTransaction.isPresent()) {
                LOG.warn("Failed to create read-only transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final ReadOnlyTransaction readTransaction = optionalReadTransaction.get();
            result = checkRemoved(readTransaction, classMapIid);
            LOG.info("Class-map {} removed from node {}", entry.getName(), nodeId.getValue());
        }
        return result;
    }

    static boolean writePolicyMap(final String policyMapName, final Set<Class> policyMapEntries, NodeId nodeId,
                                  final DataBroker mountpoint) {
        final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
        if (!optionalWriteTransaction.isPresent()) {
            LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
            return false;
        }
        final WriteTransaction writeTransaction = optionalWriteTransaction.get();
        final PolicyMap policyMap = PolicyManagerUtil.createPolicyMap(policyMapName, policyMapEntries);
        final InstanceIdentifier<PolicyMap> policyMapIid = policyMapInstanceIdentifier(policyMapName);
        writeMergeTransaction(writeTransaction, policyMapIid, policyMap);
        // Check
        final java.util.Optional<ReadOnlyTransaction> optionalReadTransaction =
                NetconfTransactionCreator.netconfReadOnlyTransaction(mountpoint);
        if (!optionalReadTransaction.isPresent()) {
            LOG.warn("Failed to create read-only transaction, mountpoint: {}", mountpoint);
            return false;
        }
        final ReadOnlyTransaction readTransaction = optionalReadTransaction.get();
        if (checkWritten(readTransaction, policyMapIid) == null) {
            return false;
        }
        LOG.info("Created policy-map {} on node {}", policyMap.getName(), nodeId.getValue());
        return true;
    }

    static boolean removePolicyMapEntries(final String policyMapName, final Set<Class> policyMapEntries,
                                          final NodeId nodeId, final DataBroker mountpoint) {
        if (policyMapEntries == null || policyMapEntries.isEmpty()) {
            return true;
        }
        for (Class entry : policyMapEntries) {
            final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                    NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
            if (!optionalWriteTransaction.isPresent()) {
                LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final WriteTransaction writeTransaction = optionalWriteTransaction.get();
            final InstanceIdentifier policyMapEntryIid = policyMapEntryInstanceIdentifier(policyMapName, entry.getName());
            if (deleteTransaction(writeTransaction, policyMapEntryIid)) {
                LOG.info("Policy map entry {} removed from node {}", entry.getName(), nodeId.getValue());
            }
        }
        return true;
    }

    static boolean writeInterface(final String policyMapName, final String interfaceName, final NodeId nodeId,
                                  final DataBroker mountpoint) {
        final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
        if (!optionalWriteTransaction.isPresent()) {
            LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
            return false;
        }
        final WriteTransaction writeTransaction = optionalWriteTransaction.get();
        final ServicePolicy servicePolicy = PolicyManagerUtil.createServicePolicy(policyMapName, Direction.Input);
        final InstanceIdentifier<ServicePolicy> servicePolicyIid = interfaceInstanceIdentifier(interfaceName);
        writeMergeTransaction(writeTransaction, servicePolicyIid, servicePolicy);
        LOG.info("Service-policy interface {}, bound to policy-map {} created on  node {}",
                interfaceName, policyMapName, nodeId.getValue());
        return true;
    }

    static boolean writeRemote(final Set<ServiceFfName> remoteForwarders, final NodeId nodeId,
                               final DataBroker mountpoint) {
        if (remoteForwarders == null || remoteForwarders.isEmpty()) {
            return true;
        }
        for (ServiceFfName forwarder : remoteForwarders) {
            final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                    NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
            if (!optionalWriteTransaction.isPresent()) {
                LOG.warn("Failed to create transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final WriteTransaction writeTransaction = optionalWriteTransaction.get();
            final InstanceIdentifier<ServiceFfName> forwarderIid = remoteSffInstanceIdentifier(forwarder);
            writeMergeTransaction(writeTransaction, forwarderIid, forwarder);
            LOG.info("Remote forwarder {} created on node {}", forwarder.getName(), nodeId.getValue());
        }
        return true;
    }

    static boolean removeRemote(final Set<ServiceFfName> remoteForwarders, final NodeId nodeId,
                                final DataBroker mountpoint) {
        if (remoteForwarders == null || remoteForwarders.isEmpty()) {
            return true;
        }
        for (ServiceFfName forwarder : remoteForwarders) {
            final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                    NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
            if (!optionalWriteTransaction.isPresent()) {
                LOG.warn("Failed to create transaction, mountpoint: {}", mountpoint);
                return false;
            }
            final WriteTransaction writeTransaction = optionalWriteTransaction.get();
            final InstanceIdentifier<ServiceFfName> forwarderIid = remoteSffInstanceIdentifier(forwarder);
            deleteTransaction(writeTransaction, forwarderIid);
            LOG.info("Remote forwarder {} removed from node {}", forwarder.getName(), nodeId.getValue());
        }
        return true;
    }

    static boolean writeServicePaths(final Set<ServiceChain> serviceChains, final NodeId nodeId,
                                     final DataBroker mountpoint) {
        for (org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain serviceChain : serviceChains) {
            for (ServicePath entry : serviceChain.getServicePath()) {
                final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                        NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
                if (!optionalWriteTransaction.isPresent()) {
                    LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
                    return false;
                }
                final WriteTransaction writeTransaction = optionalWriteTransaction.get();
                final InstanceIdentifier<ServicePath> servicePathIid = servicePathInstanceIdentifier(entry.getKey());
                writeMergeTransaction(writeTransaction, servicePathIid, entry);
                LOG.info("Service path with ID: {} created on node {}", entry.getServicePathId(), nodeId.getValue());
            }
        }
        return true;
    }

    static boolean removeServicePaths(final Set<ServiceChain> serviceChains, final NodeId nodeId,
                                      final DataBroker mountpoint) {
        if (serviceChains == null || serviceChains.isEmpty()) {
            return true;
        }
        for (ServiceChain chain : serviceChains) {
            List<ServicePath> servicePaths = chain.getServicePath();
            if (servicePaths == null || servicePaths.isEmpty()) {
                continue;
            }
            for (ServicePath servicePath : servicePaths) {
                final java.util.Optional<WriteTransaction> optionalWriteTransaction =
                        NetconfTransactionCreator.netconfWriteOnlyTransaction(mountpoint);
                if (!optionalWriteTransaction.isPresent()) {
                    LOG.warn("Failed to create write-only transaction, mountpoint: {}", mountpoint);
                    return false;
                }
                final WriteTransaction writeTransaction = optionalWriteTransaction.get();
                final InstanceIdentifier<ServicePath> servicePathIid = servicePathInstanceIdentifier(servicePath.getKey());
                if (deleteTransaction(writeTransaction, servicePathIid)) {
                    LOG.info("Service-path with ID: {} removed from node {}", servicePath.getServicePathId(),
                            nodeId.getValue());
                }
            }
        }
        return true;
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

    private static InstanceIdentifier<Local> localSffInstanceIdentifier() {
        return InstanceIdentifier.builder(Native.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain.class)
                .child(ServiceFunctionForwarder.class)
                .child(Local.class).build();
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

    private static <U extends DataObject> void writeMergeTransaction(final WriteTransaction transaction,
                                                                     final InstanceIdentifier<U> addIID,
                                                                     final U data) {
        try {
            transaction.merge(LogicalDatastoreType.CONFIGURATION, addIID, data);
            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = transaction.submit();
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
    }

    private static <U extends DataObject> boolean deleteTransaction(final WriteTransaction transaction,
                                                                    final InstanceIdentifier<U> addIID) {
        try {
            transaction.delete(LogicalDatastoreType.CONFIGURATION, addIID);
            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = transaction.submit();
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
            return false;
        }
    }

    private static <U extends DataObject> U checkWritten(final ReadOnlyTransaction transaction,
                                                         final InstanceIdentifier<U> readIID) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                final CheckedFuture<Optional<U>, ReadFailedException> submitFuture =
                        transaction.read(LogicalDatastoreType.CONFIGURATION, readIID);
                final Optional<U> optional = submitFuture.checkedGet();
                if (optional != null && optional.isPresent()) {
                    transaction.close(); // Release lock
                    return optional.get();
                } else {
                    // Could take some time until specific configuration appears on device, try to read a few times
                    Thread.sleep(2000L);
                }
            } catch (InterruptedException i) {
                LOG.error("Thread interrupted while waiting ... {} ", i);
            } catch (ReadFailedException e) {
                LOG.warn("Read transaction failed to {} ", e);
            } catch (Exception e) {
                LOG.error("Failed to .. {}", e.getMessage());
            }
        }
        return null;
    }

    private static <U extends DataObject> boolean checkRemoved(final ReadOnlyTransaction transaction,
                                                               final InstanceIdentifier<U> readIID) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                final CheckedFuture<Optional<U>, ReadFailedException> submitFuture =
                        transaction.read(LogicalDatastoreType.CONFIGURATION, readIID);
                final Optional<U> optional = submitFuture.checkedGet();
                if (optional != null && optional.isPresent()) {
                    // Could take some time until specific configuration is removed from the device
                    Thread.sleep(2000L);
                } else {
                    transaction.close(); // Release lock
                    return true;
                }
            } catch (InterruptedException i) {
                LOG.error("Thread interrupted while waiting ... {} ", i);
            } catch (ReadFailedException e) {
                LOG.warn("Read transaction failed to {} ", e);
            } catch (Exception e) {
                LOG.error("Failed to .. {}", e.getMessage());
            }
        }
        return false;
    }

}
