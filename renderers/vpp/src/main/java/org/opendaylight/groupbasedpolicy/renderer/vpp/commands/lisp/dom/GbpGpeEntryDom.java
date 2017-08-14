/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.MapReplyAction;

/**
 * Created by Shakib Ahmed on 6/1/17.
 */
public class GbpGpeEntryDom implements CommandModel{
    private String id;
    private Long vrf;
    private Long vni;
    private LocalEid localEid;
    private RemoteEid remoteEid;
    private MapReplyAction action;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVrf() {
        return vrf;
    }

    public void setVrf(Long vrf) {
        this.vrf = vrf;
    }

    public Long getVni() {
        return vni;
    }

    public void setVni(Long vni) {
        this.vni = vni;
    }

    public RemoteEid getRemoteEid() {
        return remoteEid;
    }

    public void setRemoteEid(RemoteEid remoteEid) {
        this.remoteEid = remoteEid;
    }

    public MapReplyAction getAction() {
        return action;
    }

    public LocalEid getLocalEid() {
        return localEid;
    }

    public void setLocalEid(LocalEid localEid) {
        this.localEid = localEid;
    }

    public void setAction(MapReplyAction action) {
        this.action = action;
    }

    @Override
    public GpeEntry getSALObject() {
        Preconditions.checkNotNull(id, "Gpe Entry Id need to be set!");
        return new GpeEntryBuilder()
                    .setKey(new GpeEntryKey(id))
                    .setId(id)
                    .setDpTable(vrf)
                    .setVni(vni)
                    .setRemoteEid(remoteEid)
                    .setLocalEid(localEid)
                    .setAction(action).build();
    }
}
