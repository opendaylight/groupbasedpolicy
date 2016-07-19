/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer;

import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyWriter {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWriter.class);

    private final DataBroker mountpoint;
    // Local cache
    private final Set<ClassMap> classMapEntries;
    private final Set<Class> policyMapEntries;
    private final Set<ServiceFfName> remoteForwarders;
    private final Set<ServiceChain> serviceChains;
    private final NodeId nodeId;
    private final String interfaceName;
    private final String policyMapName;
    private final String managementIpAddress;

    public PolicyWriter(final DataBroker dataBroker, final String interfaceName, final String ipAddress,
                        final String policyMapName, final NodeId nodeId) {
        classMapEntries = new HashSet<>();
        policyMapEntries = new HashSet<>();
        remoteForwarders = new HashSet<>();
        serviceChains = new HashSet<>();

        this.nodeId = Preconditions.checkNotNull(nodeId);
        mountpoint = Preconditions.checkNotNull(dataBroker);
        managementIpAddress = Preconditions.checkNotNull(ipAddress);
        this.interfaceName = Preconditions.checkNotNull(interfaceName);
        this.policyMapName = Preconditions.checkNotNull(policyMapName);
    }

    public CheckedFuture<Boolean, TransactionCommitFailedException> commitToDatastore() {
        LOG.debug("Configuring policy on node {}, interface {} ... ", nodeId.getValue(), interfaceName);
        if (policyMapEntries.isEmpty()) {
            LOG.debug("Policy map {} is empty, skipping", policyMapName);
            return Futures.immediateCheckedFuture(true);
        }
        // SFC
        boolean remoteResult = PolicyWriterUtil.writeRemote(remoteForwarders, nodeId, mountpoint);
        boolean servicePathsResult = PolicyWriterUtil.writeServicePaths(serviceChains, nodeId, mountpoint);
        // GBP - maintain order!
        boolean classMapResult = PolicyWriterUtil.writeClassMaps(classMapEntries, nodeId, mountpoint);
        boolean policyMapResult = PolicyWriterUtil.writePolicyMap(policyMapName, policyMapEntries, nodeId, mountpoint);
        boolean interfaceResult = PolicyWriterUtil.writeInterface(policyMapName, interfaceName, nodeId, mountpoint);
        // Result
        LOG.info("Policy-map created on node {}, interface {}", nodeId.getValue(), interfaceName);
        return Futures.immediateCheckedFuture(classMapResult && policyMapResult && interfaceResult && remoteResult
                && servicePathsResult);
    }

    public CheckedFuture<Boolean, TransactionCommitFailedException> removeFromDatastore() {
        LOG.debug("Removing policy from node {}, interface {} ... ", nodeId.getValue(), interfaceName);
        if (policyMapEntries.isEmpty()) {
            LOG.debug("Policy map {} is empty, nothing to remove", policyMapName);
            return Futures.immediateCheckedFuture(true);
        }
        // GBP - maintain order!
        boolean policyMapEntriesResult = PolicyWriterUtil.removePolicyMapEntries(policyMapName, policyMapEntries,
                nodeId, mountpoint);
        boolean classMapResult = PolicyWriterUtil.removeClassMaps(classMapEntries, nodeId, mountpoint);
        // SFC
        boolean remoteSffResult = PolicyWriterUtil.removeRemote(remoteForwarders, nodeId, mountpoint);
        boolean servicePathsResult = PolicyWriterUtil.removeServicePaths(serviceChains, nodeId, mountpoint);
        // Result
        LOG.info("Policy-map removed from node {}, interface {}", nodeId.getValue(), interfaceName);
        return Futures.immediateCheckedFuture(classMapResult && policyMapEntriesResult && servicePathsResult
                && remoteSffResult);
    }

    public void cache(ClassMap classMap) {
        classMapEntries.add(classMap);
    }

    public void cache(Class policyMapEntry) {
        this.policyMapEntries.add(policyMapEntry);
    }

    public void cache(ServiceFfName remoteForwarder) {
        remoteForwarders.add(remoteForwarder);
    }

    public void cache(ServiceChain serviceChain) {
        serviceChains.add(serviceChain);
    }

    public String getManagementIpAddress() {
        return managementIpAddress;
    }

    public DataBroker getMountpoint() {
        return mountpoint;
    }

    public NodeId getNodeId() {
        return nodeId;
    }
}
