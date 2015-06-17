package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;

public class NeutronSecurityRuleAwareTest {

    @Test
    public final void testIsDirectionOpposite_InIn() {
        assertFalse(NeutronSecurityRuleAware.isDirectionOpposite(Direction.In, Direction.In));
    }

    @Test
    public final void testIsDirectionOpposite_OutOut() {
        assertFalse(NeutronSecurityRuleAware.isDirectionOpposite(Direction.Out, Direction.Out));
    }

    @Test
    public final void testIsDirectionOpposite_InOut() {
        assertTrue(NeutronSecurityRuleAware.isDirectionOpposite(Direction.In, Direction.Out));
    }

    @Test
    public final void testIsDirectionOpposite_OutIn() {
        assertTrue(NeutronSecurityRuleAware.isDirectionOpposite(Direction.Out, Direction.In));
    }

}
