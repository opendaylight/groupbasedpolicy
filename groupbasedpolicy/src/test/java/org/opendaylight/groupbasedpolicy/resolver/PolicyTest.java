package org.opendaylight.groupbasedpolicy.resolver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.dto.Policy;

public class PolicyTest {

    private Policy policy;

    @Before
    public void initialisation() {
        policy = new Policy(null);
    }

    @Test
    public void equalsTest() {
        Assert.assertTrue(policy.equals(policy));
        Assert.assertFalse(policy.equals(null));
        Assert.assertFalse(policy.equals(new Object()));

        Policy other = new Policy(null);
        Assert.assertTrue(policy.equals(other));
    }

    @Test
    public void toStringTest() {
        Assert.assertNotNull(policy.toString());
    }
}
