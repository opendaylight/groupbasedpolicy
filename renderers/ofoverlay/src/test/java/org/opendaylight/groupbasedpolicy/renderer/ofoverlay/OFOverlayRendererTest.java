/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.api.PolicyValidatorRegistry;
import org.opendaylight.groupbasedpolicy.endpoint.EndpointRpcRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

public class OFOverlayRendererTest {

    private OFOverlayRenderer renderer;

    private DataBroker dataProvider;
    private RpcProviderRegistry rpcRegistry;
    private EndpointRpcRegistry endpointRpcRegistry;
    private NotificationService notificationService;
    private PolicyValidatorRegistry policyValidatorRegistry;
    private short tableOffset;
    private CheckedFuture<Optional<OfOverlayConfig>, ReadFailedException> future;
    private ListenerRegistration<DataChangeListener> configReg;
    private ReadOnlyTransaction readTransaction;

    @SuppressWarnings("unchecked")
    @Before
    public void initialisation() {
        dataProvider = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        endpointRpcRegistry = mock(EndpointRpcRegistry.class);
        notificationService = mock(NotificationService.class);
        policyValidatorRegistry = mock(PolicyValidatorRegistry.class);
        tableOffset = 5;
        configReg = mock(ListenerRegistration.class);
        when(dataProvider.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataChangeListener.class), any(DataChangeScope.class))).thenReturn(configReg);
        when(dataProvider.registerDataTreeChangeListener(any(DataTreeIdentifier.class),
                any(DataTreeChangeListener.class))).thenReturn(configReg);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);

        readTransaction = mock(ReadOnlyTransaction.class);
        when(dataProvider.newReadOnlyTransaction()).thenReturn(readTransaction);
        future = Futures.immediateCheckedFuture(Optional.<OfOverlayConfig> absent());
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);
        renderer = new OFOverlayRenderer(dataProvider, rpcRegistry, notificationService, endpointRpcRegistry,
                policyValidatorRegistry, tableOffset);
    }

    @Test
    public void constructorTest() throws Exception {
        renderer.close();
        verify(configReg, times(10)).close();
    }

    @Test
    public void applyConfigTest() throws Exception {
        OfOverlayConfig config = mock(OfOverlayConfig.class);
        when(config.getGbpOfoverlayTableOffset()).thenReturn(null);
        Method method = OFOverlayRenderer.class.getDeclaredMethod("applyConfig",OfOverlayConfig.class);
        method.setAccessible(true);
        method.invoke(renderer, config);

        verify(config).getEncapsulationFormat();
        verify(config, times(2)).getLearningMode();
    }
}
