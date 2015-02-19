/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class EprContextTest implements EprContext.EprCtxCallback {
    protected static final Logger logger = LoggerFactory.getLogger(L2EprOperationTest.class);

    @Mock
    private WriteTransaction mockWriter;
    @Mock
    private DataBroker mockDataProvider;
    @Mock
    private EprOperation mockOperation;
    @Mock
    private WriteTransaction mockWriteTransaction;
    @Mock
    private ReadOnlyTransaction mockReadTransaction;
    @Mock
    CheckedFuture<Void, TransactionCommitFailedException> mockFuture;

    private ScheduledExecutorService executor;
    private EprContext ec;
    private int callbacks;


	@Override
	public void callback(EprContext ctx) {
		this.callbacks += 1;
	}

	@Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        executor = Executors.newScheduledThreadPool(1);

        ec = new EprContext(null, null, mockDataProvider, executor);
    	ec.addOperation(mockOperation);
     }

    @Test
    public void testCreate() throws Exception {
    	when(mockDataProvider.newWriteOnlyTransaction()).thenReturn(mockWriteTransaction);
    	when(mockWriteTransaction.submit()).thenReturn(mockFuture);
    	ec.createEp();
    	verify(mockOperation).put(mockWriteTransaction);
    	verify(mockFuture).addListener(ec, executor);
    }

    @Test
    public void testDelete() throws Exception {
    	when(mockDataProvider.newWriteOnlyTransaction()).thenReturn(mockWriteTransaction);
    	when(mockWriteTransaction.submit()).thenReturn(mockFuture);
    	ec.deleteEp();
    	verify(mockOperation).delete(mockWriteTransaction);
    	verify(mockFuture).addListener(ec, executor);

    }

    @Test
    public void testLookup() throws Exception {
    	when(mockDataProvider.newReadOnlyTransaction()).thenReturn(mockReadTransaction);
    	ec.lookupEndpoint();
    	verify(mockOperation).setCallback(ec);
    	verify(mockOperation).read(mockReadTransaction, executor);

    }

    @Test
    public void testCallback() throws Exception {
		List<EprOperation> ops = new ArrayList<EprOperation>();

		EprOperation op1 = mock(EprOperation.class);
		EprOperation op2 = mock(EprOperation.class);
		EprOperation op3 = mock(EprOperation.class);
		EprOperation op4 = mock(EprOperation.class);

		ops.add(op1);
		ops.add(op2);
		ops.add(op3);
		ops.add(op4);

    	when(mockDataProvider.newReadOnlyTransaction()).thenReturn(mockReadTransaction);

    	this.callbacks = 0;
		ec.setCallback(this);

		for (EprOperation op: ops) {
			ec.addOperation(op);
		}
		assertTrue(this.callbacks == 0);

		ec.lookupEndpoint();

		for (EprOperation op: ops) {
			ec.callback(op);
		}
		assertTrue(this.callbacks == 0);
		ec.callback(mockOperation);
		assertTrue(this.callbacks == 1);

    }

}
