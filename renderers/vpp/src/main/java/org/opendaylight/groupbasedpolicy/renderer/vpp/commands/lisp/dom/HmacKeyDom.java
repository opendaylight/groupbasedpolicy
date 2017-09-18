/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.HmacKeyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.hmac.key.grouping.HmacKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.hmac.key.grouping.HmacKeyBuilder;

public class HmacKeyDom implements CommandModel {

    private HmacKeyType keyType;
    private String key;

    public HmacKeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(HmacKeyType keyType) {
        this.keyType = keyType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public HmacKey getSALObject() {
        return new HmacKeyBuilder()
                    .setKeyType(keyType)
                    .setKey(key).build();
    }

    @Override public String toString() {
        return "HmacKey{" + "keyType=" + keyType + ", key='" + key + '}';
    }
}
