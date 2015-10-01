package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;

public class SecGroupDaoTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SecGroupDao secGroupDao;

    @Before
    public void setUp() throws Exception {
        secGroupDao = new SecGroupDao();
    }

    @Test
    public void testAddAndRemoveSecGroup() {
        NeutronSecurityGroup secGroup1 = NeutronEntityFactory.securityGroup("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        NeutronSecurityGroup secGroup2 = NeutronEntityFactory.securityGroup("cccccccc-cccc-cccc-cccc-cccccccccccc",
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // one security group
        secGroupDao.addSecGroup(secGroup1);
        EndpointGroupId epGroupId1 = new EndpointGroupId(secGroup1.getSecurityGroupUUID());
        NeutronSecurityGroup daoSecGroup = secGroupDao.getSecGroupById(epGroupId1);
        assertNotNull(daoSecGroup);
        assertEquals(secGroup1, daoSecGroup);

        secGroupDao.removeSecGroup(epGroupId1);
        assertNull(secGroupDao.getSecGroupById(epGroupId1));

        // two security groups
        secGroupDao.addSecGroup(secGroup1);
        secGroupDao.addSecGroup(secGroup2);
        daoSecGroup = secGroupDao.getSecGroupById(epGroupId1);
        assertNotNull(daoSecGroup);
        assertEquals(secGroup1, daoSecGroup);
        EndpointGroupId epGroupId2 = new EndpointGroupId(secGroup2.getSecurityGroupUUID());
        daoSecGroup = secGroupDao.getSecGroupById(epGroupId2);
        assertNotNull(daoSecGroup);
        assertEquals(secGroup2, daoSecGroup);

        secGroupDao.removeSecGroup(epGroupId2);
        daoSecGroup = secGroupDao.getSecGroupById(epGroupId1);
        assertNotNull(daoSecGroup);
        assertEquals(secGroup1, daoSecGroup);
        assertNull(secGroupDao.getSecGroupById(epGroupId2));

        secGroupDao.removeSecGroup(epGroupId1);
        assertNull(secGroupDao.getSecGroupById(epGroupId1));
        assertNull(secGroupDao.getSecGroupById(epGroupId2));
    }

    @Test
    public void testAddSecGroup_replacementOfSecGroup() {
        NeutronSecurityGroup secGroup = NeutronEntityFactory.securityGroup("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        secGroupDao.addSecGroup(secGroup);
        EndpointGroupId epGroupId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
        NeutronSecurityGroup daoSecGroup = secGroupDao.getSecGroupById(epGroupId);
        assertNotNull(daoSecGroup);
        assertEquals(secGroup, daoSecGroup);

        // the same security group id but different tenant id
        secGroup = NeutronEntityFactory.securityGroup("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "cccccccc-cccc-cccc-cccc-cccccccccccc");
        secGroupDao.addSecGroup(secGroup);
        daoSecGroup = secGroupDao.getSecGroupById(epGroupId);
        assertNotNull(daoSecGroup);
        assertEquals(secGroup, daoSecGroup);
    }

    @Test
    public void testAddSecGroup_nullArgument() {
        thrown.expect(NullPointerException.class);
        secGroupDao.addSecGroup(null);
    }

    @Test
    public void testGetNameOrIdOfSecGroup_nullArgument() {
        thrown.expect(NullPointerException.class);
        secGroupDao.getNameOrIdOfSecGroup(null);
    }

    @Test
    public void testGetNameOrIdOfSecGroup_unknownEpGroupId() {
        EndpointGroupId unknownEpGroupId = new EndpointGroupId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        assertEquals("", secGroupDao.getNameOrIdOfSecGroup(unknownEpGroupId));
    }

    @Test
    public void testGetNameOrIdOfSecGroup_secGroupNameIsNull() {
        NeutronSecurityGroup secGroup = NeutronEntityFactory.securityGroupWithName(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", null);
        secGroupDao.addSecGroup(secGroup);
        EndpointGroupId epGroupId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
        String secGroupNameOrId = secGroupDao.getNameOrIdOfSecGroup(epGroupId);
        assertNotNull(secGroupNameOrId);
        assertEquals(epGroupId.getValue(), secGroupNameOrId);
    }

    @Test
    public void testGetNameOrIdOfSecGroup_secGroupNameIsEmpty() {
        NeutronSecurityGroup secGroup = NeutronEntityFactory.securityGroupWithName(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "");
        secGroupDao.addSecGroup(secGroup);
        EndpointGroupId epGroupId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
        String secGroupNameOrId = secGroupDao.getNameOrIdOfSecGroup(epGroupId);
        assertNotNull(secGroupNameOrId);
        assertEquals(epGroupId.getValue(), secGroupNameOrId);
    }

    @Test
    public void testGetNameOrIdOfSecGroup_secGroupNameIsNotEmpty() {
        NeutronSecurityGroup secGroup = NeutronEntityFactory.securityGroupWithName(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "secGroupName");
        secGroupDao.addSecGroup(secGroup);
        EndpointGroupId epGroupId = new EndpointGroupId(secGroup.getSecurityGroupUUID());
        String secGroupNameOrId = secGroupDao.getNameOrIdOfSecGroup(epGroupId);
        assertNotNull(secGroupNameOrId);
        assertEquals("secGroupName", secGroupNameOrId);
    }
}
