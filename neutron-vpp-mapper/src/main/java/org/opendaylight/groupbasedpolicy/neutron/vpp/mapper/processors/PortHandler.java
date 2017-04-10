/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.ExcludeFromPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.ExcludeFromPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.LoopbackCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.LoopbackCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.TapCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425._interface.attributes._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.PortBindingExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev150712.binding.attributes.VifDetails;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.RouterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.SubnetKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
public class PortHandler implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(PortHandler.class);

    private static final String COMPUTE_OWNER = "compute";
    private static final String DHCP_OWNER = "dhcp";
    static final String ROUTER_OWNER = "network:router_interface";
    private static final String[] SUPPORTED_DEVICE_OWNERS = {COMPUTE_OWNER, DHCP_OWNER, ROUTER_OWNER};
    private static final String VHOST_USER = "vhostuser";
    private static final String UNBOUND = "unbound";
    private static final String VPP_INTERFACE_NAME_PREFIX = "neutron_port_";
    private static final String TAP_PORT_NAME_PREFIX = "tap";
    private static final String RT_PORT_NAME_PREFIX = "qr-";
    private static final String VHOST_SOCKET_KEY = "vhostuser_socket";
    static final String DEFAULT_NODE = "default";

    private final NodeId routingNode;
    private BindingTransactionChain transactionChain;
    private DataBroker dataBroker;

    PortHandler(DataBroker dataBroker, NodeId routingNodeId) {
        this.dataBroker = dataBroker;
        this.routingNode = routingNodeId;
        transactionChain = this.dataBroker.createTransactionChain(this);
    }

    void processCreated(Port port) {
        ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
        Optional<BaseEndpointByPort> optBaseEpByPort = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                createBaseEpByPortIid(port.getUuid()), rTx);
        rTx.close();
        if (!optBaseEpByPort.isPresent()) {
            return;
        }
        processCreatedData(port, optBaseEpByPort.get());
    }

    void processCreated(BaseEndpointByPort bebp) {
        ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
        Optional<Port> optPort = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                createPortIid(bebp.getPortId()), rTx);
        rTx.close();
        if (!optPort.isPresent()) {
            return;
        }
        processCreatedData(optPort.get(), bebp);
    }

    @VisibleForTesting
    void processCreatedData(Port port, BaseEndpointByPort bebp) {
        if (isValidVhostUser(port)
                // this is a hack for vpp router port
                // Openstack does not send binding details yet
                || isValidVppRouterPort(port)) {
            VppEndpoint vppEp = buildVppEndpoint(port, bebp);
            if (vppEp == null) {
                LOG.warn("Cannot create vpp-endpoint from neutron port {}", port);
                return;
            }
            writeVppEndpoint(createVppEndpointIid(vppEp.getKey()), vppEp);
            LOG.debug("Created vpp-endpoint {}", vppEp);
        }
    }

    private boolean isValidVhostUser(Port port) {
        PortBindingExtension portBindingExt = port.getAugmentation(PortBindingExtension.class);
        if (portBindingExt != null) {
            String vifType = portBindingExt.getVifType();
            String deviceOwner = port.getDeviceOwner();
            if (vifType != null && deviceOwner != null) {
                if (vifType.contains(VHOST_USER)) {
                    for (String supportedDeviceOwner : SUPPORTED_DEVICE_OWNERS) {
                        if (deviceOwner.contains(supportedDeviceOwner)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    void processUpdated(Port original, Port delta) {
        if (!isUpdateNeeded(original, delta)){
            LOG.trace("Port update skipped, port didn`t change. before {}, after: {}" , original, delta);
            return;
        }

        LOG.trace("Updating port before: {}, after: {}" , original, delta);
        if (isValidVhostUser(original)) {
            ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
            Optional<BaseEndpointByPort> optBebp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    createBaseEpByPortIid(original.getUuid()), rTx);
            rTx.close();
            if (!optBebp.isPresent()) {
                return;
            }
            LOG.trace("Updating port - deleting old port {}" , optBebp.get().getPortId());
            processDeleted(optBebp.get());
        }
        LOG.trace("Updating port - creating new port {}" , delta.getUuid());
        processCreated(delta);
    }

    private boolean isUpdateNeeded(final Port oldPort, final Port newPort) {
        //TODO fix this to better support update of ports for VPP
        final PortBindingExtension oldPortAugmentation = oldPort.getAugmentation(PortBindingExtension.class);
        final PortBindingExtension newPortAugmentation = newPort.getAugmentation(PortBindingExtension.class);

        if (newPortAugmentation == null) {
            LOG.trace("Port {} is no longer a vhost type port, updating port...");
            return true;
        }

        final String oldDeviceOwner = oldPort.getDeviceOwner();
        final String oldVifType = oldPortAugmentation.getVifType();
        final String newDeviceOwner = newPort.getDeviceOwner();
        final String newVifType = newPortAugmentation.getVifType();

        // TODO potential bug here
        // Temporary change for Openstack Mitaka: If old neutron-binding:vif-type is vhost, new one is unbound and
        // device owner is ROUTER_OWNER, skip update. Openstack (or ml2) sometimes sends router update messages in
        // incorrect order which causes unwanted port removal
        if (oldVifType.equals(VHOST_USER) && newVifType.equals(UNBOUND) && oldDeviceOwner != null &&
                ROUTER_OWNER.equals(oldDeviceOwner) && ROUTER_OWNER.equals(newDeviceOwner)) {
            LOG.warn("Port vif-type was updated from vhost to unbound. This update is currently disabled and will be skipped");
            return false;
        }

        if (newVifType != null && !newVifType.equals(oldVifType)) {
            LOG.trace("Vif type changed, old: {} new {}", oldVifType, newVifType);
            return true;
        }

        final List<VifDetails> vifDetails = oldPortAugmentation.getVifDetails();

        if (!oldPortAugmentation.getHostId().equals(newPortAugmentation.getHostId()) ||
            nullToEmpty(vifDetails).size() != nullToEmpty(newPortAugmentation.getVifDetails()).size()) {
            return true;
        }

        for (VifDetails vifDetail : nullToEmpty(vifDetails)) {
            //check if vhostuser_socket, vhostuser_mode and port_filter are changed
            if (!newPortAugmentation.getVifDetails().contains(vifDetail))
                return true;
        }
        return false;
    }

    void processDeleted(BaseEndpointByPort bebp) {
        LOG.trace("Deleting vpp-endpoint by BaseEndpointByPort {}" , bebp);
        VppEndpointKey vppEpKey = new VppEndpointKey(bebp.getAddress(), bebp.getAddressType(), bebp.getContextId(),
                bebp.getContextType());
        InstanceIdentifier<VppEndpoint> vppEpIid = createVppEndpointIid(vppEpKey);
        ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
        Optional<VppEndpoint> readVppEp = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, vppEpIid, rTx);
        rTx.close();
        if (readVppEp.isPresent()) {
            writeVppEndpoint(vppEpIid, null);
            LOG.debug("Deleted vpp-endpoint {}", vppEpKey);
        }
    }

    private synchronized void writeVppEndpoint(InstanceIdentifier<VppEndpoint> vppEpIid, VppEndpoint vppEp) {
        WriteTransaction wTx = transactionChain.newWriteOnlyTransaction();
        if (vppEp != null) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, vppEpIid, vppEp, true);
        } else {
            wTx.delete(LogicalDatastoreType.CONFIGURATION, vppEpIid);
        }
        wTx.submit();
    }

    @VisibleForTesting
    VppEndpoint buildVppEndpoint(Port port, BaseEndpointByPort bebp) {
        PortBindingExtension portBinding = port.getAugmentation(PortBindingExtension.class);
        VppEndpointBuilder vppEpBuilder = new VppEndpointBuilder().setDescription("neutron port")
            .setContextId(bebp.getContextId())
            .setContextType(bebp.getContextType())
            .setAddress(bebp.getAddress())
            .setAddressType(bebp.getAddressType())
            .setVppInterfaceName(VPP_INTERFACE_NAME_PREFIX + bebp.getPortId().getValue())
            .setVppNodeId(new NodeId(portBinding.getHostId()));
        if (port.getDeviceOwner().contains(COMPUTE_OWNER)) {
            vppEpBuilder.setInterfaceTypeChoice(
                new VhostUserCaseBuilder().setSocket(getSocketFromPortBinding(portBinding)).build());
        } else if (port.getDeviceOwner().contains(DHCP_OWNER) && port.getMacAddress() != null) {
            TapCase tapCase = new TapCaseBuilder().setPhysicalAddress(new PhysAddress(port.getMacAddress().getValue()))
                .setName(createPortName(port.getUuid()))
                .build();
            vppEpBuilder.setInterfaceTypeChoice(tapCase);
        } else if (isValidQRouterPort(port)) {
            TapCase tapCase = new TapCaseBuilder().setPhysicalAddress(new PhysAddress(port.getMacAddress().getValue()))
                    .setName(createQRouterPortName(port.getUuid()))
                    .build();
            vppEpBuilder.setInterfaceTypeChoice(tapCase);
            vppEpBuilder.addAugmentation(ExcludeFromPolicy.class,
                    new ExcludeFromPolicyBuilder().setExcludeFromPolicy(true).build());
        } else if (isValidVppRouterPort(port)) {
            if (!DEFAULT_NODE.equals(routingNode.getValue())) {
                LOG.warn(
                        "Host-id changed by ODL for port {}. This is a supplementary workaround for choosing a routing node.",
                        port);
                vppEpBuilder.setVppNodeId(routingNode);
            } else if (port.getDeviceId() != null) {
                LOG.debug("Resolving host-id for unbound router port {}", port.getUuid());
                ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
                Optional<Ports> optPorts = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(Neutron.class).child(Ports.class).build(), readTx);
                readTx.close();
                if (optPorts.isPresent() && optPorts.get().getPort() != null) {
                    java.util.Optional<Port> optPortOnTheSameNode = optPorts.get()
                        .getPort()
                        .stream()
                        .filter(p -> !p.getUuid().equals(port.getUuid()))
                        .filter(p -> p.getAugmentation(PortBindingExtension.class) != null)
                        .filter(p -> p.getDeviceOwner().contains(DHCP_OWNER))
                        .findFirst();
                    if (optPortOnTheSameNode.isPresent()) {
                        PortBindingExtension binding =
                                optPortOnTheSameNode.get().getAugmentation(PortBindingExtension.class);
                        if (binding != null && binding.getHostId() != null) {
                            vppEpBuilder.setVppNodeId(new NodeId(binding.getHostId()));
                        } else {
                            LOG.warn("Cannot resolve location of router-port {}", port.getUuid());
                            return null;
                        }
                    }
                }
            }
            vppEpBuilder.addAugmentation(ExcludeFromPolicy.class,
                    new ExcludeFromPolicyBuilder().setExcludeFromPolicy(true).build());
            vppEpBuilder.setInterfaceTypeChoice(getLoopbackCase(port));
        }
        return vppEpBuilder.build();
    }

    private String getSocketFromPortBinding(@Nonnull PortBindingExtension portBindingExtension) {
        List<VifDetails> vifDetails = nullToEmpty(portBindingExtension.getVifDetails());

        for (VifDetails detail : vifDetails) {
            if (VHOST_SOCKET_KEY.equalsIgnoreCase(detail.getDetailsKey())) {
                return detail.getValue();
            }
        }
        return null;
    }

    private LoopbackCase getLoopbackCase(Port port) {
        LoopbackCaseBuilder loopbackCase = new LoopbackCaseBuilder()
            .setPhysAddress(new PhysAddress(port.getMacAddress().getValue()));
        Optional<FixedIps> fixedIpsOptional = resolveFirstFixedIps(port);
        if(fixedIpsOptional.isPresent() && fixedIpsOptional.get().getIpAddress() != null){
            loopbackCase.setIpAddress(fixedIpsOptional.get().getIpAddress());
            ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
            Optional<Subnet> subnetOptional =
                DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.builder(Neutron.class)
                        .child(Subnets.class)
                        .child(Subnet.class, new SubnetKey(fixedIpsOptional.get().getSubnetId()))
                        .build(), rTx);
            if (subnetOptional.isPresent()) {
                Ipv4Prefix ipv4Prefix = subnetOptional.get().getCidr().getIpv4Prefix();
                loopbackCase.setIpPrefix(new IpPrefix(ipv4Prefix));
            } else {
                LOG.warn("IpPrefix for loopback port: {} was not set.", port);
            }
            if (loopbackCase.getIpAddress() != null && loopbackCase.getIpPrefix() != null) {
                loopbackCase.setBvi(true);
                LOG.trace("Creating loopback BVI interface: {} for VPP router port: {}.", loopbackCase, port);
            }

        } else {
            LOG.warn("IpAddress for loopback port: {} was not set.", port);
        }
        return loopbackCase.build();
    }

    /**
     * If Qrouter (L3 Agent) is in use, any of Openstack neutron routers is not going be mapped
     * to ODL neutron.
     */
    private boolean isValidQRouterPort(Port port) {
        Optional<Router> optRouter = getRouterOptional(port);
        return !optRouter.isPresent() && port.getDeviceOwner().contains(ROUTER_OWNER)
                && port.getMacAddress() != null;
    }

    private boolean isValidVppRouterPort(Port port) {
        Optional<Router> optRouter = getRouterOptional(port);
        return optRouter.isPresent() && port.getDeviceOwner().contains(ROUTER_OWNER)
            && port.getMacAddress() != null;
    }

    private Optional<Router> getRouterOptional(Port port) {
        if (Strings.isNullOrEmpty(port.getDeviceId())) {
            return Optional.absent();
        }
        RouterKey routerKey = null;
        try {
            routerKey = new RouterKey(new Uuid(port.getDeviceId()));
        } catch (IllegalArgumentException e) {
            // port.getDeviceId() may not match Uuid.PATTERN_CONSTANTS
            return Optional.absent();
        }
        ReadOnlyTransaction rTx = transactionChain.newReadOnlyTransaction();
        InstanceIdentifier<Router> routerIid = InstanceIdentifier.builder(Neutron.class)
            .child(Routers.class)
            .child(Router.class, routerKey)
            .build();
        Optional<Router> optRouter = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, routerIid, rTx);
        rTx.close();
        return optRouter;
    }

    public static Optional<FixedIps> resolveFirstFixedIps(Port port) {
        List<FixedIps> fixedIps = port.getFixedIps();
        if (fixedIps != null && !fixedIps.isEmpty()) {
            return Optional.of(fixedIps.get(0));
        }
        return Optional.absent();
    }

    private String createPortName(Uuid portUuid) {
        String tapPortName;
        String uuid = portUuid.getValue();
        if (uuid != null && uuid.length() >= 12) {
            tapPortName = TAP_PORT_NAME_PREFIX + uuid.substring(0, 11);
        } else {
            tapPortName = TAP_PORT_NAME_PREFIX + uuid;
        }
        return tapPortName;
    }

    private String createQRouterPortName(Uuid portUuid) {
        String tapPortName;
        String uuid = portUuid.getValue();
        if (uuid != null && uuid.length() >= 12) {
            tapPortName = RT_PORT_NAME_PREFIX + uuid.substring(0, 11);
        } else {
            tapPortName = RT_PORT_NAME_PREFIX + uuid;
        }
        return tapPortName;
    }

    private InstanceIdentifier<VppEndpoint> createVppEndpointIid(VppEndpointKey vppEpKey) {
        return InstanceIdentifier.builder(Config.class).child(VppEndpoint.class, vppEpKey).build();
    }

    private InstanceIdentifier<BaseEndpointByPort> createBaseEpByPortIid(Uuid uuid) {
        return createBaseEpByPortIid(new UniqueId(uuid.getValue()));
    }

    private InstanceIdentifier<BaseEndpointByPort> createBaseEpByPortIid(UniqueId uuid) {
        return InstanceIdentifier.builder(Mappings.class)
            .child(GbpByNeutronMappings.class)
            .child(BaseEndpointsByPorts.class)
            .child(BaseEndpointByPort.class, new BaseEndpointByPortKey(uuid))
            .build();
    }

    InstanceIdentifier<Port> createWildcartedPortIid() {
        return portsIid().child(Port.class).build();
    }

    private InstanceIdentifier<Port> createPortIid(UniqueId uuid) {
        return portsIid().child(Port.class, new PortKey(new Uuid(uuid.getValue()))).build();
    }

    private InstanceIdentifierBuilder<Ports> portsIid() {
        return InstanceIdentifier.builder(Neutron.class).child(Ports.class);
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
            Throwable cause) {
        LOG.error("Transaction chain failed. {} \nTransaction which caused the chain to fail {}", cause.getMessage(),
                transaction, cause);
        transactionChain.close();
        transactionChain = dataBroker.createTransactionChain(this);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.trace("Transaction chain was successful. {}", chain);
    }

    private <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
