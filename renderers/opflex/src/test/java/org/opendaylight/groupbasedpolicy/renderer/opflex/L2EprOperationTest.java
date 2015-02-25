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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;



/**
 *
 */
public class L2EprOperationTest implements EprOperation.EprOpCallback  {
    protected static final Logger logger = LoggerFactory.getLogger(L2EprOperationTest.class);

    private static final int TEST_SIZE = 1;
    L2EprOperation op = null;
    private int callbacks;

   @Mock
    private WriteTransaction mockWriter;
    @Mock
    private ReadOnlyTransaction mockReader;
    @Mock
    private Identity mockId;
    @Mock
    private L2BridgeDomainId mockL2Context;
    @Mock
    private MacAddress mockMac;
    @Mock
    private CheckedFuture<Optional<Endpoint>,ReadFailedException> mockFuture;
    @Mock
    private Optional<Endpoint> mockOption;
    @Mock
    private Endpoint mockEp;
    @Mock
    private List<String> mockIdentityList;
    private ScheduledExecutorService executor;

    private static final String TEST_TENANT_ID = "e9fbd015-df23-4749-abec-8ba63bc0e738";
    private static final String TEST_EPG_ID = "8e359239-a253-42c3-9858-9acc039f1913";
    private static final String TEST_BD_ID = "badac187-2f98-416b-a931-2d0ee58be6b9";
    private static final String TEST_CONTEXT = "eef1f1de-18f8-4adc-910d-2141bf3e6699";
    private static final String TEST_IP1 = "192.168.194.131";
    private static final String TEST_IP2 = "192.168.194.132";
    private static final String TEST_IP3 = "192.168.194.133";
    private static final String TEST_MAC = "de:ad:be:ef:00:ba";
    private static final int TEST_PRR = 100;

    private static Ipv4Address TEST_IPV4_1, TEST_IPV4_2, TEST_IPV4_3;
    private static TenantId tid;
    private static EndpointGroupId egid;
    private static L2BridgeDomainId l2bdid;
    private static L3ContextId l3cid;
    private static MacAddress mac;
    private static List<L3Address> l3List;
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
        op =  new L2EprOperation(TEST_PRR);
        op.setCallback(this);

        tid = new TenantId(TEST_TENANT_ID);
        egid = new EndpointGroupId(TEST_EPG_ID);
        l3cid = new L3ContextId(TEST_CONTEXT);
        mac = new MacAddress(TEST_MAC);
        prr = new Long(TEST_PRR);
        l2bdid = new L2BridgeDomainId(TEST_BD_ID);

        op.setTenantId(tid);
        op.setEndpointGroupId(egid);
        op.setContextId(l2bdid);
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
        		Matchers.<InstanceIdentifier<Endpoint>>any(), Matchers.<Endpoint>any());
        Endpoint ep = op.getEp();
        assertTrue(ep != null);
        assertTrue(ep.getEndpointGroup().equals(egid));
        assertTrue(ep.getTenant().equals(tid));
        assertTrue(ep.getL3Address().equals(l3List));
        assertTrue(ep.getMacAddress().equals(mac));
        assertTrue(ep.getTimestamp().equals(prr));


    }

    @Test
    public void testEpDelete() throws Exception {
        op.delete(mockWriter);
        verify(mockWriter).delete(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<Endpoint>>any());

    }

    @Test
    public void testRead() throws Exception {
        executor = Executors.newScheduledThreadPool(1);
        assertTrue(executor != null);
        when(mockReader.read(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<Endpoint>>any())).thenReturn(mockFuture);
        op.read(mockReader, executor);
        verify(mockReader).read(eq(LogicalDatastoreType.OPERATIONAL),
        		Matchers.<InstanceIdentifier<Endpoint>>any());
        Endpoint ep = op.getEp();
        assertTrue(ep != null);
        assertTrue(ep.getEndpointGroup().equals(egid));
        assertTrue(ep.getTenant().equals(tid));
        assertTrue(ep.getL3Address().equals(l3List));
        assertTrue(ep.getMacAddress().equals(mac));
        assertTrue(ep.getTimestamp().equals(prr));
    }

    @Test
    public void testCallback() throws Exception {
    	this.callbacks = 0;

    	// pre-seed the EP
        op.setEp(op.buildEp());

        when(mockOption.isPresent()).thenReturn(true);
    	when(mockOption.get()).thenReturn(op.getEp());

    	op.onSuccess(mockOption);
        Endpoint ep = op.getEp();
        assertTrue(ep != null);
        assertTrue(ep.getEndpointGroup().equals(egid));
        assertTrue(ep.getTenant().equals(tid));
        assertTrue(ep.getL3Address().equals(l3List));
        assertTrue(ep.getMacAddress().equals(mac));
        assertTrue(ep.getTimestamp().equals(prr));
        assertTrue(this.callbacks == TEST_SIZE);

    }

}
