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
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
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

    public CheckedFuture<Boolean, TransactionCommitFailedException> commitToDatastore() {
        LOG.info("Configuring policy on node {} ... ", nodeId.getValue());
        // SFC
        boolean localResult = PolicyWriterUtil.writeLocal(localForwarder, nodeId, mountpoint);
        boolean remoteResult = PolicyWriterUtil.writeRemote(remoteForwarders, nodeId, mountpoint);
        boolean servicePathsResult = PolicyWriterUtil.writeServicePaths(serviceChains, nodeId, mountpoint);
        // GBP - maintain order!
        boolean classMapResult = PolicyWriterUtil.writeClassMaps(classMapEntries, nodeId, mountpoint);
        boolean policyMapResult = PolicyWriterUtil.writePolicyMap(policyMapName, policyMapEntries, nodeId, mountpoint);
        boolean interfaceResult = PolicyWriterUtil.writeInterface(policyMapName, interfaceName, nodeId, mountpoint);
        // Result
        LOG.info("Policy configuration on node {} completed", nodeId.getValue());
        return Futures.immediateCheckedFuture(classMapResult && policyMapResult && interfaceResult && localResult
                && remoteResult && servicePathsResult);
    }

    public CheckedFuture<Boolean, TransactionCommitFailedException> removeFromDatastore() {
        LOG.info("Removing policy from node {} ... ", nodeId.getValue());
        // GBP - maintain order!
        boolean policyMapEntriesResult = PolicyWriterUtil.removePolicyMapEntries(policyMapName, policyMapEntries,
                nodeId, mountpoint);
        boolean classMapResult = PolicyWriterUtil.removeClassMaps(classMapEntries, nodeId, mountpoint);
        // TODO remove class map?
        // SFC
        boolean servicePathsResult = PolicyWriterUtil.removeServicePaths(serviceChains, nodeId, mountpoint);
        boolean localResult = PolicyWriterUtil.removeLocal(nodeId, mountpoint);
        // TODO remove remote forwarders
        // Result
        LOG.info("Policy removed from node {}", nodeId.getValue());
        return Futures.immediateCheckedFuture(classMapResult && policyMapEntriesResult && servicePathsResult
                && localResult);
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
