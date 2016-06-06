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
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EPForwardingTemplateDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EPPolicyTemplateDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.EpPolicyTemplateValueKeyFactory;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.MasterDatabaseBindingDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.SimpleCachedDaoEPForwardingTemplateImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao.SimpleCachedDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen.EPForwardingTemplateListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen.EPPolicyTemplateListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen.MasterDatabaseBindingListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.EPTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SxpMapper provider implementation.
 */
public class SxpMapperProviderImpl implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SxpMapperProviderImpl.class);

    private final MasterDatabaseBindingListenerImpl sxpDatabaseListener;
    private final SxpMapperReactor sxpMapperReactor;
    private final EPTemplateListener epPolicyTemplateListener;
    private final EPTemplateListener epForwardingTemplateListener;
    private final DomainSpecificRegistry domainSpecificRegistry;
    private final EndpointAugmentor sxpEndpointAugmentor;

    public SxpMapperProviderImpl(final DataBroker dataBroker, final RpcProviderRegistry rpcRegistryDependency,
            final DomainSpecificRegistry domainSpecificRegistry) {
        LOG.info("starting SxmMapper ..");
        this.domainSpecificRegistry = domainSpecificRegistry;

        final BaseEndpointService endpointService = rpcRegistryDependency.getRpcService(BaseEndpointService.class);
        sxpMapperReactor = new SxpMapperReactorImpl(endpointService, dataBroker);

        final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateCachedDao = new SimpleCachedDaoImpl<>();
        final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateCachedDao =
                new SimpleCachedDaoEPForwardingTemplateImpl();
        final SimpleCachedDao<IpPrefix, MasterDatabaseBinding> masterDBBindingCachedDao = new SimpleCachedDaoImpl<>();

        final EpPolicyTemplateValueKeyFactory epPolicyTemplateKeyFactory = new EpPolicyTemplateValueKeyFactory(
                EPTemplateUtil.createEndpointGroupIdOrdering(), EPTemplateUtil.createConditionNameOrdering());
        final EPPolicyTemplateDaoImpl epPolicyTemplateDao = new EPPolicyTemplateDaoImpl(dataBroker, epPolicyTemplateCachedDao, epPolicyTemplateKeyFactory);
        final EPForwardingTemplateDaoImpl epForwardingTemplateDao = new EPForwardingTemplateDaoImpl(dataBroker,
                epForwardingTemplateCachedDao);
        final  MasterDatabaseBindingDaoImpl masterDBBindingDao = new MasterDatabaseBindingDaoImpl(dataBroker, masterDBBindingCachedDao);

        sxpDatabaseListener = new MasterDatabaseBindingListenerImpl(dataBroker, sxpMapperReactor, masterDBBindingCachedDao,
                epPolicyTemplateDao, epForwardingTemplateDao);
        epPolicyTemplateListener = new EPPolicyTemplateListenerImpl(dataBroker, sxpMapperReactor, epPolicyTemplateCachedDao,
                masterDBBindingDao, epForwardingTemplateDao);
        epForwardingTemplateListener = new EPForwardingTemplateListenerImpl(dataBroker, sxpMapperReactor, epForwardingTemplateCachedDao,
                masterDBBindingDao, epPolicyTemplateDao);
        sxpEndpointAugmentor = new SxpEndpointAugmentorImpl(epPolicyTemplateDao, epPolicyTemplateKeyFactory);
        domainSpecificRegistry.getEndpointAugmentorRegistry().register(sxpEndpointAugmentor);
        LOG.info("started SxmMapper");
    }

    @Override
    public void close() throws Exception {
        //TODO: stub
        sxpDatabaseListener.close();
        epPolicyTemplateListener.close();
        epForwardingTemplateListener.close();
        domainSpecificRegistry.getEndpointAugmentorRegistry().unregister(sxpEndpointAugmentor);
    }
}
