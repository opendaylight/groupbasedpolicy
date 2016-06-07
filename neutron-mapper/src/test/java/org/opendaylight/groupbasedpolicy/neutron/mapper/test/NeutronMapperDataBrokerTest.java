package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;

/**
 * Loads only modules of GBP and it's dependencies for data broker.
 * <br>
 * Therefore this implementation is faster than {@link AbstractDataBrokerTest}
 */
public class NeutronMapperDataBrokerTest extends CustomDataBrokerTest {

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(Tenants.class, Forwarding.class, SubnetAugmentForwarding.class,
                Mappings.class);
    }

}
