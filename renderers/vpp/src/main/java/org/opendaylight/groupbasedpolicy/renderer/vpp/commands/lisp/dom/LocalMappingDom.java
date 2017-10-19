/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.hmac.key.grouping.HmacKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;

public class LocalMappingDom implements CommandModel {

    private MappingId mappingId;
    private Eid eid;
    private Long vni;
    private String locatorName;
    private HmacKey hmacKey;

    public MappingId getMappingId() {
        return mappingId;
    }

    public void setMappingId(MappingId mappingId) {
        this.mappingId = mappingId;
    }

    public Eid getEid() {
        return eid;
    }

    public void setEid(Eid eid) {
        this.vni = eid.getVirtualNetworkId().getValue();
        this.eid = eid;
    }

    public String getLocatorName() {
        return locatorName;
    }

    public void setLocatorName(String locatorName) {
        this.locatorName = locatorName;
    }

    public void setLocatorSet(LocatorSet locatorSet) {
        this.locatorName = locatorSet.getName();
    }

    public HmacKey getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(HmacKey hmacKey) {
        this.hmacKey = hmacKey;
    }

    public Long getVni() {
        return vni;
    }

    public void setVni(long vni) {
        this.vni = vni;
    }


    @Override
    public LocalMapping getSALObject() {
        Preconditions.checkNotNull(mappingId, "Mapping Id needs to be set!");
        Preconditions.checkNotNull(getVni(), "Vni needs to be set!");

        return new LocalMappingBuilder()
                .setKey(new LocalMappingKey(mappingId))
                .setId(mappingId)
                .setEid(eid)
                .setLocatorSet(locatorName)
                .setHmacKey(hmacKey).build();
    }

    @Override public String toString() {
        return "LocalMapping{" + "mappingId=" + mappingId + ", eid=" + eid + ", vni=" + vni + ", locatorName='"
            + locatorName + ", hmacKey=" + hmacKey + '}';
    }
}
