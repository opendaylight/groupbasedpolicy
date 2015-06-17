package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

public class UtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public final void testCreateIpPrefix_ipv4() {
        String ipv4Prefix = "1.1.1.1/8";
        IpPrefix ipPrefix = Utils.createIpPrefix(ipv4Prefix);
        Assert.assertNotNull(ipPrefix);
        Assert.assertNull(ipPrefix.getIpv6Prefix());
        assertEquals(ipv4Prefix, ipPrefix.getIpv4Prefix().getValue());
    }

    @Test
    public final void testCreateIpPrefix_ipv6() {
        String ipv6Prefix = "fd1c:29d6:85d1::/48";
        IpPrefix ipPrefix = Utils.createIpPrefix(ipv6Prefix);
        Assert.assertNotNull(ipPrefix);
        Assert.assertNull(ipPrefix.getIpv4Prefix());
        assertEquals(ipv6Prefix, ipPrefix.getIpv6Prefix().getValue());
    }

    @Test
    public final void testCreateIpPrefix_null() {
        thrown.expect(IllegalArgumentException.class);
        Utils.createIpPrefix(null);
    }

    @Test
    public final void testCreateIpPrefix_emptyString() {
        thrown.expect(IllegalArgumentException.class);
        Utils.createIpPrefix("");
    }

    @Test
    public final void testCreateIpPrefix_invalidFormat() {
        thrown.expect(IllegalArgumentException.class);
        Utils.createIpPrefix("1.1.1.1/33");
    }

    @Test
    public final void testCreateIpAddress_ipv4() {
        String ipv4Address = "1.1.1.1";
        IpAddress ipAddress = Utils.createIpAddress(ipv4Address);
        Assert.assertNotNull(ipAddress);
        Assert.assertNull(ipAddress.getIpv6Address());
        assertEquals(ipv4Address, ipAddress.getIpv4Address().getValue());
    }
    
    @Test
    public final void testCreateIpAddress_ipv6() {
        String ipv6Address = "2001:db8::211:22ff:fe33:4455";
        IpAddress ipAddress = Utils.createIpAddress(ipv6Address);
        Assert.assertNotNull(ipAddress);
        Assert.assertNull(ipAddress.getIpv4Address());
        assertEquals(ipv6Address, ipAddress.getIpv6Address().getValue());
    }

    @Test
    public final void testCreateIpAddress_null() {
        thrown.expect(IllegalArgumentException.class);
        Utils.createIpAddress(null);
    }

    @Test
    public final void testCreateIpAddress_emptyString() {
        thrown.expect(IllegalArgumentException.class);
        Utils.createIpAddress("");
    }

    @Test
    public final void testCreateIpAddress_invalidFormat() {
        thrown.expect(IllegalArgumentException.class);
        Utils.createIpAddress("1.1.1.256");
    }

    @Test
    public final void testGetStringIpPrefix_ipv4() {
        String ipv4Prefix = "1.1.1.1/8";
        assertEquals(ipv4Prefix, Utils.getStringIpPrefix(new IpPrefix(new Ipv4Prefix(ipv4Prefix))));
    }

    @Test
    public final void testGetStringIpPrefix_ipv6() {
        String ipv6Prefix = "fd1c:29d6:85d1::/48";
        assertEquals(ipv6Prefix, Utils.getStringIpPrefix(new IpPrefix(new Ipv6Prefix(ipv6Prefix))));
    }

    @Test
    public final void testGetStringIpPrefix_null() {
        thrown.expect(NullPointerException.class);
        Utils.getStringIpPrefix(null);
    }
    
    @Test
    public final void testGetStringIpAddress_ipv4() {
        String ipv4Address = "1.1.1.1";
        assertEquals(ipv4Address, Utils.getStringIpAddress(new IpAddress(new Ipv4Address(ipv4Address))));
    }

    @Test
    public final void testGetStringIpAddress_ipv6() {
        String ipv6Address = "2001:db8::211:22ff:fe33:4455";
        assertEquals(ipv6Address, Utils.getStringIpAddress(new IpAddress(new Ipv6Address(ipv6Address))));
    }

    @Test
    public final void testGetStringIpAddress_null() {
        thrown.expect(NullPointerException.class);
        Utils.getStringIpAddress(null);
    }

    @Test
    public final void testNormalizeUuid_lowercaseUuid() {
        assertEquals("01234567-abcd-ef01-0123-0123456789ab",
                Utils.normalizeUuid("01234567-abcd-ef01-0123-0123456789ab"));
    }

    @Test
    public final void testNormalizeUuid_uppercaseUuid() {
        assertEquals("01234567-ABCD-EF01-0123-0123456789AB",
                Utils.normalizeUuid("01234567-ABCD-EF01-0123-0123456789AB"));
    }

    @Test
    public final void testNormalizeUuid_mixUuid() {
        assertEquals("01234567-ABCD-ef01-0123-0123456789Ab",
                Utils.normalizeUuid("01234567-ABCD-ef01-0123-0123456789Ab"));
    }

    @Test
    public final void testNormalizeUuid_noSlashLowercaseUuid() {
        assertEquals("01234567-abcd-ef01-0123-0123456789ab", Utils.normalizeUuid("01234567abcdef0101230123456789ab"));
    }

    @Test
    public final void testNormalizeUuid_noSlashUppercaseUuid() {
        assertEquals("01234567-ABCD-EF01-0123-0123456789AB", Utils.normalizeUuid("01234567ABCDEF0101230123456789AB"));
    }

    @Test
    public final void testNormalizeUuid_noSlashMixUuid() {
        assertEquals("01234567-ABCD-ef01-0123-0123456789Ab", Utils.normalizeUuid("01234567ABCDef0101230123456789Ab"));
    }

    @Test
    public final void testNormalizeUuid_emptyUuid() {
        assertEquals("", Utils.normalizeUuid(""));
    }

    @Test
    public final void testNormalizeUuid_badUuid() {
        assertEquals("abcdxy", Utils.normalizeUuid("abcdxy"));
    }

    @Test
    public final void testNormalizeUuid_nullUuid() {
        thrown.expect(NullPointerException.class);
        Utils.normalizeUuid(null);
    }

}
