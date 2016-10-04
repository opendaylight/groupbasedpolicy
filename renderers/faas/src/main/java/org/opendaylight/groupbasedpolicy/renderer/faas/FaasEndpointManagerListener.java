/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.faas;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IetfModelCodec;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Text;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.endpoints.locations.rev151013.endpoints.locations.container.endpoints.locations.EndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.ports.rev151013.ports.container.ports.port.PrivateIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.ports.rev151013.ports.container.ports.port.PrivateIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class FaasEndpointManagerListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FaasEndpointManagerListener.class);
    private final ScheduledExecutorService executor;
    private final ListenerRegistration<DataChangeListener> registerListener;
    private final FaasPolicyManager policyManager;
    private final DataBroker dataProvider;

    public FaasEndpointManagerListener(FaasPolicyManager policyManager, DataBroker dataProvider,
            ScheduledExecutorService executor) {
        this.executor = executor;
        this.policyManager = policyManager;
        this.dataProvider = dataProvider;
        this.registerListener = checkNotNull(dataProvider).registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                IidFactory.endpointsIidWildcard(), this, AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void close() throws Exception {
        if (registerListener != null)
            registerListener.close();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        executor.execute(new Runnable() {

            public void run() {
                executeEvent(change);
            }
        });
    }

    @VisibleForTesting
    void executeEvent(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        // Create
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint) {
                Endpoint endpoint = (Endpoint) dao;
                LOG.debug("Created Endpoint {}", endpoint);
                if (validate(endpoint)) {
                    policyManager.registerTenant(endpoint.getTenant(), endpoint.getEndpointGroup());
                    processEndpoint(endpoint);
                }
            } else if (dao instanceof EndpointL3) {
                LOG.debug("Created EndpointL3 {}", dao);
            } else if (dao instanceof EndpointL3Prefix) {
                LOG.warn("Not Handled Event Yet by Faas Renderer. Created EndpointL3Prefix {}", dao);
            }
        }
        // Update
        Map<InstanceIdentifier<?>, DataObject> dao = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dao.entrySet()) {
            if (entry.getValue() instanceof Endpoint) {
                Endpoint endpoint = (Endpoint) entry.getValue();
                LOG.debug("Updated Endpoint {}", endpoint);
                if (validate(endpoint)) {
                    policyManager.registerTenant(endpoint.getTenant(), endpoint.getEndpointGroup());
                    processEndpoint(endpoint);
                }
            } else if (entry.getValue() instanceof EndpointL3) {
                LOG.debug("Updated EndpointL3 {}", entry.getValue());
            } else if (entry.getValue() instanceof EndpointL3Prefix) {
                LOG.warn("Not Handled Event Yet by Faas Renderer. Updated EndpointL3Prefix {}", entry.getValue());
            }
        }
        // Remove
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old == null) {
                continue;
            }
            if (old instanceof Endpoint) {
                Endpoint endpoint = (Endpoint) old;
                LOG.debug("Removed Endpoint {}", endpoint);
                removeFaasEndpointLocationIfExist(endpoint.getTenant(), endpoint.getL2Context(),
                        endpoint.getMacAddress());
            } else if (old instanceof EndpointL3) {
                EndpointL3 endpoint = (EndpointL3) old;
                LOG.debug("Removed EndpointL3 {}", endpoint);
                removeFaasEndpointLocationIfExist(endpoint.getTenant(), endpoint.getL2Context(),
                        endpoint.getMacAddress());
            } else if (old instanceof EndpointL3Prefix) {
                LOG.warn("Not Handled Event Yet by Faas Renderer. Removed EndpointL3Prefix {}", old);
            }
        }
    }

    protected void processEndpoint(Endpoint endpoint) {
        Uuid tenantId = policyManager.getFaasTenantId(endpoint.getTenant());
        if (tenantId == null) {
            LOG.error("Failed Endpoint Registration. Couldn't find faas tenant Id. Endpoint {}", endpoint);
            return;
        }
        EndpointLocationBuilder epLocBuilder = new EndpointLocationBuilder();
        epLocBuilder.setDescription(new Text("gbp-endpoint"));
        epLocBuilder.setName(new Text(endpoint.getL2Context().getValue()));
        epLocBuilder.setTenantId(tenantId);
        epLocBuilder.setFaasPortRefId(endpoint.getAugmentation(FaasEndpointContext.class).getFaasPortRefId());
        Uuid epId = getFaasEndpointId(endpoint);
        if (epId == null) {
            LOG.error("Failed Endpoint registration. Couldn't Create Faas Endpoint Id");
            return;
        }
        epLocBuilder.setUuid(epId);
        Uuid faasSubnetId = getFaasSubnetId(endpoint);
        List<PrivateIps> privateIpAddresses = new ArrayList<>();
        for (L3Address ip : endpoint.getL3Address()) {
            PrivateIpsBuilder ipBuilder = new PrivateIpsBuilder();
            ipBuilder.setIpAddress(IetfModelCodec.ipAddress2013(ip.getIpAddress()));
            ipBuilder.setSubnetId(faasSubnetId);
            privateIpAddresses.add(ipBuilder.build());
        }
        if (!UlnDatastoreApi.attachEndpointToSubnet(epLocBuilder, faasSubnetId, IetfModelCodec.macAddress2013(endpoint.getMacAddress()),
                privateIpAddresses, null)) {
            LOG.error("Failed Endpoint Registration. Failed to Attach Endpoint to Faas Logical Network. Endpoint {}",
                    endpoint);
        }
    }

    private Uuid getFaasEndpointId(Endpoint endpoint) {
        MappedEndpoint mEndpoint1 = getMappedEndpoint(endpoint);
        if (mEndpoint1 != null) {
            return mEndpoint1.getEndpointLocation();
        }
        synchronized (this) {// must be atomic
            MappedEndpoint mEndpoint2 = getMappedEndpoint(endpoint);
            if (mEndpoint2 != null) {
                return mEndpoint2.getEndpointLocation();
            }
            MappedEndpointBuilder mBuilder = new MappedEndpointBuilder();
            mBuilder.setL2Context(endpoint.getL2Context());
            mBuilder.setMacAddress(endpoint.getMacAddress());
            mBuilder.setEndpointLocation(new Uuid(UUID.randomUUID().toString()));
            MappedEndpoint mEndpoint = mBuilder.build();
            WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.OPERATIONAL, FaasIidFactory.mappedEndpointIid(
                    endpoint.getTenant(), new MappedEndpointKey(endpoint.getL2Context(), endpoint.getMacAddress())),
                    mEndpoint);
            if (DataStoreHelper.submitToDs(wTx)) {
                LOG.debug("Cached in Datastore Mapped Endpoint {}", mEndpoint);
                return mEndpoint.getEndpointLocation();
            } else {
                LOG.error("Couldn't Cache in Datastore Mapped Endpoint {}", mEndpoint);
                return null;
            }
        }
    }

    @VisibleForTesting
    Uuid getFaasSubnetId(Endpoint endpoint) {
        if (endpoint.getEndpointGroup() == null) {
            LOG.error("Failed Endpoint registration -- No Endpoint-Group Id in endpoint {}", endpoint);
            return null;
        }
        SubnetId subnetId = null;
        if (endpoint.getNetworkContainment() != null) {
            LOG.trace("Subnet is defined based on endpoint containment value {}", endpoint.getNetworkContainment()
                .getValue());
            subnetId = new SubnetId(endpoint.getNetworkContainment());
        }
        if (subnetId == null) {
            LOG.error("Failed Endpoint registration -- Couldn't find a subnet for endpoint {}", endpoint.getKey());
            return null;
        }
        LOG.debug("Using subnetId {} for endpoint {}", subnetId, endpoint.getKey());
        policyManager.registerSubnetWithEpg(endpoint.getEndpointGroup(), endpoint.getTenant(), subnetId);

        Optional<MappedSubnet> subnetOp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedSubnetIid(endpoint.getTenant(), subnetId),
                dataProvider.newReadWriteTransaction());
        if (subnetOp.isPresent()) {
            return subnetOp.get().getFaasSubnetId();
        }
        LOG.error("Failed Endpoint registration -- Couldn't find Mapped Subnet Id based on GBP Subnet Id {}", subnetId);
        return null;
    }

    @VisibleForTesting
    boolean validate(Endpoint endpoint) {
        if (endpoint.getL2Context() == null) {
            LOG.error("Endpoint Failed Validation -- Missing L2 Context. Endpoint {}", endpoint);
            return false;
        }
        if (endpoint.getL3Address() == null) {
            LOG.error("Endpoint Failed Validation -- Missing L3 Address. Endpoint {}", endpoint);
            return false;
        }
        if (endpoint.getMacAddress() == null) {
            LOG.error("Endpoint Failed Validation -- Missing Mac Address. Endpoint {}", endpoint);
            return false;
        }
        if (endpoint.getTenant() == null) {
            LOG.error("Endpoint Failed Validation -- Missing Tenant Id. Endpoint {}", endpoint);
            return false;
        }
        if (endpoint.getEndpointGroup() == null) {
            LOG.error("Endpoint Failed Validation -- Missing Endpoint-Group. Endpoint {}", endpoint);
            return false;
        }
        FaasEndpointContext faasEpAug = endpoint.getAugmentation(FaasEndpointContext.class);
        if (faasEpAug == null || faasEpAug.getFaasPortRefId() == null) {
            LOG.error("Endpoint Failed Validation -- Missing Required Faas Info. Endpoint {}", endpoint);
            return false;
        }
        return true;
    }

    private void removeFaasEndpointLocationIfExist(TenantId tenantId, L2BridgeDomainId l2BridgeDomainId,
            MacAddress macAddress) {
        synchronized (this) {
            MappedEndpointKey mappedEndpointKey = new MappedEndpointKey(l2BridgeDomainId, macAddress);
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            Optional<MappedEndpoint> endpointOp = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    FaasIidFactory.mappedEndpointIid(tenantId, mappedEndpointKey), rwTx);
            DataStoreHelper.submitToDs(rwTx);
            if (endpointOp.isPresent()) {
                UlnDatastoreApi.removeEndpointLocationFromDsIfExists(policyManager.getFaasTenantId(tenantId),
                        endpointOp.get().getEndpointLocation());
            }
        }
    }

    private MappedEndpoint getMappedEndpoint(Endpoint endpoint) {
        MappedEndpointKey mappedEndpointKey = new MappedEndpointKey(endpoint.getL2Context(), endpoint.getMacAddress());
        Optional<MappedEndpoint> endpointOp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedEndpointIid(endpoint.getTenant(), mappedEndpointKey),
                dataProvider.newReadWriteTransaction());
        if (endpointOp.isPresent()) {
            return endpointOp.get();
        }
        return null;
    }
}
