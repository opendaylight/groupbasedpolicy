/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.SxpEpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: process given sgt+name - create {@link EndpointPolicyTemplateBySgt} and write it to sxp-mapper templates
 */
public class SgtToEPTemplateGeneratorImpl implements SgtInfoProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SgtToEPTemplateGeneratorImpl.class);

    private final DataBroker dataBroker;

    public SgtToEPTemplateGeneratorImpl(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> processSgtInfo(final TenantId tenantId, final List<SgtInfo> sgtInfos) {
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();

        // write endpoint-policy-templates
        boolean createParent = true;
        for (SgtInfo sgtInfo : sgtInfos) {
            final Sgt sgt = sgtInfo.getSgt();
            final String sgtName = sgtInfo.getName();
            LOG.trace("processing sgtInfo: {} - {}", sgt.getValue(), sgtName);

            final EndpointGroupId epgId = new EndpointGroupId(sgtInfo.getName());

            final InstanceIdentifier<EndpointPolicyTemplateBySgt> epPolicyTemplatePath = InstanceIdentifier
                    .create(SxpEpMapper.class)
                    .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(sgt));

            final EndpointPolicyTemplateBySgt epPolicyTemplate = new EndpointPolicyTemplateBySgtBuilder()
                    .setSgt(sgt)
                    .setEndpointGroups(Collections.singletonList(epgId))
                    .setTenant(tenantId)
                    .build();
            wTx.put(LogicalDatastoreType.CONFIGURATION, epPolicyTemplatePath, epPolicyTemplate, createParent);
            createParent = false;
        }
        return wTx.submit();
    }
}
