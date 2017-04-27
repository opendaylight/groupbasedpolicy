/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.exception;

/**
 * Created by Shakib Ahmed on 4/2/17.
 */
public class LispNotFoundException extends Exception{
    public LispNotFoundException(final String message) {
        super(message);
    }

}
