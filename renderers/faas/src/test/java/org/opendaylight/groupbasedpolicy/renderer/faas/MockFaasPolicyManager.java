/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.faas.uln.datastore.api.Pair;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.ServiceCommunicationLayer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy.ExternalImplicitGroup;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public class MockFaasPolicyManager extends FaasPolicyManager {

    private Map<EndpointGroupId, EndpointGroup> testEndpointGroups = new HashMap<>();
    private Map<SubnetId, Subnet> testSubnets = new HashMap<>();
    private Map<L2FloodDomainId, L2FloodDomain> testL2FloodDomains = new HashMap<>();
    private Map<L2BridgeDomainId, L2BridgeDomain> testL2BridgeDomains = new HashMap<>();
    private Map<L3ContextId, L3Context> testL3Contextes = new HashMap<>();
    private Map<ContractId, Uuid> testSecIdPerContract = new HashMap<>();
    private Map<TenantId, Uuid> testFaasTenantId = new HashMap<>();
    private ServiceCommunicationLayer comLayer;
    private ExternalImplicitGroup externalImplicitGroup;

    public MockFaasPolicyManager(DataBroker dataBroker, ScheduledExecutorService executor) {
        super(dataBroker, executor);
    }

    // *******************************************************
    // Test Stubs
    // *******************************************************
    @Override
    public void registerTenant(TenantId tenantId, EndpointGroupId epgId) {
        assertTrue("FaasPolicyManager.registerTenant: epgId != null", epgId != null);
        assertTrue("FaasPolicyManager.registerTenant: tenantId != null", tenantId != null);

        assertTrue("FaasPolicyManager.registerTenant: testEndpointGroups.containsKey(epgId)",
                testEndpointGroups.containsKey(epgId));

        registeredTenants.putIfAbsent(tenantId, new ArrayList<ListenerRegistration<DataChangeListener>>());
    }

    @Override
    public void registerSubnetWithEpg(EndpointGroupId epgId, TenantId tenantId, SubnetId subnetId) {
        assertTrue("FaasPolicyManager.registerTenant: epgId != null", epgId != null);
        assertTrue("FaasPolicyManager.registerTenant: tenantId != null", tenantId != null);
        assertTrue("FaasPolicyManager.registerTenant: subnetId != null", subnetId != null);

        assertTrue("FaasPolicyManager.registerTenant: testEndpointGroups.containsKey(epgId)",
                testEndpointGroups.containsKey(epgId));
        assertTrue("FaasPolicyManager.registerTenant: registeredTenants.containsKey(tenantId)",
                registeredTenants.containsKey(tenantId));
        assertTrue("FaasPolicyManager.registerTenant: testSubnets.containsKey(subnetId)",
                testSubnets.containsKey(subnetId));
        List<SubnetId> subnets = epgSubnetsMap.get(new Pair<>(epgId, tenantId));
        if (subnets == null) {
            subnets = new ArrayList<>();
        }
        for (SubnetId id : subnets) {
            if (id.equals(subnetId)) {
                return;
            }
        }
        subnets.add(subnetId);
        epgSubnetsMap.put(new Pair<>(epgId, tenantId), subnets);
    }

    @Override
    public EndpointGroup readEndpointGroup(EndpointGroupId epgId, TenantId tenantId) {
        return testEndpointGroups.get(epgId);
    }

    @Override
    public Subnet readSubnet(SubnetId subnetId, TenantId tenantId) {
        return testSubnets.get(subnetId);
    }

    @Override
    protected L2FloodDomain readL2FloodDomain(L2FloodDomainId l2fId, TenantId tenantId) {
        return testL2FloodDomains.get(l2fId);
    }

    @Override
    protected L2BridgeDomain readL2BridgeDomainInstance(TenantId tenantId, L2BridgeDomainId l2bId) {
        return testL2BridgeDomains.get(l2bId);
    }

    @Override
    protected L3Context readL3ContextInstance(TenantId tenantId, L3ContextId l3cId) {
        return testL3Contextes.get(l3cId);
    }

    @Override
    protected boolean needToCreateLogicalNetwork(ServiceCommunicationLayer comLayer, List<SubnetId> consSubnetIds,
            List<SubnetId> provSubnetIds, TenantId tenantId, ContractId contractId, EndpointGroup providerEpg,
            EndpointGroup consumerEpg, ExternalImplicitGroup externalImplicitGroup) {
        return true;
    }

    @Override
    protected Uuid getFaasSecRulesId(ContractId contractId, TenantId gbpTenantId) {
        return testSecIdPerContract.get(contractId);
    }

    @Override
    public Uuid getFaasTenantId(TenantId tenantId) {
        return testFaasTenantId.get(tenantId);
    }

    @Override
    protected void createLayer3LogicalNetwork(EndpointGroup consEpg, ContractId contractId, EndpointGroup provEpg,
            TenantId gbpTenantId, ServiceCommunicationLayer comLayer, ExternalImplicitGroup externalImplicitGroup) {
        this.comLayer = comLayer;
        this.externalImplicitGroup = externalImplicitGroup;
        assertTrue("FaasPolicyManager.createLayer3LogicalNetwork", testEndpointGroups.containsKey(consEpg.getId()));
        assertTrue("FaasPolicyManager.createLayer3LogicalNetwork", testEndpointGroups.containsKey(provEpg.getId()));
        assertTrue("FaasPolicyManager.createLayer3LogicalNetwork", registeredTenants.containsKey(gbpTenantId));

    }

    @Override
    protected void createLayer2LogicalNetwork(EndpointGroup consEpg, ContractId contractId, EndpointGroup provEpg,
            TenantId gbpTenantId, ServiceCommunicationLayer comLayer, ExternalImplicitGroup externalImplicitGroup) {
        this.comLayer = comLayer;
        this.externalImplicitGroup = externalImplicitGroup;
        assertTrue("FaasPolicyManager.createLayer2LogicalNetwork", testEndpointGroups.containsKey(consEpg.getId()));
        assertTrue("FaasPolicyManager.createLayer2LogicalNetwork", testEndpointGroups.containsKey(provEpg.getId()));
        assertTrue("FaasPolicyManager.createLayer2LogicalNetwork", registeredTenants.containsKey(gbpTenantId));
    }

    // *******************************************************
    // The following Methods are to input test data
    // *******************************************************

    public void storeTestFaasTenantId(TenantId gbpTenantId, Uuid faasTenantId) {
        testFaasTenantId.put(gbpTenantId, faasTenantId);
    }

    public void storeTestSecIdPerContract(ContractId contractId, Uuid secId) {
        testSecIdPerContract.put(contractId, secId);
    }

    public void storeTestL2BridgeDomain(L2BridgeDomain brdg) {
        if (brdg.getId() != null) {
            testL2BridgeDomains.put(brdg.getId(), brdg);
        }
    }

    public void storeTestL3Contextes(L3Context l3Context) {
        if (l3Context.getId() != null) {
            testL3Contextes.put(l3Context.getId(), l3Context);
        }
    }

    public void storeTestL2FloodDomain(L2FloodDomain fld) {
        if (fld.getId() != null) {
            testL2FloodDomains.put(fld.getId(), fld);
        }
    }

    public void storeTestSubnet(Subnet subnet) {
        if (subnet.getId() != null) {
            testSubnets.put(subnet.getId(), subnet);
        }
    }

    public void storeTestEpg(EndpointGroup epg) {
        if (epg.getId() != null) {
            testEndpointGroups.put(epg.getId(), epg);
        }
    }

    public ServiceCommunicationLayer getComLayer() {
        return comLayer;
    }

    public ExternalImplicitGroup getExternalImplicitGroup() {
        return externalImplicitGroup;
    }
}
