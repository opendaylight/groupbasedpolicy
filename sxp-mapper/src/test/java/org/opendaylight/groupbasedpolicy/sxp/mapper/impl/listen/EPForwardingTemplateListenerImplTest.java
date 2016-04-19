/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBindingBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Test for {@link EPForwardingTemplateListenerImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EPForwardingTemplateListenerImplTest {

    private static final IpPrefix IP_PREFIX_TMPL = buildIpPrefix("1.2.3.0/24");
    private static final EndpointForwardingTemplateBySubnetKey EP_FW_TEMPLATE_KEY =
            new EndpointForwardingTemplateBySubnetKey(IP_PREFIX_TMPL);
    private static final KeyedInstanceIdentifier<EndpointForwardingTemplateBySubnet, EndpointForwardingTemplateBySubnetKey> EP_FW_TEMPLATE_PATH =
            EPTemplateListener.SXP_MAPPER_TEMPLATE_PARENT_PATH
                    .child(EndpointForwardingTemplateBySubnet.class, EP_FW_TEMPLATE_KEY);
    private static final DataTreeIdentifier<EndpointForwardingTemplateBySubnet> TEMPLATE_TREE_PATH =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, EP_FW_TEMPLATE_PATH);
    private final EndpointForwardingTemplateBySubnet EP_FW_TEMPLATE_VALUE;


    @Mock
    private DataBroker dataBroker;
    @Mock
    private SxpMapperReactor sxpMapper;
    @Mock
    private SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> simpleCachedDao;
    @Mock
    private DSAsyncDao<IpPrefix, MasterDatabaseBinding> masterDBBindingDao;
    @Mock
    private ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    @Mock
    private DataTreeModification<EndpointForwardingTemplateBySubnet> dataTreeModification;
    @Mock
    private DataObjectModification<EndpointForwardingTemplateBySubnet> dataObjectModification;

    private EPForwardingTemplateListenerImpl listener;

    public EPForwardingTemplateListenerImplTest() {
        EP_FW_TEMPLATE_VALUE = new EndpointForwardingTemplateBySubnetBuilder()
                .setIpPrefix(IP_PREFIX_TMPL)
                .build();
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(dataBroker.registerDataTreeChangeListener(Matchers.<DataTreeIdentifier>any(),
                Matchers.<DataTreeChangeListener>any())).thenReturn(listenerRegistration);
        listener = new EPForwardingTemplateListenerImpl(dataBroker, sxpMapper, simpleCachedDao, masterDBBindingDao);
    }

    @Test
    public void testOnDataTreeChanged() throws Exception {
        Mockito.when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        Mockito.when(dataTreeModification.getRootPath()).thenReturn(TEMPLATE_TREE_PATH);
        Mockito.when(dataObjectModification.getDataAfter()).thenReturn(EP_FW_TEMPLATE_VALUE);

        final Sgt sgt = new Sgt(1);
        final IpPrefix ipPrefix = buildIpPrefix("1.2.3.4/32");
        final MasterDatabaseBinding prefixGroup = new MasterDatabaseBindingBuilder()
                .setSecurityGroupTag(sgt)
                .setIpPrefix(ipPrefix)
                .build();
        Mockito.when(masterDBBindingDao.read(Matchers.<IpPrefix>any())).thenReturn(
                Futures.immediateFuture(Optional.of(prefixGroup)));

        listener.onDataTreeChanged(Collections.singleton(dataTreeModification));

        final InOrder inOrder = Mockito.inOrder(masterDBBindingDao, simpleCachedDao, sxpMapper);
        inOrder.verify(simpleCachedDao).update(IP_PREFIX_TMPL, EP_FW_TEMPLATE_VALUE);
        inOrder.verify(masterDBBindingDao).read(IP_PREFIX_TMPL);
        inOrder.verify(sxpMapper).processForwardingAndSxpMasterDB(EP_FW_TEMPLATE_VALUE, prefixGroup);
        inOrder.verifyNoMoreInteractions();
    }

    private static IpPrefix buildIpPrefix(final String ipv4PrefixValue) {
        return new IpPrefix(new Ipv4Prefix(ipv4PrefixValue));
    }

    @Test
    public void testClose() throws Exception {
        Mockito.verify(listenerRegistration, Mockito.never()).close();
        listener.close();
        Mockito.verify(listenerRegistration).close();
    }
}