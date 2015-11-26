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
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.gbp.by.neutron.mappings.endpoints.by.ports.EndpointByPort;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class NeutronHelperTest {

    private UniqueId externalId;
    private DataBroker dataBroker;
    private Optional<EndpointByPort> optionalEp;

    @SuppressWarnings("unchecked")
    @Before
    public void initialise() throws Exception {
        externalId = mock(UniqueId.class);
        dataBroker = mock(DataBroker.class);

        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<EndpointByPort>, ReadFailedException> resultFuture = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(resultFuture);
        optionalEp = mock(Optional.class);
        when(resultFuture.checkedGet()).thenReturn(optionalEp);
    }

    @Test
    public void getEpKeyFromNeutronMapperTest() throws Exception {
        when(optionalEp.isPresent()).thenReturn(true);
        EndpointByPort epByPort = mock(EndpointByPort.class);
        when(optionalEp.get()).thenReturn(epByPort);
        when(epByPort.getL2Context()).thenReturn(mock(L2BridgeDomainId.class));
        when(epByPort.getMacAddress()).thenReturn(mock(MacAddress.class));

        Assert.assertNotNull(NeutronHelper.getEpKeyFromNeutronMapper(externalId, dataBroker));
    }

    @Test
    public void getEpKeyFromNeutronMapperTestPresentFalse() throws Exception {
        when(optionalEp.isPresent()).thenReturn(false);

        Assert.assertNull(NeutronHelper.getEpKeyFromNeutronMapper(externalId, dataBroker));
    }
}
