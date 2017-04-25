/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Created by Shakib Ahmed on 3/20/17.
 */
public interface CommandModel {
    <T extends DataObject> T getSALObject();
}
