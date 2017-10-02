/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

interface MappingProvider<T extends DataObject> {

    InstanceIdentifier<T> getNeutronDtoIid();

    void processCreatedNeutronDto(T t);

    void processUpdatedNeutronDto(T original, T delta);

    void processDeletedNeutronDto(T t);
}
