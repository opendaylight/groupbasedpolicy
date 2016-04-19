/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Purpose: exclusively processes sxp master database changes and EGP templates changes
 */
public class SxpMapperReactorImpl implements SxpMapperReactor {
    private final EndpointService l3EndpointService;

    public SxpMapperReactorImpl(final EndpointService l3EndpointService) {
        this.l3EndpointService = l3EndpointService;
    }

    @Override
    public ListenableFuture<RpcResult<Void>> processPolicyAndSxpMasterDB(final EndpointPolicyTemplateBySgt template, final MasterDatabaseBinding masterDatabaseBinding) {
        // apply sxpMasterDB to policy template
        final RegisterEndpointInput input = new RegisterEndpointInputBuilder()
                .setCondition(template.getConditions())
                .setTenant(template.getTenant())
                .setEndpointGroups(template.getEndpointGroups())
                .build();

        // invoke service
        return JdkFutureAdapters.listenInPoolThread(l3EndpointService.registerEndpoint(input));
    }

    @Override
    public ListenableFuture<RpcResult<Void>> processForwardingAndSxpMasterDB(final EndpointForwardingTemplateBySubnet template, final MasterDatabaseBinding masterDatabaseBinding) {
        // apply sxpMasterDB to policy template
        final RegisterEndpointInput input = new RegisterEndpointInputBuilder()
                .setNetworkContainment(template.getNetworkContainment())
                .build();

        // invoke service
        return JdkFutureAdapters.listenInPoolThread(l3EndpointService.registerEndpoint(input));
    }
}
