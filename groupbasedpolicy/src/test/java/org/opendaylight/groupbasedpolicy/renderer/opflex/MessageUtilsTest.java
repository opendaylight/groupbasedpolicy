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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.AgentOvsMit;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.MitLib;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyUri;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MessageUtilsTest {
    protected static final Logger logger = LoggerFactory.getLogger(MessageUtilsTest.class);
    public static final String SEP = "/";
    public static final String TENANT_PREFIX = "/PolicyUniverse/PolicySpace";
    public static final String CONTRACT_RN = "contract";
    public static final String EPG_RN = "GbpEpGroup";
    public static final String TENANT_UUID = "8ca978fa-05bc-4120-b037-f74802d18396";
    public static final String EPG_UUID = "420c5855-0578-4ca5-b3d2-3057e640e55a";
    public static final String EPG_NAME = "webFarm1";

    public static final String TEST_TARGET_NAME1 = "target1";
    public static final String TEST_TARGET_NAME2 = "target2";
    public static final String TEST_CONTRACT_ID1 = "bcef4a60-ce45-4eb2-9a47-5d93bf6877bc";
    public static final String TEST_CONTRACT_ID2 = "e8de1a72-6d0b-45e4-9980-a425b2b4a40d";
    public static final Integer TEST_RULE_ORDER = 1;
    public static final String TEST_RULE_NAME1 = "rule1";
    public static final String TEST_ACTION_NAME1 = "action1";
    public static final String TEST_ACTION_NAME2 = "action2";
    public static final Integer TEST_ACTION_ORDER1 = 1;
    public static final Integer TEST_ACTION_ORDER2 = 2;
    public static final String TEST_CLASSIFIER_NAME1 = "classifier1";
    public static final String TEST_CLASSIFIER_NAME2 = "classifier2";
    public static final String TEST_CLASSIFIER_INSTANCE_NAME1 = "classifierInstance1";
    public static final String TEST_CLASSIFIER_INSTANCE_NAME2 = "classifierInstance2";
    private static final String TEST_URI1 = TENANT_PREFIX + PolicyUri.POLICY_URI_SEP + TENANT_UUID + SEP + EPG_RN + SEP + EPG_UUID;
    private static final String TEST_SELECTOR_NAME1 = "selector1";
    private static final String TEST_NETWORK_DOMAIN_ID = "9AF7B4EF-1C5B-4FA9-A769-F368F781C4E6";

    private static final String TEST_IP_SUBNET_1 = "10.0.2.23/24";
    private static final String TEST_IP_SUBNET_2 = "192.168.194.1/24";
    private static final String TEST_IP_SUBNET_3 = "192.168.195.23/23";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        MessageUtils.init();
    	MessageUtils.setMit(new AgentOvsMit());
    	MessageUtils.setOpflexLib(new MitLib());
    }


    @Test
    public void testUri() throws Exception {

        PolicyUri uri = MessageUtils.parseUri(TEST_URI1);
        assertTrue(uri != null);
        int index = 0;
        String element = uri.getElement(index++);
        while (element != null) {
            System.out.println("Element: " + element);
            element = uri.getElement(index++);
        }
        assertTrue(uri.contains(EPG_RN));
        index = uri.whichElement(EPG_UUID);
        assertTrue(index == uri.totalElements()-1);

        assertTrue(MessageUtils.hasEpg(TEST_URI1));
        assertTrue(MessageUtils.isEpgUri(TEST_URI1));

    }


    private List<ConsumerNamedSelector> getTestConsumerNamedSelectorList() {
        List<ConsumerNamedSelector> cns = new ArrayList<ConsumerNamedSelector>();

        ConsumerNamedSelectorBuilder cnsb = new ConsumerNamedSelectorBuilder();
        cnsb.setContract(getTestContractIdList());
        cnsb.setName(new SelectorName(TEST_SELECTOR_NAME1));
        cns.add(cnsb.build());

        return cns;
    }


    private List<ContractId> getTestContractIdList() {
        List<ContractId> cid = new ArrayList<ContractId>();

        cid.add(new ContractId(TEST_CONTRACT_ID1));
        cid.add(new ContractId(TEST_CONTRACT_ID2));

        return cid;
    }

    private List<ProviderNamedSelector> getTestProviderNamedSelectorList() {
        List<ProviderNamedSelector> pns = new ArrayList<ProviderNamedSelector>();

        ProviderNamedSelectorBuilder pnsb = new ProviderNamedSelectorBuilder();
        pnsb.setContract(getTestContractIdList());
        pnsb.setName(new SelectorName(TEST_SELECTOR_NAME1));

        pns.add(pnsb.build());

        return pns;
    }


    private void printMos(Set<ManagedObject> mol) {
    	for (ManagedObject mo: mol) {
	        if (mo == null) return;

	        if (mo.getProperties() != null) {
	            for (ManagedObject.Property p : mo.getProperties()) {
	                assertTrue(p != null);
	                System.out.println("\t" + p.getName() + ": " + p.getData());
	            }
	        }
	        if (mo.getChildren() == null) return;

	        for (Uri children: mo.getChildren()) {
	            assertTrue(children != null);
	        }
    	}
    }

    @Test
    public void testIsGenieUri() throws Exception {
        PolicyUri uri = new PolicyUri();
        uri.push("PolicyUniverse");
        uri.push("PolicySpace");
        uri.push(TENANT_UUID);
        uri.push("GbpEpGroup");
        assertTrue(MessageUtils.isGenieUri(new Uri(uri.toString())));
    }

    private static final String TEST_URI_1 =
    		"/endpoints/endpoint/e60dec20-ff2c-4b10-90a3-e8c27ce9fd08/00:11:22:33:44:55";
    private static final String TEST_URI_2 =
    		"/endpoints/endpoint-l3/e60dec20-ff2c-4b10-90a3-e8c27ce9fd08/192.168.10.3";
    private static final String TEST_URI_3 = "/tenants/tenant/51134b1e-6047-4d51-8d07-4135afd3672f";
    private static final String TEST_URI_4 =
    		TEST_URI_3 + "/contract/81cb5b9f-b443-4d85-9da2-dfe2b3e5f7a3";
    private static final String TEST_URI_5 =
    		TEST_URI_4 + "/subject/HTTP";
    private static final String TEST_URI_6 =
    		TEST_URI_5 + "/rule/block";
    private static final String TEST_URI_7 =
    		TEST_URI_3 + "/l2-flood-domain/e2168e8d-856c-4927-9695-156ed567d6a8";
    private static final String TEST_URI_8 =
    		TEST_URI_3 + "/l2-bridge-domain/1d148938-38e3-41e5-a6dc-4b35541a498b";
    private static final String TEST_URI_9 =
    		TEST_URI_3 + "/subnet/99693e3c-a5e1-4229-a3b6-9aae9cf69b61";
    private static final String TEST_URI_10 =
    		TEST_URI_3 + "/l3-context/bf11bd5b-7a45-412d-bff3-c9c1f1770023";


//    private static final String TEST_GENIE_URI_1 =
//    		"/EprL2Universe/EprL2Ep/e60dec20-ff2c-4b10-90a3-e8c27ce9fd08/00:11:22:33:44:55";
//    private static final String TEST_GENIE_URI_2 =
//    		"/EprL3Universe/EprL3Ep/e60dec20-ff2c-4b10-90a3-e8c27ce9fd08/192.168.10.3";

    //@Test
    public void testOdlUriIterator() throws Exception {
    	PolicyUri uri = new PolicyUri(TEST_URI_1);
    	MessageUtils.UriIterator it = new MessageUtils.UriIterator(uri, MessageUtils.getOdlKeys());

    	while (it.hasNext()) {
    		if (it.isKey()) {
    			System.out.println("Key is " + it.getElement());
    		}
    		else {
    			System.out.println("Element is " + it.getElement());
    		}
    		it.next();
    	}
    	uri = new PolicyUri(TEST_URI_2);
    	it = new MessageUtils.UriIterator(uri, MessageUtils.getOdlKeys());
    	while (it.hasNext()) {
    		if (it.isKey()) {
    			System.out.println("Key is " + it.getElement());
    		}
    		else {
    			System.out.println("Element is " + it.getElement());
    		}
    		it.next();
    	}
    }

    //@Test
    public void testOdlUritoGenieUri() throws Exception {
    	PolicyUri guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_1));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_2));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_3));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_4));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_5));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_6));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_7));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_8));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_9));
    	System.out.println(guri.originalPath());
    	guri = MessageUtils.odlUriToGenieUri(new PolicyUri(TEST_URI_10));
    	System.out.println(guri.originalPath());
    }

    //@Test
    public void testGetEndpointGroup() throws Exception {
    	TenantBuilder tb = new TenantBuilder();
    	tb.setId(new TenantId(TENANT_UUID));
    	Tenant t = tb.build();
    	ManagedObject epgMo = new ManagedObject();

    	RuleGroup rg = new RuleGroup(null, 0, t, null, null);

        EndpointGroupBuilder epgb = new EndpointGroupBuilder();
        epgb.setConsumerNamedSelector(getTestConsumerNamedSelectorList());
        epgb.setProviderNamedSelector(getTestProviderNamedSelectorList());

        epgb.setIntraGroupPolicy(IntraGroupPolicy.Allow);
        epgb.setNetworkDomain(new NetworkDomainId(TEST_NETWORK_DOMAIN_ID));
        epgb.setName(new Name(EPG_NAME));
        epgb.setId(new EndpointGroupId(EPG_UUID));

        EndpointGroup epg = epgb.build();
        assertTrue(epg != null);
        PolicyUri uri = new PolicyUri();
        uri.push("PolicyUniverse");
        uri.push("PolicySpace");
        uri.push(TENANT_UUID);
        uri.push("GbpEpGroup");
        uri.push(epg.getId().getValue());
        Set<ManagedObject> children = MessageUtils.getEndpointGroupMo(epgMo, uri, epg, rg);
        printMos(children);
    }

    //@Test
    public void testIpv4PlusSubnet() throws Exception {
    	MessageUtils.Ipv4PlusSubnet ipv4 = new MessageUtils.Ipv4PlusSubnet(TEST_IP_SUBNET_1);
    	System.out.println("Prefix is " + ipv4.getPrefixAsString() + ", Mask is " + ipv4.getMaskAsString());
    	ipv4 = new MessageUtils.Ipv4PlusSubnet(TEST_IP_SUBNET_2);
    	System.out.println("Prefix is " + ipv4.getPrefixAsString() + ", Mask is " + ipv4.getMaskAsString());
    	ipv4 = new MessageUtils.Ipv4PlusSubnet(TEST_IP_SUBNET_3);
    	System.out.println("Prefix is " + ipv4.getPrefixAsString() + ", Mask is " + ipv4.getMaskAsString());
    }
}
