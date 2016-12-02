/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.hostconfigs;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.util.HostconfigUtil;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.Renderers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.RendererKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.Hostconfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.Hostconfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.HostconfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppNodeListener extends DataTreeChangeHandler<RendererNode> {

    private static final Logger LOG = LoggerFactory.getLogger(VppNodeListener.class);

    private SocketInfo socketInfo;
    public static final RendererName VPP_RENDERER_NAME = new RendererName("vpp-renderer");

    public VppNodeListener(DataBroker dataBroker, SocketInfo socketInfo) {
        super(dataBroker);
        this.socketInfo = socketInfo;
        registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, getVppNodeWildcardIid()));
    }

    private InstanceIdentifier<RendererNode> getVppNodeWildcardIid() {
        return InstanceIdentifier.builder(Renderers.class)
            .child(Renderer.class, new RendererKey(VPP_RENDERER_NAME))
            .child(RendererNodes.class)
            .child(RendererNode.class)
            .build();
    }

    @Override
    protected void onWrite(DataObjectModification<RendererNode> rootNode,
            InstanceIdentifier<RendererNode> rootIdentifier) {
        writeData(rootNode.getDataAfter());
    }

    @Override
    protected void onDelete(DataObjectModification<RendererNode> rootNode,
            InstanceIdentifier<RendererNode> rootIdentifier) {
        deleteData(rootNode.getDataBefore());
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<RendererNode> rootNode,
            InstanceIdentifier<RendererNode> rootIdentifier) {
        deleteData(rootNode.getDataBefore());
        writeData(rootNode.getDataAfter());
    }

    private void writeData(RendererNode rendererNode) {
        NodeKey nodeKey = rendererNode.getNodePath().firstKeyOf(Node.class);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        Hostconfig hcData = HostconfigUtil.createHostconfigsDataFor(nodeKey.getNodeId(), socketInfo);
        wTx.put(LogicalDatastoreType.OPERATIONAL, hostconfigIid(nodeKey.getNodeId()), hcData);
        DataStoreHelper.submitToDs(wTx);
        LOG.info("Hostconfig data written to DS for VPP node {}", nodeKey);
    }

    private InstanceIdentifier<Hostconfig> hostconfigIid(NodeId nodeId) {
        return InstanceIdentifier.builder(Neutron.class)
            .child(Hostconfigs.class)
            .child(Hostconfig.class, new HostconfigKey(nodeId.getValue(), HostconfigUtil.L2_HOST_TYPE))
            .build();
    }

    private void deleteData(RendererNode rendererNode) {
        NodeKey nodeKey = rendererNode.getNodePath().firstKeyOf(Node.class);
        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL, hostconfigIid(nodeKey.getNodeId()), rwTx);
        DataStoreHelper.submitToDs(rwTx);
        LOG.info("Hostconfig data removed from DS for VPP node {}", nodeKey);
    }
}
