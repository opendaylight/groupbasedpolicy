/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.mapper.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.L3EndpointDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;

/**
 * SxpMapper provider implementation.
 */
public class SxpMapperProviderImpl implements AutoCloseable {

    private final DataBroker dataBrokerDependency;
    private final RpcProviderRegistry rpcRegistryDependency;
    private final SxpDatabaseListenerImpl sxpDatabaseListener;
    private final SxpMapperReactor sxpMapperReactor;
    private final EPTemplateListener epPolicyTemplateListener;
    private final EPTemplateListener epForwardingTemplateListener;

    public SxpMapperProviderImpl(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistryDependency) {
        this.dataBrokerDependency = dataBroker;
        this.rpcRegistryDependency = rpcRegistryDependency;

        final L3EndpointDao l3EndpointDao = new L3EndpointDaoImpl(dataBroker);
        sxpMapperReactor = new SxpMapperReactorImpl(l3EndpointDao);
        sxpDatabaseListener = new SxpDatabaseListenerImpl(dataBroker, sxpMapperReactor);
        epPolicyTemplateListener = new EPPolicyTemplateListenerImpl(dataBroker, sxpMapperReactor);
        epForwardingTemplateListener = new EPForwardingTemplateListenerImpl(dataBroker, sxpMapperReactor);

    }

    // register listeners to ip/sgt and EP-templates (by SGT, by subnet) -> 3x
    // exclusively write L3-EP to DS upon DataChangeEvent

    @Override
    public void close() throws Exception {
        //TODO: stub
        sxpDatabaseListener.close();
        epPolicyTemplateListener.close();
        epForwardingTemplateListener.close();
    }
}
