/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.faas.rev151009.mapped.tenants.entities.mapped.entity.MappedEndpointKey;

public class FaasIidFactoryTest {

    @Test
    public void testLogicalNetworksIid() {
        assertNotNull(FaasIidFactory.logicalNetworksIid());
    }

    @Test
    public void testLogicalNetworkIid() {
        EndpointGroupId consumerEpgId = new EndpointGroupId("consumerEpgId");
        TenantId consumerTenantId = new TenantId("consumerTenantId");
        ContractId contractId = new ContractId("contractId");
        EndpointGroupId providerEpgId = new EndpointGroupId("providerEpgId");
        TenantId providerTenantId = new TenantId("providerTenantId");

        assertNotNull(FaasIidFactory.logicalNetworkIid(consumerEpgId, consumerTenantId, contractId, providerEpgId,
                providerTenantId));
    }

    @Test
    public void testMappedTenantsEntitiesIid() {
        assertNotNull(FaasIidFactory.mappedTenantsEntitiesIid());
    }

    @Test
    public void testMappedSubnetIid() {
        TenantId gbpTenantId = new TenantId("gbpTenantId");
        SubnetId subnetId = new SubnetId("subnetId");

        assertNotNull(FaasIidFactory.mappedSubnetIid(gbpTenantId, subnetId));
    }

    @Test
    public void testMappedTenantIid() {
        TenantId gbpTenantId = new TenantId("gbpTenantId");

        assertNotNull(FaasIidFactory.mappedTenantIid(gbpTenantId));
    }

    @Test
    public void testMappedEntityIid() {
        TenantId gbpTenantId = new TenantId("gbpTenantId");

        assertNotNull(FaasIidFactory.mappedEntityIid(gbpTenantId));
    }

    @Test
    public void testMappedContractIid() {
        TenantId gbpTenantId = new TenantId("gbpTenantId");
        ContractId contractId = new ContractId("contractId");

        assertNotNull(FaasIidFactory.mappedContractIid(gbpTenantId, contractId));
    }

    @Test
    public void testMappedEndpointIid() {
        TenantId gbpTenantId = new TenantId("gbpTenantId");
        MappedEndpointKey mappedEndpointKey =
                new MappedEndpointKey(new L2BridgeDomainId("L2BridgeDomainId"), new MacAddress("00:00:00:00:35:02"));

        assertNotNull(FaasIidFactory.mappedEndpointIid(gbpTenantId, mappedEndpointKey));
    }

}
