/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface NeutronAware<T extends DataObject> {

    void onCreated(T createdItem, Neutron neutron);
    void onUpdated(T oldItem, T newItem, Neutron oldNeutron, Neutron newNeutron);
    void onDeleted(T deletedItem, Neutron oldNeutron, Neutron newNeutron);

}
