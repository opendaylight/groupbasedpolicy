/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.base.Equivalence;
import com.google.common.base.Objects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;

/**
 * Purpose: hashCode and equals wrapper for any child of {@link AddressEndpointKey}
 */
public class AddressEndpointKeyEquivalence extends Equivalence<AddressEndpointKey> {

    @Override
    protected boolean doEquivalent(final AddressEndpointKey a, final AddressEndpointKey b) {
        if (!Objects.equal(a.getContextType(), b.getContextType())) {
            return false;
        }
        if (!Objects.equal(a.getAddressType(), b.getAddressType())) {
            return false;
        }
        if (!Objects.equal(a.getAddress(), b.getAddress())) {
            return false;
        }
        if (!Objects.equal(a.getContextId(), b.getContextId())) {
            return false;
        }

        return true;
    }

    @Override
    protected int doHash(final AddressEndpointKey addressEndpointKey) {
        return Objects.hashCode(
                addressEndpointKey.getAddress(),
                addressEndpointKey.getAddressType(),
                addressEndpointKey.getContextId(),
                addressEndpointKey.getContextType());
    }
}
