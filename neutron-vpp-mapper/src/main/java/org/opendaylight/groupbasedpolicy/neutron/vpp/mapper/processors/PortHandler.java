/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.GbpByNeutronMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.BaseEndpointsByPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.base.endpoints.by.ports.BaseEndpointByPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.vpp.endpoint._interface.type.choice.VhostUserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class PortHandler  implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(MappingProvider.class);

    private BindingTransactionChain transactionChain;
    BaseEndpointByPortListener portByBaseEpListener;
    DataBroker dataBroker;

    PortHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        transactionChain = this.dataBroker.createTransactionChain(this);
    }

    void processCreated(Port port) {
        ReadTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<BaseEndpointByPort> optBaseEpByPort = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                createBaseEpByPortIid(port.getUuid()), rTx);
        if (!optBaseEpByPort.isPresent()) {
            return;
        }
        processCreatedData(port, optBaseEpByPort.get());
    }

    void processCreated(BaseEndpointByPort bebp) {
        ReadTransaction rTx = dataBroker.newReadOnlyTransaction();
        Optional<Port> optPort = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, createPortIid(
                bebp.getPortId()), rTx);
        if (!optPort.isPresent()) {
            return;
        }
        processCreatedData(optPort.get(), bebp);
    }

    @VisibleForTesting
    void processCreatedData(Port port, BaseEndpointByPort bebp) {
        // port not used yet
        VppEndpoint vppEp = buildVppEp(bebp);
        writeVppEndpoint(createVppEndpointIid(vppEp.getKey()), vppEp, true);
        LOG.debug("Created vpp-endpoint {}", vppEp);
    }

    void processUpdated(Port original, Port delta) {
        processCreated(delta);
    }

    void processDeleted(BaseEndpointByPort bebp) {
        VppEndpoint vppEp = buildVppEp(bebp);
        writeVppEndpoint(createVppEndpointIid(vppEp.getKey()), vppEp, false);
        LOG.debug("Deleted vpp-endpoint {}", vppEp);
    }

    private void writeVppEndpoint(InstanceIdentifier<VppEndpoint> vppEpIid, VppEndpoint vppEp, boolean created) {
        WriteTransaction wTx = transactionChain.newWriteOnlyTransaction();
        InstanceIdentifier<VppEndpoint> iid = createVppEndpointIid(vppEp.getKey());
        if (created == true) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, iid, vppEp, true);
        } else {
            wTx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        }
        try {
            wTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction chain commit failed. {}", e);
            transactionChain.close();
            transactionChain = dataBroker.createTransactionChain(this);
        }
    }

    @VisibleForTesting
    VppEndpoint buildVppEp(BaseEndpointByPort bebp) {
        return new VppEndpointBuilder().setContextId(bebp.getContextId())
            .setContextType(bebp.getContextType())
            .setAddress(bebp.getAddress())
            .setInterfaceTypeChoice(new VhostUserCaseBuilder().setSocket(bebp.getPortId().getValue()).build())
            .setAddressType(bebp.getAddressType())
            .build();
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
        LOG.error("Transaction chain failed. {}", cause.getMessage());
        transactionChain.close();
        transactionChain = dataBroker.createTransactionChain(this);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.trace("Transaction chain was successfull. {}", chain);
    }
}
