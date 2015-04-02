/*
 * Copyright (c) 2014 Huawei Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.endpoint;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;

/**
 *
 * @author Khal
 *
 */
public interface EpRendererAugmentation {

    void buildEndpointAugmentation(EndpointBuilder eb,
            RegisterEndpointInput input);

    void buildEndpointL3Augmentation(EndpointL3Builder eb,
            RegisterEndpointInput input);

    void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb,
            RegisterL3PrefixEndpointInput input);

}
