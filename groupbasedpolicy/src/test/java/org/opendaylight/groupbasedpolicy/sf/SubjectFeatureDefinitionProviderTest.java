/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sf;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings({"unchecked"})
@RunWith(MockitoJUnitRunner.class)
public class SubjectFeatureDefinitionProviderTest {

    public static final int NUM_OF_SUBJECT_FEATURE_DEFINITIONS = 5;

    @Mock
    DataBroker dataProvider;
    @Mock
    WriteTransaction writeTransaction;
    @Mock
    CheckedFuture<Void, TransactionCommitFailedException> checkedFuture;

    @Before
    public void init() {
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(writeTransaction);
    }

    @Test
    public void testConstructor() throws TransactionCommitFailedException {
        doNothing().when(writeTransaction).put(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class),
                any(SubjectFeatureDefinitions.class));

        SubjectFeatureDefinitionProvider provider = new SubjectFeatureDefinitionProvider(dataProvider);

        assertNotNull(provider);
        verify(dataProvider).newWriteOnlyTransaction();
        verify(writeTransaction, times(NUM_OF_SUBJECT_FEATURE_DEFINITIONS)).put(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class), any(SubjectFeatureDefinitions.class));

    }

    @Test
    public void testClose() throws Exception {
        doNothing().when(writeTransaction).delete(eq(LogicalDatastoreType.CONFIGURATION),
                any(InstanceIdentifier.class));

        SubjectFeatureDefinitionProvider provider = new SubjectFeatureDefinitionProvider(dataProvider);

        assertNotNull(provider);

        provider.close();

        verify(dataProvider, times(2)).newWriteOnlyTransaction();
        verify(writeTransaction, times(NUM_OF_SUBJECT_FEATURE_DEFINITIONS))
            .delete(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class));

    }

}
