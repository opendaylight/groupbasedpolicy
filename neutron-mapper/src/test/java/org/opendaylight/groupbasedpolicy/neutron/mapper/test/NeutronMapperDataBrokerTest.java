/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;

/**
 * Loads only modules of GBP and it's dependencies for data broker.
 * <br>
 * Therefore this implementation is faster than {@link AbstractDataBrokerTest}
 */
public class NeutronMapperDataBrokerTest extends CustomDataBrokerTest {

    @Override
    public Collection<Class<?>> getClassesFromModules() {
        return ImmutableList.<Class<?>>of(Neutron.class, Tenants.class, Forwarding.class, SubnetAugmentForwarding.class,
                Mappings.class);
    }

}
