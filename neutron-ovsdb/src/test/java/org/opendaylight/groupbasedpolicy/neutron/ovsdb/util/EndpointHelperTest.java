/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointHelperTest {

    private EndpointKey epKey;
    private Endpoint endpoint;
    private ReadOnlyTransaction readTransaction;
    private ReadWriteTransaction writeTransaction;
    private Optional<Endpoint> readOptional;
    private CheckedFuture<Void, TransactionCommitFailedException> submitFuture;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        epKey = mock(EndpointKey.class);
        OfOverlayContext ofc = mock(OfOverlayContext.class);
        endpoint = new EndpointBuilder().setL2Context(new L2BridgeDomainId("foo"))
            .setMacAddress(new MacAddress("01:23:45:67:89:AB"))
            .setTenant(new TenantId("fooTenant"))
            .addAugmentation(OfOverlayContext.class, ofc)
            .build();
        readTransaction = mock(ReadOnlyTransaction.class);
        writeTransaction = mock(ReadWriteTransaction.class);

        CheckedFuture<Optional<Endpoint>, ReadFailedException> readFuture = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(readFuture);
        readOptional = mock(Optional.class);
        when(readFuture.checkedGet()).thenReturn(readOptional);

        submitFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(submitFuture);
    }

    @Test
    public void testLookupEndpoint() {
        when(readOptional.isPresent()).thenReturn(true);
        when(readOptional.get()).thenReturn(endpoint);
        Endpoint result = EndpointHelper.lookupEndpoint(epKey, readTransaction);
        assertEquals(result, endpoint);
    }

    @Test
    public void testLookupEndpoint_NotPresent() {
        when(readOptional.isPresent()).thenReturn(false);
        Endpoint result = EndpointHelper.lookupEndpoint(epKey, readTransaction);
        assertNull(result);
    }

    @Test
    public void testUpdateEndpointWithLocation() throws Exception {
        String nodeIdString = "nodeIdString";
        String nodeConnectorIdString = "nodeConnectorIdString";
        EndpointHelper.updateEndpointWithLocation(endpoint, nodeIdString, nodeConnectorIdString, writeTransaction);
        verify(submitFuture).checkedGet();
    }

    @Test
    public void testUpdateEndpointRemoveLocation() throws Exception {
        EndpointHelper.updateEndpointRemoveLocation(endpoint, writeTransaction);
        verify(submitFuture).checkedGet();
    }

}
