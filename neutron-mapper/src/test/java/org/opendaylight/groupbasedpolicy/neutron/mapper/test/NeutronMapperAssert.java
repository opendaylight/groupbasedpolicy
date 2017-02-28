/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.domain_extension.l2_l3.util.L2L3IidFactory;
import org.opendaylight.groupbasedpolicy.neutron.gbp.util.NeutronGbpIidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronSubnetAware;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.forwarding.by.tenant.NetworkDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NeutronMapperAssert {

    // asserts for port

    public static void assertPortExists(DataBroker dataBroker, Uuid portUuid) {
        Optional<EndpointByPort> portOptional = getEndpointByPortOptional(dataBroker, portUuid);
        assertTrue(portOptional.isPresent());
    }

    public static void assertPortNotExists(DataBroker dataBroker, Uuid portUuid) {
        Optional<EndpointByPort> portOptional = getEndpointByPortOptional(dataBroker, portUuid);
        assertFalse(portOptional.isPresent());
    }

    private static Optional<EndpointByPort> getEndpointByPortOptional(DataBroker dataBroker, Uuid portUuid) {
        UniqueId portId = new UniqueId(portUuid.getValue());
        InstanceIdentifier<EndpointByPort> iid = NeutronGbpIidFactory.endpointByPortIid(portId);
        Optional<EndpointByPort> portOptional;
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            portOptional = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL, iid, rTx);
        }
        return portOptional;
    }

    public static void assertNetworkDomainExists(DataBroker dataBroker, Port port, Subnet subnet, Neutron neutron) {
        Optional<NetworkDomain> opt =
                getNetworkDomainOptional(dataBroker, port.getTenantId(), neutron, subnet);
        assertTrue(opt.isPresent());
    }

    public static void assertNetworkDomainNotExists(DataBroker dataBroker, Port port, Subnet subnet,
            Neutron neutron) {
        Optional<NetworkDomain> opt =
                getNetworkDomainOptional(dataBroker, port.getTenantId(), neutron, subnet);
        assertFalse(opt.isPresent());
    }

    public static void assertNetworkDomainExists(DataBroker dataBroker, Uuid tenantUuid, Subnet subnet,
            Neutron neutron) {
        Optional<NetworkDomain> opt = getNetworkDomainOptional(dataBroker, tenantUuid, neutron, subnet);
        assertTrue(opt.isPresent());
    }

    public static void assertNetworkDomainNotExists(DataBroker dataBroker, Uuid tenantUuid, Subnet subnet,
            Neutron neutron) {
        Optional<NetworkDomain> opt = getNetworkDomainOptional(dataBroker, tenantUuid, neutron, subnet);
        assertFalse(opt.isPresent());
    }

    private static Optional<NetworkDomain> getNetworkDomainOptional(DataBroker dataBroker, Uuid tenantUuid,
            Neutron neutron, Subnet subnet) {
        InstanceIdentifier<NetworkDomain> iid;
        NetworkDomain subnetDomain = NeutronSubnetAware.createSubnet(subnet, neutron);
        TenantId tenantId = new TenantId(tenantUuid.getValue());
        iid = L2L3IidFactory.subnetIid(tenantId, subnetDomain.getNetworkDomainId());
        Optional<NetworkDomain> optional;
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            optional = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, iid, rTx);
        }
        return optional;
    }

}
