/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import com.google.common.collect.Ordering;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.DomainSpecificRegistry;
import org.opendaylight.groupbasedpolicy.api.EndpointAugmentorRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EPForwardingTemplateDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EPPolicyTemplateDaoFacadeImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EPPolicyTemplateDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.EpPolicyTemplateValueKeyFactory;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.MasterDatabaseBindingDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.SimpleCachedDaoEPForwardingTemplateImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.dao.SimpleCachedDaoImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.listen.EPForwardingTemplateListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.listen.EPPolicyTemplateListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.listen.MasterDatabaseBindingListenerImpl;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.EPTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.BaseEndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.SgtGeneratorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for {@link SxpEpProviderProviderImpl}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SxpEpProviderProviderImpl.class, EPTemplateUtil.class})
public class SxpEpProviderProviderImplTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private DomainSpecificRegistry domainSpecificRegistry;
    @Mock
    private EPPolicyTemplateProviderRegistryImpl templateProviderRegistry;
    @Mock
    private SxpMapperReactorImpl sxpMapperReactor;
    @Mock
    private BaseEndpointService endpointService;
    @Mock
    private SimpleCachedDaoImpl<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateCachedDao;
    @Mock
    private SimpleCachedDaoImpl<IpPrefix, MasterDatabaseBinding> masterDBBindingCachedDao;
    @Mock
    private SimpleCachedDaoEPForwardingTemplateImpl epFwTemplateCachedDao;
    @Mock
    private Ordering<EndpointGroupId> groupOrdering;
    @Mock
    private Ordering<ConditionName> conditionOrdering;
    @Mock
    private EpPolicyTemplateValueKeyFactory epPolicyTemplateKeyFactory;
    @Mock
    private EPPolicyTemplateDaoImpl epPolicyTemplateDao;
    @Mock
    private EPForwardingTemplateDaoImpl epForwardingTemplateDao;
    @Mock
    private MasterDatabaseBindingDaoImpl masterDBBindingDao;
    @Mock
    private MasterDatabaseBindingListenerImpl masterDBBindingListener;
    @Mock
    private EPPolicyTemplateListenerImpl epPolicyTemplateListener;
    @Mock
    private EPForwardingTemplateListenerImpl epForwardingTemplateListener;
    @Mock
    private SxpEndpointAugmentorImpl sxpEPAugmentor;
    @Mock
    private EndpointAugmentorRegistry epAugmentorRegistry;
    @Mock
    private EPPolicyTemplateDaoFacadeImpl epPolicyTemplateDaoFacade;
    @Mock
    private SgtGeneratorConfig sgtGeneratorConfig;
    @Mock
    private SgtGeneratorImpl sgtGenerator;
    @Mock
    private EPToSgtMapperImpl epToSgtMapper;

    private SxpEpProviderProviderImpl provider;

    @Before
    public void setUp() throws Exception {
        whenNew(EPPolicyTemplateProviderRegistryImpl.class).withNoArguments().thenReturn(templateProviderRegistry);
        whenNew(SxpMapperReactorImpl.class).withArguments(endpointService, dataBroker).thenReturn(sxpMapperReactor);
        whenNew(SimpleCachedDaoImpl.class).withNoArguments().thenReturn(epPolicyTemplateCachedDao, masterDBBindingCachedDao);
        whenNew(SimpleCachedDaoEPForwardingTemplateImpl.class).withNoArguments().thenReturn(epFwTemplateCachedDao);
        stub(method(EPTemplateUtil.class, "createEndpointGroupIdOrdering")).toReturn(groupOrdering);
        stub(method(EPTemplateUtil.class, "createConditionNameOrdering")).toReturn(conditionOrdering);
        whenNew(EpPolicyTemplateValueKeyFactory.class).withArguments(groupOrdering, conditionOrdering)
                .thenReturn(epPolicyTemplateKeyFactory);
        whenNew(EPPolicyTemplateDaoImpl.class).withArguments(dataBroker, epPolicyTemplateCachedDao, epPolicyTemplateKeyFactory)
                .thenReturn(epPolicyTemplateDao);
        whenNew(SgtGeneratorImpl.class).withArguments(sgtGeneratorConfig).thenReturn(sgtGenerator);
        whenNew(EPPolicyTemplateDaoFacadeImpl.class).withArguments(dataBroker, epPolicyTemplateDao, epPolicyTemplateCachedDao, sgtGenerator)
                .thenReturn(epPolicyTemplateDaoFacade);
        whenNew(EPToSgtMapperImpl.class).withArguments(epPolicyTemplateDaoFacade).thenReturn(epToSgtMapper);

        whenNew(EPForwardingTemplateDaoImpl.class).withArguments(dataBroker, epFwTemplateCachedDao)
                .thenReturn(epForwardingTemplateDao);
        whenNew(MasterDatabaseBindingDaoImpl.class).withArguments(dataBroker, masterDBBindingCachedDao)
                .thenReturn(masterDBBindingDao);
        whenNew(MasterDatabaseBindingListenerImpl.class).withArguments(dataBroker, sxpMapperReactor, masterDBBindingCachedDao,
                epPolicyTemplateDaoFacade, epForwardingTemplateDao).thenReturn(masterDBBindingListener);
        whenNew(EPPolicyTemplateListenerImpl.class).withArguments(dataBroker, sxpMapperReactor, epPolicyTemplateCachedDao,
                masterDBBindingDao, epForwardingTemplateDao).thenReturn(epPolicyTemplateListener);
        whenNew(EPForwardingTemplateListenerImpl.class).withArguments(dataBroker, sxpMapperReactor, epFwTemplateCachedDao,
                masterDBBindingDao, epPolicyTemplateDaoFacade).thenReturn(epForwardingTemplateListener);
        whenNew(SxpEndpointAugmentorImpl.class).withArguments(epPolicyTemplateDao, epPolicyTemplateKeyFactory)
                .thenReturn(sxpEPAugmentor);


        provider = new SxpEpProviderProviderImpl(dataBroker, endpointService, domainSpecificRegistry, sgtGeneratorConfig);

        Mockito.verify(templateProviderRegistry).addDistributionTarget(epPolicyTemplateDaoFacade);

        // check if all expected object got constructed and wired
        verifyNew(EPPolicyTemplateProviderRegistryImpl.class).withNoArguments();
        verifyNew(SxpMapperReactorImpl.class).withArguments(endpointService, dataBroker);
        verifyNew(SimpleCachedDaoImpl.class, Mockito.times(2)).withNoArguments();
        verifyNew(SimpleCachedDaoEPForwardingTemplateImpl.class).withNoArguments();
        verifyNew(EpPolicyTemplateValueKeyFactory.class).withArguments(groupOrdering, conditionOrdering);
        verifyNew(EPPolicyTemplateDaoImpl.class).withArguments(dataBroker, epPolicyTemplateCachedDao, epPolicyTemplateKeyFactory);
        verifyNew(SgtGeneratorImpl.class).withArguments(sgtGeneratorConfig);
        verifyNew(EPPolicyTemplateDaoFacadeImpl.class).withArguments(dataBroker, epPolicyTemplateDao, epPolicyTemplateCachedDao, sgtGenerator);
        verifyNew(EPToSgtMapperImpl.class).withArguments(epPolicyTemplateDaoFacade);
        verifyNew(EPForwardingTemplateDaoImpl.class).withArguments(dataBroker, epFwTemplateCachedDao);
        verifyNew(MasterDatabaseBindingDaoImpl.class).withArguments(dataBroker, masterDBBindingCachedDao);
        verifyNew(MasterDatabaseBindingListenerImpl.class).withArguments(dataBroker, sxpMapperReactor, masterDBBindingCachedDao,
                epPolicyTemplateDaoFacade, epForwardingTemplateDao);
        verifyNew(EPPolicyTemplateListenerImpl.class).withArguments(dataBroker, sxpMapperReactor, epPolicyTemplateCachedDao,
                masterDBBindingDao, epForwardingTemplateDao);
        verifyNew(EPForwardingTemplateListenerImpl.class).withArguments(dataBroker, sxpMapperReactor, epFwTemplateCachedDao,
                masterDBBindingDao, epPolicyTemplateDaoFacade);
    }

    @Test
    public void testGetEPPolicyTemplateProviderRegistry() throws Exception {
        Assert.assertSame(templateProviderRegistry, provider.getEPPolicyTemplateProviderRegistry());
    }

    @Test
    public void testClose() throws Exception {
        provider.close();
        Mockito.verify(masterDBBindingListener).close();
        Mockito.verify(epPolicyTemplateListener).close();
        Mockito.verify(epForwardingTemplateListener).close();
        Mockito.verify(templateProviderRegistry).close();
    }
}
