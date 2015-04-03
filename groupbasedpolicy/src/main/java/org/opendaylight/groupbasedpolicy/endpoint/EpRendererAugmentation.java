/*
 * Copyright (c) 2015 Huawei Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.endpoint;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3PrefixBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public interface EpRendererAugmentation {

    Augmentation<Endpoint> buildEndpointAugmentation(RegisterEndpointInput input);

    Augmentation<EndpointL3> buildEndpointL3Augmentation(RegisterEndpointInput input);

    void buildL3PrefixEndpointAugmentation(EndpointL3PrefixBuilder eb,
            RegisterL3PrefixEndpointInput input);

}
