package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.NetworkMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.mapper.rev150223.mappings.network.mappings.NetworkMapping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NeutronMapperIidFactoryTest {

    @Test
    public void instantiate() {
        NeutronMapperIidFactory iidFactory = new NeutronMapperIidFactory();
        assertNotNull(iidFactory);
    }

    @Test
    public void testEndpointByPortIid() {
        String dummyId = "00000000-0000-0000-0000-000000000001";
        InstanceIdentifier<NetworkMapping> iid = NeutronMapperIidFactory.networkMappingIid(new UniqueId(dummyId));
        assertNotNull(iid);
        assertEquals(iid.getTargetType(), NetworkMapping.class);
        assertTrue(iid.toString().contains(dummyId));

        Iterator<InstanceIdentifier.PathArgument> pathArguments = iid.getPathArguments().iterator();

        assertEquals(pathArguments.next().toString(), Mappings.class.getName());
        assertEquals(pathArguments.next().toString(), NetworkMappings.class.getName());
        assertEquals(pathArguments.next().getType().toString(), NetworkMapping.class.toString());
    }
}
