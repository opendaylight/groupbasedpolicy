/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class EndpointHelperTest {

    EndpointKey epKey;
    Endpoint endpoint;
    ReadOnlyTransaction readTransaction;
    ReadWriteTransaction writeTransaction;
    Optional<Endpoint> readOptional;
    CheckedFuture<Void, TransactionCommitFailedException> submitFuture;

    @SuppressWarnings("unchecked")
    @Before
    public void initialise() throws Exception {
        epKey = mock(EndpointKey.class);
        endpoint = mock(Endpoint.class);
        readTransaction = mock(ReadOnlyTransaction.class);
        writeTransaction = mock(ReadWriteTransaction.class);

        CheckedFuture<Optional<Endpoint>, ReadFailedException> readFuture = mock(CheckedFuture.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(
                readFuture);
        readOptional = mock(Optional.class);
        when(readFuture.checkedGet()).thenReturn(readOptional);

        OfOverlayContext ofc = mock(OfOverlayContext.class);
        when(endpoint.getAugmentation(OfOverlayContext.class)).thenReturn(ofc);

        submitFuture = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(submitFuture);
    }

    @Test
    public void lookupEndpointTest() {
        when(readOptional.isPresent()).thenReturn(true);
        when(readOptional.get()).thenReturn(endpoint);
        Endpoint result = EndpointHelper.lookupEndpoint(epKey, readTransaction);
        Assert.assertEquals(result, endpoint);
    }

    @Test
    public void lookupEndpointTestNull() {
        when(readOptional.isPresent()).thenReturn(false);
        Endpoint result = EndpointHelper.lookupEndpoint(epKey, readTransaction);
        Assert.assertNull(result);
    }

    @Test
    public void updateEndpointWithLocationTest() throws Exception {
        String nodeIdString = "nodeIdString";
        String nodeConnectorIdString = "nodeConnectorIdString";
        EndpointHelper.updateEndpointWithLocation(endpoint, nodeIdString, nodeConnectorIdString, writeTransaction);
        verify(submitFuture).checkedGet();
    }

    @Test
    public void updateEndpointRemoveLocationTest() throws Exception {
        EndpointHelper.updateEndpointRemoveLocation(endpoint, writeTransaction);
        verify(submitFuture).checkedGet();
    }

}
