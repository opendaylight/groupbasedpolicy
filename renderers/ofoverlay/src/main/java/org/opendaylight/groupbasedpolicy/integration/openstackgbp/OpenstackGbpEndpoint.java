package org.opendaylight.groupbasedpolicy.integration.openstackgbp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.OpenstackEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.OpenstackEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.OpenstackEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.openstack.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.openstack.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.openstack.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.openstack.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.openstack.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.openstack.endpoints.EndpointL3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.unregister.endpoint.input.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.openstackendpoint.rev141204.unregister.endpoint.input.L3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class OpenstackGbpEndpoint implements AutoCloseable,
        OpenstackEndpointService {

    public static final InstanceIdentifier<OpenstackEndpoints> OPENSTACKEP_IID = InstanceIdentifier
            .builder(OpenstackEndpoints.class).build();

    private static final Logger LOG = LoggerFactory
            .getLogger(OpenstackGbpEndpoint.class);

    private DataBroker dataProvider;
    private final ScheduledExecutorService executor;

    private final static InstanceIdentifier<Nodes> nodesIid = InstanceIdentifier
            .builder(Nodes.class).build();
    private final static InstanceIdentifier<Node> nodeIid = InstanceIdentifier
            .builder(Nodes.class).child(Node.class).build();
    private ListenerRegistration<DataChangeListener> nodesReg;

    final BindingAwareBroker.RpcRegistration<OpenstackEndpointService> rpcRegistration;

    public OpenstackGbpEndpoint(DataBroker dataProvider,
            RpcProviderRegistry rpcRegistry) {
        super();
        this.dataProvider = dataProvider;
        executor = Executors.newScheduledThreadPool(1);

        if (rpcRegistry != null) {
            rpcRegistration = rpcRegistry.addRpcImplementation(
                    OpenstackEndpointService.class, this);
        } else
            rpcRegistration = null;

        if (dataProvider != null) {
            // XXX - This is a hack to avoid a bug in the data broker
            // API where you have to write all the parents before you can write
            // a child
            InstanceIdentifier<OpenstackEndpoints> iid = InstanceIdentifier
                    .builder(OpenstackEndpoints.class).build();
            WriteTransaction t = this.dataProvider.newWriteOnlyTransaction();
            t.put(LogicalDatastoreType.OPERATIONAL, iid,
                    new OpenstackEndpointsBuilder().build());
            CheckedFuture<Void, TransactionCommitFailedException> f = t
                    .submit();
            Futures.addCallback(f, new FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable t) {
                    LOG.error("Could not write endpoint base container", t);
                }

                @Override
                public void onSuccess(Void result) {
                }
            });
            nodesReg = dataProvider.registerDataChangeListener(
                    LogicalDatastoreType.OPERATIONAL, nodeIid,
                    new NodesListener(), DataChangeScope.SUBTREE);
        }
    }

    // private OpenstackEndpoints buildOpenstackEndpoint(Name neutronPortId,
    // TenantId tenant) {
    // return new OpenstackEndpoints();
    // }

    public void setDataProvider(final DataBroker salDataProvider) {
        this.dataProvider = salDataProvider;
    }

    @Override
    public void close() throws Exception {
        // When we close this service we need to shutdown our executor!
        // executor.shutdown();

        if (dataProvider != null) {
            WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.OPERATIONAL, OPENSTACKEP_IID);
            Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.debug("Delete OS EP commit result: " + result);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Delete of OS EP failed", t);
                }
            });
        }
    }

    // ******************************************************************/
    /**
     * Construct an endpoint with the appropriate augmentations from the
     * endpoint input. This can be overridden by a concrete implementation.
     *
     * @param input
     *            the input object
     * @param timestamp
     *            the current timestamp
     */
    protected EndpointBuilder buildEndpoint(RegisterEndpointInput input) {
        return new EndpointBuilder(input);
    }

    /**
     * Construct an L3 endpoint with the appropriate augmentations from the
     * endpoint input. This can be overridden by a concrete implementation.
     *
     * @param input
     *            the input object
     * @param timestamp
     *            the current timestamp
     */
    protected EndpointL3Builder buildEndpointL3(RegisterEndpointInput input) {
        return new EndpointL3Builder(input);
    }

    @Override
    public Future<RpcResult<Void>> unregisterEndpoint(
            UnregisterEndpointInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        if (input.getL2() != null) {
            for (L2 l2a : input.getL2()) {
                EndpointKey key = new EndpointKey(l2a.getL2Context(),
                        l2a.getMacAddress());
                InstanceIdentifier<Endpoint> iid = InstanceIdentifier
                        .builder(OpenstackEndpoints.class)
                        .child(Endpoint.class, key).build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iid);
            }
        }
        if (input.getL3() != null) {
            for (L3 l3addr : input.getL3()) {
                EndpointL3Key key3 = new EndpointL3Key(l3addr.getIpAddress(),
                        l3addr.getL3Context());
                InstanceIdentifier<EndpointL3> iid_l3 = InstanceIdentifier
                        .builder(OpenstackEndpoints.class)
                        .child(EndpointL3.class, key3).build();
                t.delete(LogicalDatastoreType.OPERATIONAL, iid_l3);
            }
        }
        unregisterStandardEndpoint(input);
        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, executor);
    }

    public Future<RpcResult<Void>> unregisterStandardEndpoint(
            UnregisterEndpointInput input) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        if (input.getL2() != null) {
            for (L2 l2a : input.getL2()) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey key = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey(
                        l2a.getL2Context(), l2a.getMacAddress());
                if (existsL2Endpoint(key)) {
                    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint> iid = InstanceIdentifier
                            .builder(
                                    org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints.class)
                            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint.class,
                                    key).build();
                    t.delete(LogicalDatastoreType.OPERATIONAL, iid);
                }

            }
        }
        if (input.getL3() != null) {
            for (L3 l3addr : input.getL3()) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key key3 = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key(
                        l3addr.getIpAddress(), l3addr.getL3Context());
                if (existsL3Endpoint(key3)) {
                    InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3> iid_l3 = InstanceIdentifier
                            .builder(Endpoints.class)
                            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3.class,
                                    key3).build();
                    t.delete(LogicalDatastoreType.OPERATIONAL, iid_l3);
                }
            }
        }
        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, executor);
    }

    private Boolean existsL2Endpoint(
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey key) {
        Boolean exists = false;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint> iid = InstanceIdentifier
                .builder(Endpoints.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint.class,
                        key).build();

        if (dataProvider != null) {
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint> result;
            try {
                result = dataProvider.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, iid).get();
                if (result.isPresent()) {
                    exists = true;
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return exists;
    }

    private Boolean existsL3Endpoint(
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key keyL3) {
        Boolean exists = false;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3> iidL3 = InstanceIdentifier
                .builder(Endpoints.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3.class,
                        keyL3).build();
        if (dataProvider != null) {
            Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3> result;
            try {
                result = dataProvider.newReadOnlyTransaction()
                        .read(LogicalDatastoreType.OPERATIONAL, iidL3).get();
                if (result.isPresent()) {
                    exists = true;
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return exists;
    }

    @Override
    public Future<RpcResult<Void>> registerEndpoint(RegisterEndpointInput input) {
        long timestamp = System.currentTimeMillis();

        WriteTransaction t = dataProvider.newWriteOnlyTransaction();

        if (input.getL2Context() != null && input.getMacAddress() != null) {
            Endpoint ep = buildEndpoint(input).setTimestamp(timestamp).build();

            EndpointKey key = new EndpointKey(ep.getL2Context(),
                    ep.getMacAddress());
            InstanceIdentifier<Endpoint> iid = InstanceIdentifier
                    .builder(OpenstackEndpoints.class)
                    .child(Endpoint.class, key).build();
            t.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
            NodeInfo nodeInfo = mapNeutronPortToNodeInfo(ep.getNeutronPortId()
                    .getValue());
            if (nodeInfo.getNode() != null
                    && nodeInfo.getNodeConnector() != null) {
                writeNewEp(translateEndpoint(ep, nodeInfo.getNodeConnector()
                        .getId(), nodeInfo.getNode().getId()));
            }
        }
        if (input.getL3Address() != null) {
            for (L3Address l3addr : input.getL3Address()) {
                EndpointL3Key key3 = new EndpointL3Key(l3addr.getIpAddress(),
                        l3addr.getL3Context());
                EndpointL3 ep3 = buildEndpointL3(input)
                        .setIpAddress(key3.getIpAddress())
                        .setL3Context(key3.getL3Context())
                        .setTimestamp(timestamp).build();
                InstanceIdentifier<EndpointL3> iid_l3 = InstanceIdentifier
                        .builder(OpenstackEndpoints.class)
                        .child(EndpointL3.class, key3).build();
                t.put(LogicalDatastoreType.OPERATIONAL, iid_l3, ep3);
                NodeInfo nodeInfo = mapNeutronPortToNodeInfo(ep3
                        .getNeutronPortId().toString());
                if (nodeInfo.getNode() != null
                        && nodeInfo.getNodeConnector() != null) {
                    writeNewEpL3(translateEndpointL3(ep3, nodeInfo
                            .getNodeConnector().getId(), nodeInfo.getNode()
                            .getId()));
                }
            }
        }
        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, executor);
        // Now check for Nodes that match the neutron port id
    }

    // A wrapper class around node, noeConnector info so we can pass a final
    // object inside OnSuccess anonymous inner class
    private static class NodeInfo {
        NodeConnector nodeConnector;
        Node node;

        private NodeInfo() {

        }

        private NodeInfo(NodeConnector nc, Node node) {
            this.nodeConnector = nc;
            this.node = node;
        }

        private Node getNode() {
            return this.node;
        }

        private NodeConnector getNodeConnector() {
            return this.nodeConnector;
        }

        public void setNodeConnector(NodeConnector nodeConnector) {
            this.nodeConnector = nodeConnector;
        }

        public void setNode(Node node) {
            this.node = node;
        }
    }

    private NodeInfo mapNeutronPortToNodeInfo(final String neutronPortId) {
        final NodeInfo nodeInfo = new NodeInfo();

        if (dataProvider != null) {

            ListenableFuture<Optional<Nodes>> future = dataProvider
                    .newReadOnlyTransaction().read(
                            LogicalDatastoreType.OPERATIONAL, nodesIid);

            Futures.addCallback(future, new FutureCallback<Optional<Nodes>>() {
                @Override
                public void onSuccess(Optional<Nodes> result) {
                    if (result.isPresent()) {
                        Nodes nodes = result.get();
                        for (Node node : nodes.getNode()) {
                            if (node.getNodeConnector() != null) {
                                for (NodeConnector nc : node.getNodeConnector()) {
                                    FlowCapableNodeConnector fcnc = nc
                                            .getAugmentation(FlowCapableNodeConnector.class);
                                    if (fcnc.getName().equals(neutronPortId)) {
                                        nodeInfo.setNode(node);
                                        nodeInfo.setNodeConnector(nc);
                                    }
                                }
                            }
                        }

                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOG.error("Count not read switch information", t);
                }
            });
        }
        return nodeInfo;
    }

    private Future<RpcResult<Void>> writeNewEp(
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint ep) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint> iid = InstanceIdentifier
                .builder(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint.class,
                        ep.getKey()).build();
        t.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, executor);

    }

    private Future<RpcResult<Void>> writeNewEpL3(
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3 ep) {
        WriteTransaction t = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3> iid = InstanceIdentifier
                .builder(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3.class,
                        ep.getKey()).build();
        t.put(LogicalDatastoreType.OPERATIONAL, iid, ep);
        ListenableFuture<Void> r = t.submit();
        return Futures.transform(r, futureTrans, executor);

    }

    private boolean validEp(Endpoint endpoint) {
        return (endpoint != null && endpoint.getTenant() != null
                && endpoint.getEndpointGroup() != null
                && endpoint.getL2Context() != null && endpoint.getMacAddress() != null);
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint translateEndpoint(
            Endpoint ep, NodeConnectorId nodeConnectorId, NodeId nodeId) {
        OfOverlayContextBuilder ofOverlayAugmentation = new OfOverlayContextBuilder();
        ofOverlayAugmentation.setNodeId(nodeId);
        ofOverlayAugmentation.setNodeConnectorId(nodeConnectorId);
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder newEpBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder();
        newEpBuilder.addAugmentation(OfOverlayContext.class,
                ofOverlayAugmentation.build());
        newEpBuilder.setCondition(ep.getCondition());
        newEpBuilder.setEndpointGroup(ep.getEndpointGroup());
        newEpBuilder.setL2Context(ep.getL2Context());
        newEpBuilder.setMacAddress(ep.getMacAddress());
        // TODO This is horrible, and a consequence of the
        // openstackendpoint.yang not either including or importing
        // groups from endpoint.yang ... cargo cult code again that needs to be
        // cleaned up.
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey newEpKey = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey(
                ep.getL2Context(), ep.getMacAddress());
        newEpBuilder.setKey(newEpKey);
        // TODO As per above, horrible
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address> newL3AddressList = new ArrayList<>();
        if (ep.getL3Address() != null) {
            for (L3Address l3 : ep.getL3Address()) {
                LOG.debug(l3.toString());
                org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder newL3AddressBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder();
                newL3AddressBuilder.setIpAddress(l3.getIpAddress());
                newL3AddressBuilder.setL3Context(l3.getL3Context());
                newL3AddressList.add(newL3AddressBuilder.build());
            }
        }
        newEpBuilder.setL3Address(newL3AddressList);
        newEpBuilder.setTenant(ep.getTenant());
        newEpBuilder.setTimestamp(ep.getTimestamp());
        return newEpBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3 translateEndpointL3(
            EndpointL3 ep, NodeConnectorId nodeConnectorId, NodeId nodeId) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder newEpL3Builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder();

        newEpL3Builder.setCondition(ep.getCondition());
        newEpL3Builder.setIpAddress(ep.getIpAddress());
        newEpL3Builder.setL3Context(ep.getL3Context());
        newEpL3Builder.setEndpointGroup(ep.getEndpointGroup());
        newEpL3Builder.setL2Context(ep.getL2Context());
        newEpL3Builder.setMacAddress(ep.getMacAddress());
        // TODO This is horrible, and a consequence of the
        // openstackendpoint.yang not either including or importing
        // groups from endpoint.yang ... cargo cult code again that needs to be
        // cleaned up.
        org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key newEpL3Key = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Key(
                ep.getIpAddress(), ep.getL3Context());
        newEpL3Builder.setKey(newEpL3Key);
        // TODO As per above, horrible
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address> newL3AddressList = new ArrayList<>();
        for (L3Address l3 : ep.getL3Address()) {
            LOG.debug(l3.toString());
            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder newL3AddressBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder();
            newL3AddressBuilder.setIpAddress(l3.getIpAddress());
            newL3AddressBuilder.setL3Context(l3.getL3Context());
            newL3AddressList.add(newL3AddressBuilder.build());
        }
        newEpL3Builder.setL3Address(newL3AddressList);
        newEpL3Builder.setTenant(ep.getTenant());
        newEpL3Builder.setTimestamp(ep.getTimestamp());
        return newEpL3Builder.build();
    }

    private void updateOfOverlayEndpoint(final String neutronPortId,
            final NodeConnectorId nodeConnectorId, final NodeId nodeId) {

        if (dataProvider != null) {
            InstanceIdentifier<OpenstackEndpoints> iid = InstanceIdentifier
                    .builder(OpenstackEndpoints.class).build();
            ListenableFuture<Optional<OpenstackEndpoints>> future = dataProvider
                    .newReadOnlyTransaction().read(
                            LogicalDatastoreType.OPERATIONAL, iid);
            Futures.addCallback(future,
                    new FutureCallback<Optional<OpenstackEndpoints>>() {
                        @Override
                        public void onSuccess(
                                Optional<OpenstackEndpoints> result) {
                            if (result.isPresent()) {
                                OpenstackEndpoints openstackEndpoints = result
                                        .get();
                                if (openstackEndpoints.getEndpoint() != null) {
                                    for (Endpoint ep : openstackEndpoints
                                            .getEndpoint()) {
                                        if (validEp(ep)
                                                && ep.getNeutronPortId()
                                                        .getValue()
                                                        .equals(neutronPortId)) {
                                            LOG.debug("Match: " + ep.toString());
                                            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint newEp = translateEndpoint(
                                                    ep, nodeConnectorId, nodeId);
                                            writeNewEp(newEp);
                                            LOG.debug(newEp.toString());
                                        }
                                    }
                                }
                                if (openstackEndpoints.getEndpointL3() != null) {
                                    for (EndpointL3 ep : openstackEndpoints
                                            .getEndpointL3()) {
                                        if (ep.getNeutronPortId().getValue()
                                                .equals(neutronPortId)) {
                                            LOG.debug("L3 Match: "
                                                    + ep.toString());
                                            org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3 newEpL3 = translateEndpointL3(
                                                    ep, nodeConnectorId, nodeId);
                                            writeNewEpL3(newEpL3);
                                            LOG.debug(newEpL3.toString());
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOG.error("Count not read switch information", t);
                        }
                    });
        }
    }

    private class NodesListener implements DataChangeListener {
        @Override
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            for (DataObject dao : change.getCreatedData().values()) {
                if (!(dao instanceof Node))
                    continue;
                Node node = (Node) dao;
                if (node.getNodeConnector() != null) {
                    for (NodeConnector nc : node.getNodeConnector()) {
                        FlowCapableNodeConnector fcnc = nc
                                .getAugmentation(FlowCapableNodeConnector.class);
                        if (fcnc.getName().matches(
                                "tap[a-f,0-9]{8}-[a-f,0-9]{2}")) {
                            LOG.debug("Created Tap:" + fcnc.getName() + ": "
                                    + nc.getId() + " : " + node.getId());
                            updateOfOverlayEndpoint(fcnc.getName(), nc.getId(),
                                    node.getId());
                        }
                    }
                }
            }
            for (DataObject dao : change.getUpdatedData().values()) {
                if (!(dao instanceof Node))
                    continue;
                Node node = (Node) dao;
                if (node.getNodeConnector() != null) {
                    for (NodeConnector nc : node.getNodeConnector()) {
                        FlowCapableNodeConnector fcnc = nc
                                .getAugmentation(FlowCapableNodeConnector.class);
                        if (fcnc.getName().matches(
                                "tap[a-f,0-9]{8}-[a-f,0-9]{2}")) {
                            LOG.debug("Updated Tap:" + fcnc.getName() + ": "
                                    + nc.getId() + " : " + node.getId());
                            updateOfOverlayEndpoint(fcnc.getName(), nc.getId(),
                                    node.getId());
                        }
                    }
                }
            }
        }
    }

    Function<Void, RpcResult<Void>> futureTrans = new Function<Void, RpcResult<Void>>() {
        @Override
        public RpcResult<Void> apply(Void input) {
            return RpcResultBuilder.<Void> success().build();
        }
    };
}
