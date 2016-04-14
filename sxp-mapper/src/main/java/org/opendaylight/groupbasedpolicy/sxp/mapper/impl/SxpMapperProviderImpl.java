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
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoAsync;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoCached;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.DSDaoCachedEPForwardingTemplateImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.DSDaoCachedImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EPForwardingTemplateDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EPPolicyTemplateDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.MasterDatabaseBindingDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen.EPForwardingTemplateListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen.EPPolicyTemplateListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen.MasterDatabaseBindingListenerImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;

/**
 * SxpMapper provider implementation.
 */
public class SxpMapperProviderImpl implements AutoCloseable {

    private final DataBroker dataBrokerDependency;
    private final RpcProviderRegistry rpcRegistryDependency;
    private final MasterDatabaseBindingListenerImpl sxpDatabaseListener;
    private final SxpMapperReactor sxpMapperReactor;
    private final EPTemplateListener epPolicyTemplateListener;
    private final EPTemplateListener epForwardingTemplateListener;

    public SxpMapperProviderImpl(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistryDependency) {
        this.dataBrokerDependency = dataBroker;
        this.rpcRegistryDependency = rpcRegistryDependency;

        final EndpointService endpointService = rpcRegistryDependency.getRpcService(EndpointService.class);
        sxpMapperReactor = new SxpMapperReactorImpl(endpointService);

        final DSDaoCached<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateCachedDao = new DSDaoCachedImpl<>();
        final DSDaoCached<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateCachedDao = new DSDaoCachedImpl<>();
                new DSDaoCachedEPForwardingTemplateImpl();
        final DSDaoCached<IpPrefix, MasterDatabaseBinding> masterDBBindingCachedDao = new DSDaoCachedImpl<>();

        final DSDaoAsync<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao = new EPPolicyTemplateDaoImpl(dataBroker, epPolicyTemplateCachedDao);
        final EPForwardingTemplateDaoImpl epForwardingTemplateDao = new EPForwardingTemplateDaoImpl(dataBroker,
                epForwardingTemplateCachedDao);
        final MasterDatabaseBindingDaoImpl masterDBBindingDao = new MasterDatabaseBindingDaoImpl(dataBroker, masterDBBindingCachedDao);

        sxpDatabaseListener = new MasterDatabaseBindingListenerImpl(dataBroker, sxpMapperReactor, masterDBBindingCachedDao,
                epPolicyTemplateDao, epForwardingTemplateDao);
        epPolicyTemplateListener = new EPPolicyTemplateListenerImpl(dataBroker, sxpMapperReactor, epPolicyTemplateCachedDao,
                masterDBBindingDao, epForwardingTemplateDao);
        epForwardingTemplateListener = new EPForwardingTemplateListenerImpl(dataBroker, sxpMapperReactor, epForwardingTemplateCachedDao,
                masterDBBindingDao, epPolicyTemplateDao);
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
