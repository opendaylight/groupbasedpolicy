/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;


/**
 * Mock version of policy resolver useful for tests
 * @author readams
 */
public class MockPolicyResolver extends PolicyResolver {

    public MockPolicyResolver() {
        super(null, null);
    }

    public void addTenant(Tenant unresolvedTenant) {
        TenantContext context = new TenantContext(null);
        Tenant t = InheritanceUtils.resolveTenant(unresolvedTenant);
        IndexedTenant it = new IndexedTenant(t);
        context.tenant.set(it);
        resolvedTenants.put(unresolvedTenant.getId(), context);
        
        updatePolicy();
    }
}
