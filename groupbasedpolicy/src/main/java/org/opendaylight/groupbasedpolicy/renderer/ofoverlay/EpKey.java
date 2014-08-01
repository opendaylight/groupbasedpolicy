/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2ContextId;

/**
 * A key for a single endpoint
 */
@Immutable
public class EpKey {

    @Override
    public String toString() {
        return "EpKey [l2Context=" + l2Context + ", macAddress=" + macAddress +
               "]";
    }

    final L2ContextId l2Context;
    final MacAddress macAddress;
    
    public EpKey(L2ContextId l2Context, MacAddress macAddress) {
        super();
        this.l2Context = l2Context;
        this.macAddress = macAddress;
    }

    public L2ContextId getL2Context() {
        return l2Context;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result +
                 ((l2Context == null) ? 0 : l2Context.hashCode());
        result = prime * result +
                 ((macAddress == null) ? 0 : macAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EpKey other = (EpKey) obj;
        if (l2Context == null) {
            if (other.l2Context != null)
                return false;
        } else if (!l2Context.equals(other.l2Context))
            return false;
        if (macAddress == null) {
            if (other.macAddress != null)
                return false;
        } else if (!macAddress.equals(other.macAddress))
            return false;
        return true;
    }
    
}