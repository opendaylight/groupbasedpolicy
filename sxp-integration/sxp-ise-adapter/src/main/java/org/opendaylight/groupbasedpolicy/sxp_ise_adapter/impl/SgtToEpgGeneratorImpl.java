/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: process given sgt+name - create {@link EndpointGroup} and write it to tenants/tenant/policy/endpoint-group
 */
public class SgtToEpgGeneratorImpl implements SgtInfoProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SgtToEpgGeneratorImpl.class);

    private final DataBroker dataBroker;

    public SgtToEpgGeneratorImpl(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> processSgtInfo(final TenantId tenantId, final List<SgtInfo> sgtInfos) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();

        // create and write endpointgroups
        boolean createParent = true;
        for (SgtInfo sgtInfo : sgtInfos) {
            final Integer sgtValue = sgtInfo.getSgt().getValue();
            final String sgtName = sgtInfo.getName();
            LOG.trace("processing sgtInfo: {} - {}", sgtValue, sgtName);

            final EndpointGroupId epgId = new EndpointGroupId(sgtName);
            final InstanceIdentifier<EndpointGroup> epgPath = IidFactory.endpointGroupIid(tenantId, epgId);
            final EndpointGroup epg = new EndpointGroupBuilder()
                    .setId(epgId)
                    .setDescription(new Description("imported from ISE for sgt=" + sgtValue))
                    .setName(new Name(sgtName.replaceAll(" ", "_") + "--" + sgtValue))
                    .build();
            wTx.put(LogicalDatastoreType.CONFIGURATION, epgPath, epg, createParent);
            createParent = false;
        }
        return wTx.submit();
    }
}
