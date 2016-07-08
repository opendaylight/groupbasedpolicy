/*
 * Copyright (c) 2016 Huawei Technologies and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.faas.endpoint.rev151009.FaasEndpointContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.logical.faas.common.rev151013.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;

public class FaasEndpointManagerListenerTest {

    private InstanceIdentifier<DataObject> endpointId;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change;
    private MockFaasEndpointManagerListener endpointManagerListener;
    private MockFaasPolicyManager policyManager;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime()
        .availableProcessors());

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        endpointId = mock(InstanceIdentifier.class);
        change = mock(AsyncDataChangeEvent.class);
        endpointId = mock(InstanceIdentifier.class);
        DataBroker dataProvider = mock(DataBroker.class);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        policyManager = new MockFaasPolicyManager(dataProvider, executor);
        endpointManagerListener = new MockFaasEndpointManagerListener(policyManager, dataProvider, executor);
        Set<InstanceIdentifier<?>> removedPaths = new HashSet<>();
        removedPaths.add(endpointId);
        when(change.getRemovedPaths()).thenReturn(removedPaths);
    }

    @Test
    public void testOnDataChangeEndpoint() {
        // prepare input test data
        DataObject testEndpoint = makeTestEndpoint();
        endpointManagerListener.setExpectedEndpoint((Endpoint) testEndpoint);
        Map<InstanceIdentifier<?>, DataObject> testData = new HashMap<>();
        testData.put(endpointId, testEndpoint);
        when(change.getCreatedData()).thenReturn(testData);
        when(change.getOriginalData()).thenReturn(testData);
        when(change.getUpdatedData()).thenReturn(testData);
        // invoke event -- expected data is verified in mocked classes
        endpointManagerListener.onDataChanged(change);
    }

    private DataObject makeTestEndpoint() {
        EndpointBuilder builder = new EndpointBuilder();
        builder.setL2Context(new L2BridgeDomainId("L2Context"));
        List<L3Address> l3Addrs = new ArrayList<L3Address>();
        L3AddressBuilder addb = new L3AddressBuilder();
        addb.setIpAddress(new IpAddress(new String("10.0.0.2").toCharArray()));
        addb.setL3Context(new L3ContextId("L3Context"));
        l3Addrs.add(addb.build());
        builder.setL3Address(l3Addrs);
        builder.setMacAddress(new MacAddress("00:00:00:00:35:02"));
        builder.setTenant(new TenantId("tenant"));
        builder.setEndpointGroup(new EndpointGroupId("endpointGroupId"));
        builder.setNetworkContainment(new NetworkDomainId("subnet"));
        FaasEndpointContextBuilder faasAugb = new FaasEndpointContextBuilder();
        faasAugb.setFaasPortRefId(new Uuid("165b3a20-adc7-11e5-bf7f-feff819cdc9f"));
        builder.addAugmentation(FaasEndpointContext.class, faasAugb.build());
        return builder.build();
    }
}
