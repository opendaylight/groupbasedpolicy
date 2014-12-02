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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class L3EprOperationTest implements EprOperation.EprOpCallback {
    protected static final Logger logger = LoggerFactory.getLogger(L3EprOperationTest.class);

    private static final int TEST_SIZE = 1;
    L3EprOperation op = null;
    private int callbacks;
    @Mock
    private WriteTransaction mockWriter;
    @Mock
    private ReadOnlyTransaction mockReader;
    @Mock
    private Identity mockId;
    @Mock
    private L3ContextId mockL3Context;
    @Mock
    private IpAddress mockIp;
    @Mock
    private List<L3Address> mockAddresses;
    @Mock
    private CheckedFuture<Optional<EndpointL3>,ReadFailedException> mockFuture;
    @Mock
    private Optional<EndpointL3> mockOption;
    @Mock
    private EndpointL3 mockEp;
    @Mock
    private List<String> mockIdentityList;
    private ScheduledExecutorService executor;

    private static final String TEST_TENANT_ID = "e9fbd015-df23-4749-abec-8ba63bc0e738";
    private static final String TEST_EPG_ID = "8e359239-a253-42c3-9858-9acc039f1913";
    private static final String TEST_CONTEXT = "4ac7e31a-5775-46ac-b228-f40cdfeeabe7";
    private static final String TEST_IP1 = "192.168.194.131";
    private static final String TEST_IP2 = "192.168.194.132";
    private static final String TEST_IP3 = "192.168.194.133";
    private static final String TEST_MAC = "de:ad:be:ef:00:ba";
    private static final int TEST_PRR = 100;

    private static Ipv4Address TEST_IPV4_1, TEST_IPV4_2, TEST_IPV4_3;
    private static TenantId tid;
    private static EndpointGroupId egid;
    private static L3ContextId l3cid;
    private static MacAddress mac;
    private static List<L3Address> l3List;
    private static IpAddress ip;
    private static Long prr;

	@Override
	public void callback(EprOperation op) {
		this.callbacks += 1;

	}

	@Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        TEST_IPV4_1 = new Ipv4Address(TEST_IP1);
        TEST_IPV4_2 = new Ipv4Address(TEST_IP2);
        TEST_IPV4_3 = new Ipv4Address(TEST_IP3);
        op =  new L3EprOperation(TEST_PRR);
        op.setCallback(this);

        tid = new TenantId(TEST_TENANT_ID);
        egid = new EndpointGroupId(TEST_EPG_ID);
        l3cid = new L3ContextId(TEST_CONTEXT);
        mac = new MacAddress(TEST_MAC);
        ip = new IpAddress(TEST_IPV4_1);
        prr = new Long(TEST_PRR);

        op.setTenantId(tid);
        op.setEndpointGroupId(egid);
        op.setIpAddress(new IpAddress(TEST_IPV4_1));
        op.setContextId(l3cid);
        op.setMacAddress(mac);

        // Add 3 contexts
        L3AddressBuilder l3ab = new L3AddressBuilder();
        l3ab.setL3Context(l3cid);
        l3ab.setIpAddress(new IpAddress(TEST_IPV4_1));
        l3List = new ArrayList<L3Address>();
        l3List.add(l3ab.build());
        l3ab.setIpAddress(new IpAddress(TEST_IPV4_2));
        l3List.add(l3ab.build());
        l3ab.setIpAddress(new IpAddress(TEST_IPV4_3));
        l3List.add(l3ab.build());
        op.setL3AddressList(l3List);
    }


    @Test
    public void testEpPut() throws Exception {

        op.put(mockWriter);
        verify(mockWriter).put(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<EndpointL3>>any(), Matchers.<EndpointL3>any());
        EndpointL3 epl3 = op.getEp();
        assertTrue(epl3 != null);
        assertTrue(epl3.getEndpointGroup().equals(egid));
        assertTrue(epl3.getTenant().equals(tid));
        assertTrue(epl3.getIpAddress().equals(ip));
        assertTrue(epl3.getL3Address().equals(l3List));
        assertTrue(epl3.getMacAddress().equals(mac));
        assertTrue(epl3.getTimestamp().equals(prr));
        assertTrue(epl3.getL3Context().equals(l3cid));

    }


    @Test
    public void testEpDelete() throws Exception {
        op.delete(mockWriter);
        verify(mockWriter).delete(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<EndpointL3>>any());

    }

    @Test
    public void testEpRead() throws Exception {
        executor = Executors.newScheduledThreadPool(1);
        assertTrue(executor != null);
        when(mockReader.read(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<EndpointL3>>any())).thenReturn(mockFuture);
        op.read(mockReader, executor);
        verify(mockReader).read(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<EndpointL3>>any());
        EndpointL3 epl3 = op.getEp();
        assertTrue(epl3 != null);
        assertTrue(epl3.getEndpointGroup().equals(egid));
        assertTrue(epl3.getTenant().equals(tid));
        assertTrue(epl3.getIpAddress().equals(ip));
        assertTrue(epl3.getL3Address().equals(l3List));
        assertTrue(epl3.getMacAddress().equals(mac));
        assertTrue(epl3.getTimestamp().equals(prr));
        assertTrue(epl3.getL3Context().equals(l3cid));

    }

    @Test
    public void testCallback() throws Exception {
    	this.callbacks = 0;

    	// pre-seed the EP
        op.setEp(op.buildEp());

        when(mockOption.isPresent()).thenReturn(true);
    	when(mockOption.get()).thenReturn(op.getEp());

    	op.onSuccess(mockOption);
        EndpointL3 epl3 = op.getEp();
        assertTrue(epl3 != null);
        assertTrue(epl3.getEndpointGroup().equals(egid));
        assertTrue(epl3.getTenant().equals(tid));
        assertTrue(epl3.getIpAddress().equals(ip));
        assertTrue(epl3.getL3Address().equals(l3List));
        assertTrue(epl3.getMacAddress().equals(mac));
        assertTrue(epl3.getTimestamp().equals(prr));
        assertTrue(epl3.getL3Context().equals(l3cid));
        assertTrue(this.callbacks == TEST_SIZE);
    }


}
