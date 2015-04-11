package org.opendaylight.groupbasedpolicy.resolver;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;


public interface ActionInstanceValidator {

    boolean isValid(ActionInstance actionInstance);
}
