/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import org.opendaylight.neutron.spi.NeutronObject;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface MappingProcessor <D extends DataObject, T extends NeutronObject> {

     int canCreate(T t);
     int canUpdate(T original, T delta);
     int canDelete(T t);

     void updated(T t);
     void created(T t);
     void deleted(T t);

     T convertToNeutron(D d);
}
