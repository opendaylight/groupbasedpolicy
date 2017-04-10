/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.EndpointRegistrator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.SubnetAugmentForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.Forwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Tenants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;

import com.google.common.collect.ImmutableList;

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

    protected EndpointRegistrator getEpRegistrator() {
        EndpointRegistrator epRegistrator = mock(EndpointRegistrator.class);
        when(epRegistrator.registerEndpoint(any(RegisterEndpointInput.class))).thenReturn(true);
        when(epRegistrator.registerEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput.class)))
                    .thenReturn(true);
        when(epRegistrator.unregisterEndpoint(any(UnregisterEndpointInput.class))).thenReturn(true);
        when(epRegistrator.unregisterEndpoint(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput.class)))
                    .thenReturn(true);
        return epRegistrator;
    }

}
