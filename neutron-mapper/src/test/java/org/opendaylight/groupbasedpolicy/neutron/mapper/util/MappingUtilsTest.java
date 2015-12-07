package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L2FloodDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.forwarding.context.L3ContextKey;

public class MappingUtilsTest extends GbpDataBrokerTest {

    private final TenantId tenantId = new TenantId("00000000-0000-0000-0000-000000000001");
    private final L2FloodDomainId l2FloodDomainId = new L2FloodDomainId("00000000-0000-0000-0000-000000000002");
    private final L2BridgeDomainId l2BridgeDomainId = new L2BridgeDomainId("00000000-0000-0000-0000-000000000003");
    private final L3ContextId l3ContextId = new L3ContextId("00000000-0000-0000-0000-000000000004");
    private final String[] ips = {"170.0.0.1", "170.0.0.2", "170.0.0.3"};
    private L2FloodDomainBuilder l2FloodDomainBuilder;
    private List<Neutron_IPs> emptyList, singleIp, multiIp;
    private L2BridgeDomainBuilder l2BridgeDomainBuilder;
    private L3ContextBuilder l3ContextBuilder;

    @Before
    public void init() {
        l2FloodDomainBuilder = new L2FloodDomainBuilder();
        l2FloodDomainBuilder.setName(new Name("l2fdn"))
                .setKey(new L2FloodDomainKey(l2FloodDomainId));
        l2BridgeDomainBuilder = new L2BridgeDomainBuilder();
        l2BridgeDomainBuilder.setName(new Name("l2bdn"))
                .setKey(new L2BridgeDomainKey(l2BridgeDomainId));
        l3ContextBuilder = new L3ContextBuilder();
        l3ContextBuilder.setName(new Name("l3cn"))
                .setKey(new L3ContextKey(l3ContextId));
        Neutron_IPs neutronIp1 = new Neutron_IPs();
        Neutron_IPs neutronIp2 = new Neutron_IPs();
        Neutron_IPs neutronIp3 = new Neutron_IPs();
        neutronIp1.setIpAddress(ips[0]);
        neutronIp2.setIpAddress(ips[1]);
        neutronIp3.setIpAddress(ips[2]);
        emptyList = new ArrayList<>();
        singleIp = new ArrayList<>();
        singleIp.add(neutronIp1);
        multiIp = new ArrayList<>();
        multiIp.add(neutronIp1);
        multiIp.add(neutronIp2);
        multiIp.add(neutronIp3);
    }

    @Test
    public void testCreateForwardingContext1() {
        //case #1 - L2FloodDomain is not present in DS
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        MappingUtils.ForwardingCtx
                forwardingCtx =
                MappingUtils.createForwardingContext(tenantId, l2FloodDomainBuilder.build().getId(), rwTx);
        assertNotNull(forwardingCtx);
        assertNull(forwardingCtx.getL2FloodDomain());
        assertNull(forwardingCtx.getL2BridgeDomain());
        assertNull(forwardingCtx.getL3Context());
    }

    @Test
    public void testCreateForwardingContext2() {
        //case #2 - L2FloodDomain is present in DS, but its parent is null
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        l2FloodDomainBuilder.setParent(null);
        L2FloodDomain l2FloodDomain = l2FloodDomainBuilder.build();
        writeL2FloodDomain(tenantId, l2FloodDomain, rwTx);
        MappingUtils.ForwardingCtx
                forwardingCtx =
                MappingUtils.createForwardingContext(tenantId, l2FloodDomainBuilder.build().getId(), rwTx);
        assertNotNull(forwardingCtx);
        assertTrue(forwardingCtx.getL2FloodDomain().equals(l2FloodDomain));
        assertNull(forwardingCtx.getL2BridgeDomain());
        assertNull(forwardingCtx.getL3Context());
    }

    @Test
    public void testCreateForwardingContext3() {
        //case #3 - L2FloodDomain is present in DS with not-null parent, but L2BridgeDomain is not present in DS
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        l2FloodDomainBuilder.setParent(l2BridgeDomainBuilder.build().getId());
        L2FloodDomain l2FloodDomain = l2FloodDomainBuilder.build();
        writeL2FloodDomain(tenantId, l2FloodDomain, rwTx);
        MappingUtils.ForwardingCtx
                forwardingCtx =
                MappingUtils.createForwardingContext(tenantId, l2FloodDomainBuilder.build().getId(), rwTx);
        assertNotNull(forwardingCtx);
        assertTrue(forwardingCtx.getL2FloodDomain().equals(l2FloodDomain));
        assertNull(forwardingCtx.getL2BridgeDomain());
        assertNull(forwardingCtx.getL3Context());
    }

    @Test
    public void testCreateForwardingContext4() {
        //case #4 - L2BridgeDomain is also present in DS but with null parent
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        l2BridgeDomainBuilder.setParent(null);
        l2FloodDomainBuilder.setParent(l2BridgeDomainBuilder.build().getId());
        L2FloodDomain l2FloodDomain = l2FloodDomainBuilder.build();
        L2BridgeDomain l2BridgeDomain = l2BridgeDomainBuilder.build();
        writeL2FloodDomain(tenantId, l2FloodDomain, rwTx);
        writeL2BridgeDomain(tenantId, l2BridgeDomain, rwTx);
        MappingUtils.ForwardingCtx
                forwardingCtx =
                MappingUtils.createForwardingContext(tenantId, l2FloodDomainBuilder.build().getId(), rwTx);
        assertNotNull(forwardingCtx);
        assertTrue(forwardingCtx.getL2FloodDomain().equals(l2FloodDomain));
        assertTrue(forwardingCtx.getL2BridgeDomain().equals(l2BridgeDomain));
        assertNull(forwardingCtx.getL3Context());
    }

    @Test
    public void testCreateForwardingContext5() {
        //case #5 - L2BridgeDomain is also present in DS with not-null parent, but L3Context is not present in DS
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        l2BridgeDomainBuilder.setParent(l3ContextBuilder.build().getId());
        l2FloodDomainBuilder.setParent(l2BridgeDomainBuilder.build().getId());
        L2FloodDomain l2FloodDomain = l2FloodDomainBuilder.build();
        L2BridgeDomain l2BridgeDomain = l2BridgeDomainBuilder.build();
        writeL2FloodDomain(tenantId, l2FloodDomain, rwTx);
        writeL2BridgeDomain(tenantId, l2BridgeDomain, rwTx);
        MappingUtils.ForwardingCtx
                forwardingCtx =
                MappingUtils.createForwardingContext(tenantId, l2FloodDomainBuilder.build().getId(), rwTx);
        assertNotNull(forwardingCtx);
        assertTrue(forwardingCtx.getL2FloodDomain().equals(l2FloodDomain));
        assertTrue(forwardingCtx.getL2BridgeDomain().equals(l2BridgeDomain));
        assertNull(forwardingCtx.getL3Context());
    }

    @Test
    public void testCreateForwardingContext6() {
        //case #6 - L3Context present in DS
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        l2BridgeDomainBuilder.setParent(l3ContextBuilder.build().getId());
        l2FloodDomainBuilder.setParent(l2BridgeDomainBuilder.build().getId());
        L2FloodDomain l2FloodDomain = l2FloodDomainBuilder.build();
        L2BridgeDomain l2BridgeDomain = l2BridgeDomainBuilder.build();
        L3Context l3Context = l3ContextBuilder.build();
        writeL2FloodDomain(tenantId, l2FloodDomain, rwTx);
        writeL2BridgeDomain(tenantId, l2BridgeDomain, rwTx);
        writeL3Context(tenantId, l3Context, rwTx);
        MappingUtils.ForwardingCtx
                forwardingCtx =
                MappingUtils.createForwardingContext(tenantId, l2FloodDomainBuilder.build().getId(), rwTx);
        assertNotNull(forwardingCtx);
        assertTrue(forwardingCtx.getL2FloodDomain().equals(l2FloodDomain));
        assertTrue(forwardingCtx.getL2BridgeDomain().equals(l2BridgeDomain));
        assertTrue(forwardingCtx.getL3Context().equals(l3Context));
    }

    @Test
    public void testGetFirstIpNullIp() {
        Neutron_IPs result = MappingUtils.getFirstIp(emptyList);
        assertNull(result);
    }

    @Test
    public void testGetFirstIpOneIp() {
        Neutron_IPs result = MappingUtils.getFirstIp(singleIp);
        assertNotNull(result);
        assertEquals(result.getIpAddress(), ips[0]);
    }

    @Test
    public void testGetFirstIpMoreIps() {
        Neutron_IPs result = MappingUtils.getFirstIp(multiIp);
        assertNotNull(result);
        assertEquals(result.getIpAddress(), ips[0]);
    }

    private void writeL2FloodDomain(TenantId tenantId, L2FloodDomain l2Fd, ReadWriteTransaction rwTx) {
        checkNotNull(l2Fd);
        L2FloodDomainId l2FdId = l2Fd.getId();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2FloodDomainIid(tenantId, l2FdId), l2Fd);
    }

    private void writeL2BridgeDomain(TenantId tenantId, L2BridgeDomain l2Bd, ReadWriteTransaction rwTx) {
        checkNotNull(l2Bd);
        L2BridgeDomainId l2BdId = l2Bd.getId();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l2BridgeDomainIid(tenantId, l2BdId), l2Bd);
    }

    private void writeL3Context(TenantId tenantId, L3Context l3Ct, ReadWriteTransaction rwTx) {
        checkNotNull(l3Ct);
        L3ContextId l3CtId = l3Ct.getId();
        rwTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.l3ContextIid(tenantId, l3CtId), l3Ct);
    }

}
