/*
 * Copyright (c) 2015 Huawei Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.faas.uln.datastore.api.UlnDatastoreApi;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Text;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.subnet.ExternalGateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.subnets.rev151013.subnets.container.subnets.subnet.ExternalGatewaysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedSubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.subnet.Gateways;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.subnet.gateways.Prefixes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class FaasSubnetManagerListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(FaasSubnetManagerListener.class);
    private ConcurrentHashMap<SubnetId, Uuid> mappedSubnets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private final DataBroker dataProvider;
    private final TenantId gbpTenantId;
    private final Uuid faasTenantId;

    public FaasSubnetManagerListener(DataBroker dataProvider, TenantId gbpTenantId, Uuid faasTenantId,
            ScheduledExecutorService executor) {
        this.executor = executor;
        this.faasTenantId = faasTenantId;
        this.gbpTenantId = gbpTenantId;
        this.dataProvider = dataProvider;
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
            if (dao instanceof Subnet) {
                Subnet subnet = (Subnet) dao;
                LOG.debug("Subnet {} is Created.", subnet.getId().getValue());
                UlnDatastoreApi.submitSubnetToDs(initSubnetBuilder(subnet).build());
            }
        }
        // Update
        Map<InstanceIdentifier<?>, DataObject> dao = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dao.entrySet()) {
            if (entry.getValue() instanceof Subnet) {
                Subnet subnet = (Subnet) entry.getValue();
                LOG.debug("Subnet {} is Updated.", subnet.getId().getValue());
                UlnDatastoreApi.submitSubnetToDs(initSubnetBuilder(subnet).build());
            }
        }
        // Remove
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old == null) {
                continue;
            }
            if (old instanceof Subnet) {
                Subnet subnet = (Subnet) old;
                ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
                Optional<MappedSubnet> op = DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                        FaasIidFactory.mappedSubnetIid(gbpTenantId, subnet.getId()), rwTx);
                if (op.isPresent()) {
                    DataStoreHelper.submitToDs(rwTx);
                }
                Uuid faasSubnetId = mappedSubnets.remove(subnet.getId());
                if (faasSubnetId != null) {
                    UlnDatastoreApi.removeSubnetFromDsIfExists(faasTenantId, faasSubnetId);
                }
            }
        }
    }

    public void loadAll(List<Subnet> subnets, List<MappedSubnet> mpSubnets) {
        if (mpSubnets != null) {
            for (MappedSubnet mpSubnet : mpSubnets) {
                mappedSubnets.putIfAbsent(mpSubnet.getGbpSubnetId(), mpSubnet.getFaasSubnetId());
            }
        }
        if (subnets != null) {
            for (Subnet subnet : subnets) {
                LOG.debug("Loading Subnet {}", subnet.getId().getValue());
                UlnDatastoreApi.submitSubnetToDs(initSubnetBuilder(subnet).build());
            }
        }
    }

    protected SubnetBuilder initSubnetBuilder(Subnet gbpSubnet) {
        SubnetBuilder builder = new SubnetBuilder();
        if (gbpSubnet.getGateways() != null) {
            List<ExternalGateways> gateways = new ArrayList<>();
            for (Gateways gw : gbpSubnet.getGateways()) {
                ExternalGatewaysBuilder eb = new ExternalGatewaysBuilder();
                eb.setExternalGateway(gw.getGateway());
                if (gw.getPrefixes() != null) {
                    List<IpPrefix> ipPrefixes = new ArrayList<>();
                    for (Prefixes px : gw.getPrefixes()) {
                        ipPrefixes.add(px.getPrefix());
                    }
                    eb.setPrefixes(ipPrefixes);
                }
                gateways.add(eb.build());
            }
            builder.setExternalGateways(gateways);
        }

        builder.setIpPrefix(gbpSubnet.getIpPrefix());
        builder.setUuid(getFaasSubnetId(gbpSubnet.getId()));
        builder.setName(new Text(gbpSubnet.getId().getValue()));
        if (gbpSubnet.getDescription() != null)
            builder.setDescription(new Text("gbp-subnet: " + gbpSubnet.getDescription().getValue()));
        else
            builder.setDescription(new Text("gbp-subnet"));
        builder.setTenantId(faasTenantId);
        builder.setVirtualRouterIp(gbpSubnet.getVirtualRouterIp());
        // TODO DNS servers
        builder.setDnsNameservers(null);
        // TODO DHCP server
        builder.setEnableDhcp(false);
        return builder;
    }

    private Uuid getFaasSubnetId(SubnetId subnetId) {
        Uuid val = mappedSubnets.get(subnetId);
        if (val != null) {
            return val;
        }
        Uuid faasSubnetId = null;
        if (FaasPolicyManager.isUUid(subnetId.getValue())) {
            faasSubnetId = new Uuid(subnetId.getValue());
        } else {
            faasSubnetId = new Uuid(UUID.randomUUID().toString());
        }
        mappedSubnets.putIfAbsent(subnetId, faasSubnetId);
        val = mappedSubnets.get(subnetId);
        MappedSubnetBuilder builder = new MappedSubnetBuilder();
        builder.setFaasSubnetId(val);
        builder.setGbpSubnetId(subnetId);
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        MappedSubnet result = builder.build();
        wTx.put(LogicalDatastoreType.OPERATIONAL,
                FaasIidFactory.mappedSubnetIid(gbpTenantId, subnetId), result);
        if (DataStoreHelper.submitToDs(wTx)) {
            LOG.debug("Cached in Datastore Mapped Subnet {}", result);
        } else {
            LOG.error("Couldn't Cache in Datastore Mapped Subnet {}", result);
        }
        return val;
    }

}
