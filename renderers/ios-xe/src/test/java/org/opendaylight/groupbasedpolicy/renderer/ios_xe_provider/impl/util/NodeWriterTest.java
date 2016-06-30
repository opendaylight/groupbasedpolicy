/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import java.util.concurrent.ExecutionException;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NodeWriter;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for {@link NodeWriter}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DataStoreHelper.class})
public class NodeWriterTest {

    private DataBroker dataBroker;
    private NodeWriter nodeWriter;
    private RendererNode rendererNode;
    private WriteTransaction transaction;
    private CheckedFuture<Void, TransactionCommitFailedException> future;

    @Before
    @SuppressWarnings("unchecked")
    public void init() throws Exception {
        nodeWriter = new NodeWriter();
        dataBroker = mock(DataBroker.class);
        rendererNode = mock(RendererNode.class);
        transaction = mock(WriteTransaction.class);
        future = mock(CheckedFuture.class);
    }

    @Test
    public void commitToDs_emptyCache() {
        final ListenableFuture<Boolean> result = nodeWriter.commitToDatastore(dataBroker);
        assertTrue(getFutureResult(result));
    }

    @Test
    public void commitToDs_failure() {
        nodeWriter.cache(rendererNode);
        stub(method(DataStoreHelper.class, "submitToDs")).toReturn(false);
        final ListenableFuture<Boolean> result = nodeWriter.commitToDatastore(dataBroker);
        assertFalse(getFutureResult(result));
    }

    @Test
    public void commitToDs_success() {
        nodeWriter.cache(rendererNode);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        stub(method(DataStoreHelper.class, "submitToDs")).toReturn(true);
        final ListenableFuture<Boolean> result = nodeWriter.commitToDatastore(dataBroker);
        assertTrue(getFutureResult(result));
    }

    @Test
    public void removeFromDs_emptyCache() {
        nodeWriter.removeFromDatastore(dataBroker);
        final ListenableFuture<Boolean> result = nodeWriter.removeFromDatastore(dataBroker);
        assertTrue(getFutureResult(result));
    }

    @Test
    public void removeFromDs_transactionCommitException() throws Exception {
        nodeWriter.cache(rendererNode);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        when(transaction.submit()).thenReturn(future);
        when(future.checkedGet()).thenThrow(new TransactionCommitFailedException("exception"));
        final ListenableFuture<Boolean> result = nodeWriter.removeFromDatastore(dataBroker);
        assertFalse(getFutureResult(result));
    }

    @Test
    public void removeFromDs_otherException() throws Exception {
        nodeWriter.cache(rendererNode);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        when(transaction.submit()).thenReturn(future);
        when(future.checkedGet()).thenThrow(new NullPointerException("exception"));
        final ListenableFuture<Boolean> result = nodeWriter.removeFromDatastore(dataBroker);
        assertFalse(getFutureResult(result));
    }

    @Test
    public void removeFromDs_success() throws Exception {
        nodeWriter.cache(rendererNode);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        when(transaction.submit()).thenReturn(future);
        when(future.checkedGet()).thenReturn(null);
        final ListenableFuture<Boolean> result = nodeWriter.removeFromDatastore(dataBroker);
        assertTrue(getFutureResult(result));
    }

    private boolean getFutureResult(final ListenableFuture<Boolean> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Assert.fail();
            return false;
        }
    }
}