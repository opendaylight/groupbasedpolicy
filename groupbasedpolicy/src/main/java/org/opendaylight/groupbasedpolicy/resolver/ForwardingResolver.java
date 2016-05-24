/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.forwarding.ForwardingByTenant;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class ForwardingResolver extends DataTreeChangeHandler<ForwardingByTenant> {

    public ForwardingResolver(DataBroker dataProvider) {
        super(dataProvider);
    }

    @Override
    protected void onWrite(DataObjectModification<ForwardingByTenant> rootNode,
            InstanceIdentifier<ForwardingByTenant> rootIdentifier) {
        ForwardingByTenant forwardingByTenant = rootNode.getDataAfter();
        updateForwarding(rootIdentifier, forwardingByTenant);
    }

    @Override
    protected void onDelete(DataObjectModification<ForwardingByTenant> rootNode,
            InstanceIdentifier<ForwardingByTenant> rootIdentifier) {
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, rootIdentifier);
        wTx.submit();
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<ForwardingByTenant> rootNode,
            InstanceIdentifier<ForwardingByTenant> rootIdentifier) {
        ForwardingByTenant forwardingByTenant = rootNode.getDataAfter();
        updateForwarding(rootIdentifier, forwardingByTenant);
    }

    @VisibleForTesting
    void updateForwarding(InstanceIdentifier<ForwardingByTenant> rootIdentifier,
            ForwardingByTenant forwardingByTenant) {
        Preconditions.checkNotNull(rootIdentifier);
        Preconditions.checkNotNull(forwardingByTenant);
        // TODO add validation of forwarding
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, rootIdentifier, forwardingByTenant);
        wTx.submit();
    }

}
