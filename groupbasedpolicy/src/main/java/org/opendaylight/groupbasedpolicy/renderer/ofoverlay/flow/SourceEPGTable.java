/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;


/**
 * Manage the table that assigns source endpoint group, bridge domain, and 
 * router domain to registers to be used by other tables.
 * @author readams
 */
public class SourceEPGTable extends FlowTable {
    public static final short TABLE_ID = 1;

    public SourceEPGTable(FlowTableCtx ctx) {
        super(ctx);
    }

    @Override
    public void update(NodeId nodeId, Dirty dirty) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Table getEmptyTable() {
        return new TableBuilder()
            .setId(Short.valueOf((short)TABLE_ID))
            .build();
    }

}
