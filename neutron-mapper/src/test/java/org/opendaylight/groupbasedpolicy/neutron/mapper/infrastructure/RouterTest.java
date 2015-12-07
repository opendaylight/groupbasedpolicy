package org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.GbpDataBrokerTest;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

public class RouterTest extends GbpDataBrokerTest {

    private static final String IPV4_NAME = "IPv4";
    private static final String IPV6_NAME = "IPv6";
    private static final SubjectName ROUTER_SUBJECT_NAME = new SubjectName("ALLOW_IPv4_IPv6");
    private final String tenantId = "00000000-0000-0000-0000-000000000002";

    @Test
    public void instantiate() {
        Router service = new Router();
        assertNotNull(service);
    }

    @Test
    public void testWriteRouterClauseWithConsProvEicIpv4() throws Exception {
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        String ipv4Prefix = "170.0.0.1/8";
        Router.writeRouterClauseWithConsProvEic(new TenantId(tenantId), new IpPrefix(new Ipv4Prefix(ipv4Prefix)), rwTx);
        rwTx.submit().get();

        //expected clause name
        String clauseNameIpV4 = ROUTER_SUBJECT_NAME.getValue() + MappingUtils.NAME_DOUBLE_DELIMETER + ipv4Prefix;
        clauseNameIpV4 = clauseNameIpV4.replace('/', '_');

        PolicyAssert.assertClauseExists(dataBroker, tenantId, Router.CONTRACT_ID.getValue(), clauseNameIpV4);

    }

    @Test
    public void testWriteRouterClauseWithConsProvEicIpv6() throws Exception {
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        String ipv6Prefix = "2001:0db8:85a3:0000:0000:8a2e:0370:7334/128";
        Router.writeRouterClauseWithConsProvEic(new TenantId(tenantId), new IpPrefix(new Ipv6Prefix(ipv6Prefix)), rwTx);
        rwTx.submit().get();

        //expected clause name
        String clauseNameIpV6 = ROUTER_SUBJECT_NAME.getValue() + MappingUtils.NAME_DOUBLE_DELIMETER + ipv6Prefix;
        clauseNameIpV6 = clauseNameIpV6.replace('/', '_').replace(':', '.');

        PolicyAssert.assertClauseExists(dataBroker, tenantId, Router.CONTRACT_ID.getValue(), clauseNameIpV6);
    }

    @Test
    public void testWriteRouterEntitiesToTenant() throws Exception {
        //write everything
        DataBroker dataBroker = getDataBroker();
        ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        Router.writeRouterEntitiesToTenant(new TenantId(tenantId), rwTx);
        rwTx.submit().get();

        //read classifier instances
        PolicyAssert.assertClassifierInstanceExists(dataBroker, tenantId, IPV4_NAME);
        PolicyAssert.assertClassifierInstanceExists(dataBroker, tenantId, IPV6_NAME);

        //read contract
        PolicyAssert.assertContractExists(dataBroker, tenantId, Router.CONTRACT_ID.getValue());

        //read group id
        PolicyAssert.assertEndpointGroupExists(dataBroker, tenantId, Router.EPG_ID.getValue());
    }

    @Test
    public void testGetAllClassifierInstances() {
        Set<ClassifierInstance> classifierInstances = Router.getAllClassifierInstances();
        assertNotNull(classifierInstances);
        assertFalse(classifierInstances.isEmpty());
        assertEquals(classifierInstances.size(), 2);
    }

}
