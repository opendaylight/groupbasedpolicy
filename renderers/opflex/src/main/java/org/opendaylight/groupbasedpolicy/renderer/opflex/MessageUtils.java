/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Authors : tbachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointIdentity;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject.Property;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.AgentOvsMit;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.MitLib;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyClassInfo;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyObjectInstance;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyObjectInstance.PolicyReference;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyUri;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class MessageUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MessageUtils.class);
    /*
     * Endpoint Groups in ODL's Group Based Policy are specified in the
     * following format:
     * 
     * /tenants/tenant/<tenant UUID>/endpoint-group/<endpoint-group UUID>
     */
    public static final String POLICY_ROOT = "";
    public static final String TENANTS_RN = "tenants";
    public static final String TENANT_RN = "tenant";
    public static final String CONTRACT_RN = "contract";
    public static final String SUBJECT_RN = "subject";
    public static final String RULE_RN = "rule";
    public static final String CLAUSE_RN = "clause";
    public static final String EPG_RN = "endpoint-group";
    public static final String ENDPOINTS_RN = "endpoints";
    public static final String ENDPOINT_RN = "endpoint";
    public static final String ENDPOINT_L3_RN = "endpoint-l3";
    public static final String L2_FLOOD_DOMAIN_RN = "l2-flood-domain";
    public static final String L2_BRIDGE_DOMAIN_RN = "l2-bridge-domain";
    public static final String SUBNET_RN = "subnet";
    public static final String L3_CONTEXT_RN = "l3-context";
    public static final String CLASSIFIER_INSTANCE_RN = "classifier-instance";
    public static final String CLASSIFIER_REF_RN = "classifier-ref";

    public static final String GENIE_EPR_L2_ROOT = "EprL2Universe";
    public static final String GENIE_EPR_L3_ROOT = "EprL3Universe";
    public static final String GENIE_ENDPOINT_RN = "EprL2Ep";
    public static final String GENIE_ENDPOINT_L3_RN = "EprL3Ep";

    public static final String GENIE_TENANTS_RN = "PolicyUniverse";
    public static final String GENIE_POLICY_ROOT = PolicyUri.POLICY_URI_SEP + GENIE_TENANTS_RN;
    public static final String GENIE_TENANT_RN = "PolicySpace";
    public static final String GENIE_CONTRACT_RN = "GbpContract";
    public static final String GENIE_SUBJECT_RN = "GbpSubject";
    public static final String GENIE_RULE_RN = "GbpRule";
    public static final String GENIE_EPG_RN = "GbpEpGroup";
    public static final String GENIE_CLASSIFIER_RN = "GbpeL24Classifier";
    public static final String GENIE_FLOOD_DOMAIN_RN = "GbpFloodDomain";
    public static final String GENIE_BRIDGE_DOMAIN_RN = "GbpBridgeDomain";
    public static final String GENIE_SUBNETS_RN = "GbpSubnets";
    public static final String GENIE_SUBNET_RN = "GbpSubnet";
    public static final String GENIE_ROUTING_DOMAIN_RN = "GbpRoutingDomain";
    public static final String GENIE_ENDPOINT_NET_RN = "EprL3Net";

    public static final String GENIE_ENDPOINT_CONTEXT = "context";
    public static final String GENIE_ENDPOINT_EPG = "group";
    public static final String GENIE_ENDPOINT_MAC = "mac";
    public static final String GENIE_ENDPOINT_UUID = "uuid";
    public static final String GENIE_ENDPOINT_IP = "ip";

    public static final String GENIE_SUBNET_NAME_DEFAULT = "default-subnet";
    public static final String GENIE_SUBNET_ADDRESS = "address";
    public static final String GENIE_SUBNET_NAME = "name";
    public static final String GENIE_SUBNET_PREFIX_LEN = "prefixLen";
    public static final String GENIE_SUBNET_VIRTUAL_ROUTER_IP = "virtualRouterIp";

    public static final String GENIE_CLASSIFIER_REF_RN = "GbpRuleToClassifierRSrc";
    public static final String GENIE_CONSUMER_CONTRACT_REF_RN = "GbpEpGroupToConsContractRSrc";
    public static final String GENIE_PROVIDER_CONTRACT_REF_RN = "GbpEpGroupToProvContractRSrc";
    public static final String GENIE_SUBNETS_TO_NETWORK_RN = "GbpSubnetsToNetworkRSrc";
    public static final String GENIE_FLOOD_DOMAIN_TO_NETWORK_RN = "GbpFloodDomainToNetworkRSrc";
    public static final String GENIE_BRIDGE_DOMAIN_TO_NETWORK_RN = "GbpBridgeDomainToNetworkRSrc";
    public static final String GENIE_INTRA_EPG_RN = "intraGroupPolicy";
    public static final String GENIE_EPG_TO_NETWORK_DOMAIN_RN = "GbpEpGroupToNetworkRSrc";
    public static final String GENIE_SUBNET_TO_NETWORK_SRC_REF = "GbpSubnetsToNetworkRSrc";

    public static final String TENANT_PREFIX = POLICY_ROOT + PolicyUri.POLICY_URI_SEP + TENANTS_RN
            + PolicyUri.POLICY_URI_SEP + TENANT_RN + PolicyUri.POLICY_URI_SEP;
    public static final String GENIE_TENANT_PREFIX = GENIE_POLICY_ROOT + PolicyUri.POLICY_URI_SEP + GENIE_TENANT_RN
            + PolicyUri.POLICY_URI_SEP;

    public static final String GENIE_CONTRACT_NAME = "name";
    public static final String GENIE_SUBJECT_NAME = "name";
    public static final String GENIE_RULE_NAME = "name";
    public static final String GENIE_CLASSIFIER_NAME = "name";
    public static final String GENIE_ENDPOINT_GROUP_NAME = "name";
    public static final String GENIE_SUBNETS_NAME = "name";
    public static final String GENIE_FLOOD_DOMAIN_NAME = "name";
    public static final String GENIE_BRIDGE_DOMAIN_NAME = "name";
    public static final String GENIE_ROUTING_DOMAIN_NAME = "name";
    public static final String GENIE_CONSUMER_CONTRACT_TARGET = "target";
    public static final String GENIE_PROVIDER_CONTRACT_TARGET = "target";
    public static final String GENIE_CLASSIFIER_REF_TARGET = "target";
    public static final String GENIE_EPG_TO_NETWORK_DOMAIN_TARGET = "target";
    public static final String GENIE_SUBNETS_TO_NETWORK_DOMAIN_TARGET = "target";
    public static final String GENIE_FLOOD_DOMAIN_TO_NETWORK_DOMAIN_TARGET = "target";
    public static final String GENIE_BRIDGE_DOMAIN_TO_NETWORK_DOMAIN_TARGET = "target";

    public static final String GENIE_RULE_ORDER = "order";

    public static final String GENIE_CLASSIFIER_CONNECTION_TRACKING = "connectionTracking";
    public static final String GENIE_CLASSIFIER_DIRECTION = "direction";
    public static final String GENIE_CLASSIFIER_ARP_OPC = "arpOpc";
    public static final String GENIE_CLASSIFIER_DFROM_PORT = "dFromPort";
    public static final String GENIE_CLASSIFIER_DTO_PORT = "dToPort";
    public static final String GENIE_CLASSIFIER_ETHERT = "etherT";
    public static final String GENIE_CLASSIFIER_PROT = "prot";
    public static final String GENIE_CLASSIFIER_SFROM_PORT = "sFromPort";
    public static final String GENIE_CLASSIFIER_STO_PORT = "sToPort";

    private static AgentOvsMit mit;
    private static MitLib lib;

    private static ConcurrentMap<String, Integer> odlKeys;
    private static ConcurrentMap<String, Integer> genieKeys;

    private static ConcurrentMap<String, String> odlToGenieMap;
    private static ConcurrentMap<String, String> genieToOdlMap;

    public static void setMit(AgentOvsMit currentMit) {
        mit = currentMit;

    }

    public static ConcurrentMap<String, Integer> getOdlKeys() {
        return odlKeys;
    }

    public static ConcurrentMap<String, Integer> getGenieKeys() {
        return genieKeys;
    }

    public static void init() {

        odlKeys = new ConcurrentHashMap<String, Integer>();
        genieKeys = new ConcurrentHashMap<String, Integer>();

        odlKeys.put(ENDPOINT_RN, 2);
        odlKeys.put(ENDPOINT_L3_RN, 2);
        odlKeys.put(TENANT_RN, 1);
        odlKeys.put(L3_CONTEXT_RN, 1);
        odlKeys.put(L2_BRIDGE_DOMAIN_RN, 1);
        odlKeys.put(L2_FLOOD_DOMAIN_RN, 1);
        odlKeys.put(SUBNET_RN, 1);
        odlKeys.put(EPG_RN, 1);
        odlKeys.put(CLASSIFIER_INSTANCE_RN, 1);
        odlKeys.put(CONTRACT_RN, 1);
        odlKeys.put(SUBJECT_RN, 1);
        odlKeys.put(RULE_RN, 1);

        genieKeys.put(GENIE_ENDPOINT_RN, 2);
        genieKeys.put(GENIE_ENDPOINT_L3_RN, 2);
        genieKeys.put(GENIE_TENANT_RN, 1);
        genieKeys.put(GENIE_ROUTING_DOMAIN_RN, 1);
        genieKeys.put(GENIE_BRIDGE_DOMAIN_RN, 1);
        genieKeys.put(GENIE_FLOOD_DOMAIN_RN, 1);
        genieKeys.put(GENIE_SUBNET_RN, 1);
        genieKeys.put(GENIE_SUBNETS_RN, 1);
        genieKeys.put(GENIE_EPG_RN, 1);
        genieKeys.put(GENIE_CLASSIFIER_RN, 1);
        genieKeys.put(GENIE_CONTRACT_RN, 1);
        genieKeys.put(GENIE_SUBJECT_RN, 1);
        genieKeys.put(GENIE_RULE_RN, 1);

        odlToGenieMap = new ConcurrentHashMap<String, String>();
        odlToGenieMap.put(ENDPOINTS_RN, "");
        odlToGenieMap.put(ENDPOINT_RN, GENIE_EPR_L2_ROOT + PolicyUri.POLICY_URI_SEP + GENIE_ENDPOINT_RN);
        odlToGenieMap.put(ENDPOINT_L3_RN, GENIE_EPR_L3_ROOT + PolicyUri.POLICY_URI_SEP + GENIE_ENDPOINT_L3_RN);
        odlToGenieMap.put(TENANTS_RN, GENIE_TENANTS_RN);
        odlToGenieMap.put(TENANT_RN, GENIE_TENANT_RN);
        odlToGenieMap.put(EPG_RN, GENIE_EPG_RN);
        odlToGenieMap.put(CONTRACT_RN, GENIE_CONTRACT_RN);
        odlToGenieMap.put(SUBJECT_RN, GENIE_SUBJECT_RN);
        odlToGenieMap.put(RULE_RN, GENIE_RULE_RN);
        odlToGenieMap.put(CLAUSE_RN, "");
        odlToGenieMap.put(CLASSIFIER_REF_RN, GENIE_CLASSIFIER_RN);
        odlToGenieMap.put(L2_FLOOD_DOMAIN_RN, GENIE_FLOOD_DOMAIN_RN);
        odlToGenieMap.put(L2_BRIDGE_DOMAIN_RN, GENIE_BRIDGE_DOMAIN_RN);
        odlToGenieMap.put(SUBNET_RN, GENIE_SUBNETS_RN + PolicyUri.POLICY_URI_SEP + GENIE_SUBNET_NAME_DEFAULT
                + PolicyUri.POLICY_URI_SEP + GENIE_SUBNET_RN);
        odlToGenieMap.put(L3_CONTEXT_RN, GENIE_ROUTING_DOMAIN_RN);

        genieToOdlMap = new ConcurrentHashMap<String, String>();
        genieToOdlMap.put(GENIE_ENDPOINT_RN, ENDPOINT_RN);
        genieToOdlMap.put(GENIE_ENDPOINT_L3_RN, ENDPOINT_L3_RN);
        genieToOdlMap.put(GENIE_TENANTS_RN, TENANTS_RN);
        genieToOdlMap.put(GENIE_TENANT_RN, TENANT_RN);
        genieToOdlMap.put(GENIE_EPG_RN, EPG_RN);
        genieToOdlMap.put(GENIE_CONTRACT_RN, CONTRACT_RN);
        genieToOdlMap.put(GENIE_SUBJECT_RN, SUBJECT_RN);
        genieToOdlMap.put(GENIE_RULE_RN, RULE_RN);
        genieToOdlMap.put(GENIE_CLASSIFIER_RN, CLASSIFIER_REF_RN);
        genieToOdlMap.put(GENIE_FLOOD_DOMAIN_RN, L2_FLOOD_DOMAIN_RN);
        genieToOdlMap.put(GENIE_BRIDGE_DOMAIN_RN, L2_BRIDGE_DOMAIN_RN);
        genieToOdlMap.put(GENIE_SUBNETS_RN, "");
        genieToOdlMap.put(GENIE_SUBNET_RN, SUBNET_RN);
        genieToOdlMap.put(GENIE_ROUTING_DOMAIN_RN, L3_CONTEXT_RN);
        genieToOdlMap.put(GENIE_EPR_L2_ROOT, ENDPOINTS_RN);
        genieToOdlMap.put(GENIE_EPR_L3_ROOT, ENDPOINTS_RN);

    }

    private static BigInteger intToBigInt(int i) {
        return new BigInteger(Integer.toString(i));
    }

    public static void setOpflexLib(MitLib opflexLib) {
        lib = opflexLib;
    }

    public static PolicyUri parseUri(String uri) {
        PolicyUri u = new PolicyUri(uri);
        if (u.valid())
            return u;
        return null;
    }

    /*
     * Until I clean this up, this is going to accept the Genie URI. The format
     * for these URIs is:
     * 
     * /PolicyUniverse/PolicySpace/[name]
     */
    public static String getTenantFromUri(String uri) {
        PolicyUri genieUri = odlUriToGenieUri(new PolicyUri(uri));
        if (genieUri.totalElements() >= 3)
            return genieUri.getElement(2);
        return null;
    }

    /*
     * Until I clean this up, this is going to be the Genie URI. The format for
     * these URIs is:
     * 
     * /PolicyUniverse/PolicySpace/[name]/GbpEpGroup/[name]
     * 
     * Where [name] is the tenant and ID for the EPG
     */
    public static String getEndpointGroupFromUri(String uri) {
        PolicyUri genieUri = odlUriToGenieUri(new PolicyUri(uri));
        PolicyUri pu = new PolicyUri(genieUri.toString());
        if (!pu.contains(GENIE_EPG_RN)) {
            return null;
        }
        int epgIdx = pu.whichElement(GENIE_EPG_RN);
        /*
         * subtract 1 to compare between total elements and an array index; it's
         * an EPG URI if it's the second to the last element
         */
        if (epgIdx == pu.totalElements() - 1 - 1) {
            return pu.getElement(epgIdx + 1);
        }
        return null;
    }

    public static String getContextFromUri(String uri) {
        PolicyUri genieUri = odlUriToGenieUri(new PolicyUri(uri));
        PolicyUri pu = new PolicyUri(genieUri.toString());

        if (!pu.contains(GENIE_EPG_RN)) {
            return null;
        }
        int epgIdx = pu.whichElement(GENIE_EPG_RN);
        /*
         * subtract 1 to compare between total elements and an array index; it's
         * an EPG URI if it's the second to the last element
         */
        if (epgIdx == pu.totalElements() - 1 - 1) {
            return pu.getElement(epgIdx + 2);
        }
        return null;
    }

    public static String createEpgUri(String tenantId, String epgId) {
        return GENIE_TENANT_PREFIX + tenantId + PolicyUri.POLICY_URI_SEP + GENIE_EPG_RN + PolicyUri.POLICY_URI_SEP
                + epgId;
    }

    public static boolean hasEpg(String uri) {
        return new PolicyUri(uri).contains(GENIE_EPG_RN);
    }

    public static boolean isEpgUri(String uri) {
        PolicyUri pu = new PolicyUri(uri);
        if (!pu.contains(GENIE_EPG_RN)) {
            return false;
        }
        int epgIdx = pu.whichElement(GENIE_EPG_RN);
        /*
         * subtract 1 to compare between total elements and an array index; it's
         * an EPG URI if it's the second to the last element
         */
        return (epgIdx == pu.totalElements() - 1 - 1);
    }

    /**
     * Check to see if the given URI is already in genie format
     *
     * @param uri
     * @return
     */
    public static boolean isGenieUri(Uri uri) {
        PolicyUri puri = new PolicyUri(uri.toString());
        List<String> genieRoot = Arrays.asList("PolicyUniverse", "EprL2Universe", "EprL3Universe");

        if (genieRoot.contains(puri.getElement(0)))
            return true;

        return false;

    }

    /**
     * Check to see if the given URI is already in ODL format
     *
     * @param uri
     * @return
     */
    public static boolean isOdlUri(Uri uri) {
        PolicyUri puri = new PolicyUri(uri.toString());
        List<String> odlRoot = Arrays.asList("tenants", "endpoints");

        if (odlRoot.contains(puri.getElement(0)))
            return true;

        return false;
    }

    /**
     * Iterator for URIs. Provides iteration, along with identification of key
     * values needed for URI translation.
     *
     * @author tbachman
     */
    public static class UriIterator implements Iterator<String> {

        private final PolicyUri uri;
        private int index;
        private int keyCount;
        private final Map<String, Integer> keyMap;

        public UriIterator(PolicyUri uri, ConcurrentMap<String, Integer> keyMap) {
            this.uri = uri;
            this.index = 0;
            this.keyCount = 0;
            this.keyMap = keyMap;
        }

        public boolean isKey() {
            if (keyCount > 0)
                return true;
            return false;
        }

        public String getElement() {
            if (this.index >= this.uri.totalElements())
                return null;
            return this.uri.getElement(index);
        }

        @Override
        public boolean hasNext() {
            if (this.index < this.uri.totalElements())
                return true;

            return false;
        }

        @Override
        public String next() {
            /*
             * Check to see if the subsequent elements are keys, and if so, set
             * the number of keys
             */
            if (keyCount > 0) {
                keyCount -= 1;
            }
            if (keyCount == 0 && keyMap.containsKey(this.getElement())) {
                keyCount = keyMap.get(this.getElement());
            }

            this.index += 1;

            if (this.index >= this.uri.totalElements())
                return null;

            return this.uri.getElement(index);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Convert ODL URI to a Genie URI. The ODL names are unique, so we are able
     * to provide a conversion based solely on name.
     * This only maps URIs from the following roots:
     * /endpoint /policy
     *
     * @param odlUri
     * @return
     */
    public static PolicyUri odlUriToGenieUri(PolicyUri odlUri) {

        PolicyUri genieUri = new PolicyUri();

        /*
         * If it's already a genie URI, do nothing
         */
        if (isGenieUri(new Uri(odlUri.toString()))) {
            return odlUri;
        }

        UriIterator it = new UriIterator(odlUri, odlKeys);
        while (it.getElement() != null) {

            /*
             * Get the converted element, then make the following checks:
             * 
             * o element is key - push the element directly onto the stack
             * without translation
             * o no corresponding genie element - return --
             * we're done
             * o genie element, but result is null - don't push
             * anything on the stack; continue
             * o regular element - convert the
             * element and push it on the stack
             */
            String element = it.getElement();
            String genieElement = odlToGenieMap.get(element);

            if (it.isKey()) {
                genieUri.push(it.getElement());
                it.next();
                continue;
            } else if (genieElement == null)
                break;
            else if (genieElement.equals("")) {
                it.next();
                continue;
            }

            genieUri.push(genieElement);
            it.next();
        }

        return genieUri;
    }

    public static PolicyUri genieUriToOdlUri(PolicyUri genieUri) {

        PolicyUri odlUri = new PolicyUri();

        /*
         * If it's already a genie URI, do nothing
         */
        if (isOdlUri(new Uri(genieUri.toString()))) {
            return genieUri;
        }

        UriIterator it = new UriIterator(genieUri, genieKeys);
        while (it.getElement() != null) {

            /*
             * Get the converted element, then make the following checks:
             * 
             * o element is key - push the element directly onto the stack
             * without translation
             * o no corresponding genie element - return --
             * we're done
             * o genie element, but result is null - don't push
             * anything on the stack; continue
             * o regular element - convert the
             * element and push it on the stack
             */
            String element = it.getElement();
            String odlElement = genieToOdlMap.get(element);

            if (it.isKey()) {
                odlUri.push(it.getElement());
                it.next();
                continue;
            } else if (odlElement == null)
                break;
            else if (odlElement.equals("")) {
                it.next();
                continue;
            }

            odlUri.push(odlElement);
            it.next();
        }

        return odlUri;

    }

    private static void setParentFields(PolicyUri current, PolicyObjectInstance poi, String parentRelation,
            boolean hasId) {
        PolicyUri uriCopy = new PolicyUri(current);

        // Pop off the subject name and class to get to parent
        uriCopy.pop();
        uriCopy.pop();
        PolicyUri parent = new PolicyUri(uriCopy);

        // remove parent ID to get to parent subject
        if (hasId) {
            uriCopy.pop();
        }
        String parentSubject = uriCopy.pop();
        poi.setParent(parent.getUri());
        poi.setParentSubject(parentSubject);
        poi.setParentRelation(parentRelation);
    }

    public static List<ManagedObject> getSubjectMo(PolicyUri current, Subject s, RuleGroup rg, IndexedTenant it) {
        if (s == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);

        String prefix = convertedUri.toString();

        ManagedObject mo = new ManagedObject();
        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        List<Uri> childrenUris = new ArrayList<Uri>();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_SUBJECT_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_SUBJECT_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_SUBJECT_NAME) && s.getName() != null) {
                poi.setString(ppi.getPropId(), s.getName().getValue());
            } else if (ppi.getPropName().equals(GENIE_RULE_RN) && s.getRule() != null) {
                /*
                 * Each subject has a set of resolved rules. Add those as
                 * children.
                 */

                for (Rule r : rg.getRules()) {
                    PolicyUri uri = new PolicyUri(prefix);
                    uri.push(GENIE_RULE_RN);
                    uri.push(r.getName().getValue());
                    childrenUris.add(uri.getUri());
                    poi.addChild(uri.getUri()); // TODO: remove?
                    mol.addAll(MessageUtils.getRuleMo(uri, r, rg, it));
                }
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setChildren(childrenUris);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_SUBJECT_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;
    }

    public static List<ManagedObject> getRuleMo(PolicyUri current, Rule r, RuleGroup rg, IndexedTenant it) {
        if (r == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);

        String prefix = convertedUri.toString();

        ManagedObject mo = new ManagedObject();
        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        List<Uri> childrenUris = new ArrayList<Uri>();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_RULE_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_RULE_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_RULE_NAME) && r.getName() != null) {
                poi.setString(ppi.getPropId(), r.getName().getValue());
            } else if (ppi.getPropName().equals(GENIE_RULE_ORDER) && r.getOrder() != null) {
                poi.setUint64(ppi.getPropId(), intToBigInt(r.getOrder().intValue()));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_REF_RN) && r.getClassifierRef() != null) {
                for (ClassifierRef cr : r.getClassifierRef()) {
                    ClassifierInstance ci = it.getClassifier(cr.getName());
                    if (ci != null) {
                        PolicyUri uri = new PolicyUri(prefix);
                        uri.push(GENIE_CLASSIFIER_REF_RN);
                        uri.push(cr.getName().getValue());
                        mol.addAll(MessageUtils.getClassifierRefMo(uri, ci, cr, rg, it));
                        childrenUris.add(uri.getUri());
                        poi.addChild(uri.getUri()); // TODO: remove?
                    }
                }
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setChildren(childrenUris);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_RULE_RN);
        mo.setUri(convertedUri.getUri());

        mol.add(mo);

        return mol;
    }

    /**
     * Convert a Contract to the equivalent Genie MO
     *
     * @param c
     * @return
     */
    public static List<ManagedObject> getContractAndSubMos(List<ManagedObject> cmol, PolicyUri current, Contract c,
            RuleGroup rg, IndexedTenant it) {
        if (c == null)
            return null;

        ManagedObject mo = new ManagedObject();
        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        List<Uri> childrenUris = new ArrayList<Uri>();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        String prefix = convertedUri.toString();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_CONTRACT_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_CONTRACT_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        if (c.getSubject() == null)
            LOG.warn("subject is NULL");
        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_CONTRACT_NAME) && c.getId() != null) {
                poi.setString(ppi.getPropId(), c.getId().getValue());
            } else if (ppi.getPropName().equals(GENIE_SUBJECT_RN) && c.getSubject() != null) {

                LOG.warn("related subject is {}", rg.getRelatedSubject());
                /*
                 * Get the subject in scope for this contract (NB: there could
                 * be more than one -- we get multiple subjects for a single
                 * contract in multiple RuleGroup objects).
                 */
                SubjectName sn = rg.getRelatedSubject();
                if (sn == null)
                    continue;

                /* Find the related subject object */
                for (Subject s : c.getSubject()) {
                    LOG.warn("subject is {}", s.getName());

                    if (s.getName().getValue().equals(sn.getValue())) {

                        PolicyUri uri = new PolicyUri(prefix);
                        uri.push(GENIE_SUBJECT_RN);
                        uri.push(s.getName().getValue());

                        mol.addAll(MessageUtils.getSubjectMo(uri, s, rg, it));
                        childrenUris.add(uri.getUri());
                        poi.addChild(uri.getUri()); // TODO: needed?
                        break;
                    }
                }
            }
        }
        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setChildren(childrenUris);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_CONTRACT_RN);
        mo.setUri(convertedUri.getUri());
        cmol.add(mo);

        return mol;
    }

    public static ManagedObject getConsumerNamedSelectorMo(PolicyUri current, Contract c) {
        if (c == null)
            return null;

        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_CONSUMER_CONTRACT_REF_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_CONSUMER_CONTRACT_REF_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_CONSUMER_CONTRACT_TARGET)) {
                PolicyUri uri = new PolicyUri(convertedUri);
                // Go up to the EPG
                uri.pop();
                uri.pop();
                uri.push(GENIE_CONSUMER_CONTRACT_REF_RN);
                uri.push(c.getId().getValue());
                String newUri = odlUriToGenieUri(uri).toString();
                PolicyReference pr = new PolicyReference(pci.getClassId(), new Uri(newUri));
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_CONSUMER_CONTRACT_REF_RN);
        mo.setUri(convertedUri.getUri());

        return mo;
    }

    public static ManagedObject getProviderNamedSelectorMo(PolicyUri current, Contract c) {
        if (c == null)
            return null;
        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_PROVIDER_CONTRACT_REF_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_PROVIDER_CONTRACT_REF_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_PROVIDER_CONTRACT_TARGET)) {
                PolicyUri uri = new PolicyUri(convertedUri);
                // Go up to the EPG
                uri.pop();
                uri.pop();
                uri.push(GENIE_CONTRACT_RN);
                uri.push(c.getId().getValue());
                String newUri = odlUriToGenieUri(uri).toString();
                PolicyReference pr = new PolicyReference(pci.getClassId(), new Uri(newUri));
                // TODO: should we chase the contracts?
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_PROVIDER_CONTRACT_REF_RN);
        mo.setUri(convertedUri.getUri());

        return mo;

    }

    private static List<BigInteger> getParamList(HashMap<String, List<BigInteger>> hm, String type) {
        List<BigInteger> pvl = hm.get(type);
        if (pvl == null) {
            pvl = new ArrayList<>();
            hm.put(type, pvl);
        }
        return pvl;
    }

    private static final Integer TCP_PROTO = 6;
    private static final Integer UDP_PROTO = 17;

    /**
     * Build up a set of possible parameter values using the classifier
     * instance.
     *
     * @param ci
     * @param cr
     * @return
     */
    private static Map<String, List<BigInteger>> buildParameterValues(ClassifierInstance ci, ClassifierRef cr) {
        HashMap<String, List<BigInteger>> pmap = new HashMap<>();
        List<BigInteger> pvl = null;

        /*
         * Create the map of classifier types/values
         */
        for (ParameterValue pv : ci.getParameterValue()) {
            /*
             * The parameter-value name tells us the type of classifier
             * involved: "type": EtherType/L2 "proto": IP/L3
             * "sourceport"/"destport": TCP/UDP/L4
             */
            if (pv.getName().getValue().equals("type")) {
                if (pv.getIntValue() != null) {
                    switch (pv.getIntValue().intValue()) {
                        case 0x0806:
                        case 0x8906:
                        case 0x0800:
                        case 0x86DD:
                        case 0x88E5:
                        case 0x8847:
                        case 0x22F3:

                            pvl = getParamList(pmap, GENIE_CLASSIFIER_ETHERT);
                            pvl.add(intToBigInt(pv.getIntValue().intValue()));
                            break;

                        default:
                            break;
                    }
                } else if (pv.getStringValue() != null) {
                    if (pv.getStringValue().equals("TCP")) {
                        pvl = getParamList(pmap, GENIE_CLASSIFIER_PROT);
                        pvl.add(intToBigInt(TCP_PROTO.intValue()));
                    } else if (pv.getStringValue().equals("UDP")) {
                        pvl = getParamList(pmap, GENIE_CLASSIFIER_PROT);
                        pvl.add(intToBigInt(UDP_PROTO.intValue()));
                    }
                }
            }
            if (pv.getName().getValue().equals("proto")) {
                pvl = getParamList(pmap, GENIE_CLASSIFIER_ARP_OPC);
                pvl.add(intToBigInt(pv.getIntValue().intValue()));

            }
            if (pv.getName().getValue().equals("sourceport")) {
                if (cr.getDirection().equals(Direction.In)) {
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_STO_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                } else if (cr.getDirection().equals(Direction.Out)) {
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_SFROM_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                } else {
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_STO_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_SFROM_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                }
            }
            if (pv.getName().getValue().equals("destport")) {
                if (cr.getDirection().equals(Direction.In)) {
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_DTO_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                } else if (cr.getDirection().equals(Direction.Out)) {
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_DFROM_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                } else {
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_DTO_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));
                    pvl = getParamList(pmap, GENIE_CLASSIFIER_DFROM_PORT);
                    pvl.add(intToBigInt(pv.getIntValue().intValue()));

                }
            }
        }

        return pmap;

    }

    public static List<ManagedObject> getClassifierRefMo(PolicyUri current, ClassifierInstance ci, ClassifierRef cr,
            RuleGroup rg, IndexedTenant it) {

        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_CLASSIFIER_REF_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_CLASSIFIER_REF_RN, false);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_CLASSIFIER_REF_TARGET)) {

                PolicyUri uri = new PolicyUri(GENIE_POLICY_ROOT);
                uri.push(GENIE_TENANT_RN);
                uri.push(rg.getContractTenant().getId().getValue());
                uri.push(GENIE_CLASSIFIER_RN);
                uri.push(ci.getName().getValue());
                ManagedObject clMo = getClassifierInstanceMo(uri, ci, cr, rg, it);
                if (clMo != null) {
                    mol.add(clMo);
                }

                PolicyReference pr = new PolicyReference(pci.getClassId(), uri.getUri());
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_CLASSIFIER_REF_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;
    }

    /**
     * Create the Genie Classifier Instance MO. We need to use fields from both
     * the ClassifierRef and the ClassifierInstance in the ODL model.
     *
     * @param current
     * @param ci
     * @param cr
     * @param rg
     * @param it
     * @return
     */
    public static ManagedObject getClassifierInstanceMo(PolicyUri current, ClassifierInstance ci, ClassifierRef cr,
            RuleGroup rg, IndexedTenant it) {
        if (ci == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);

        if (ci.getParameterValue() == null)
            return null;

        Map<String, List<BigInteger>> pmap = buildParameterValues(ci, cr);

        ManagedObject mo = new ManagedObject();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_CLASSIFIER_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_CLASSIFIER_RN, true);

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        String prefix = convertedUri.toString();

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_CLASSIFIER_NAME) && cr.getName() != null) {
                poi.setString(ppi.getPropId(), cr.getName().getValue());
                Uri child = new Uri(prefix + GENIE_CLASSIFIER_NAME + cr.getName().getValue());
                poi.setUri(child);
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_DIRECTION) && cr.getDirection() != null) {
                // initialize with bogus values for placeholders, then replace
                // with real ones
                List<Integer> odl2genie = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
                odl2genie.set(0, 1);
                odl2genie.set(1, 2);
                odl2genie.set(2, 0);

                /*
                 * The direction enums are different between the ODL and Genie
                 * models:
                 * 
                 * Value: | ODL | Genie --------------+-----+--------
                 * bidirectional | 2 | 0 in | 0 | 1 out | 1 | 2
                 */
                Integer genieDirection = odl2genie.get(cr.getDirection().getIntValue());
                poi.setUint64(ppi.getPropId(), new BigInteger(genieDirection.toString()));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_CONNECTION_TRACKING)
                    && cr.getConnectionTracking() != null) {
                poi.setUint64(ppi.getPropId(), intToBigInt(cr.getConnectionTracking().getIntValue()));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_ARP_OPC) && pmap.get(GENIE_CLASSIFIER_ARP_OPC) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_ARP_OPC).get(0));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_ETHERT) && pmap.get(GENIE_CLASSIFIER_ETHERT) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_ETHERT).get(0));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_DFROM_PORT)
                    && pmap.get(GENIE_CLASSIFIER_DFROM_PORT) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_DFROM_PORT).get(0));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_DTO_PORT)
                    && pmap.get(GENIE_CLASSIFIER_DTO_PORT) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_DTO_PORT).get(0));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_SFROM_PORT)
                    && pmap.get(GENIE_CLASSIFIER_SFROM_PORT) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_SFROM_PORT).get(0));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_STO_PORT)
                    && pmap.get(GENIE_CLASSIFIER_STO_PORT) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_STO_PORT).get(0));
            } else if (ppi.getPropName().equals(GENIE_CLASSIFIER_PROT) && pmap.get(GENIE_CLASSIFIER_PROT) != null) {
                poi.setUint64(ppi.getPropId(), pmap.get(GENIE_CLASSIFIER_PROT).get(0));
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_CLASSIFIER_RN);
        mo.setUri(convertedUri.getUri());

        return mo;
    }

    public static class Ipv4PlusSubnet {

        private String prefix;
        private final String mask;

        public Ipv4PlusSubnet(String ipAndMask) {
            String[] parts = ipAndMask.split("/");
            this.mask = parts[1];
            this.prefix = "";

            int ip = 0;
            int index = 0;
            for (String s : parts[0].split("\\.")) {
                ip |= (Integer.parseInt(s) & 0xff) << (24 - 8 * index);
                index += 1;
            }

            int msk = -1 << (32 - Integer.parseInt(parts[1]));
            int sub = ip & msk;
            for (int i = 0; i < 3; i++) {
                this.prefix = this.prefix + String.valueOf((sub >> (24 - 8 * i)) & 0xff) + ".";
            }
            this.prefix = this.prefix + String.valueOf(sub & 0xff);
        }

        public String getPrefixAsString() {
            return this.prefix;
        }

        public String getMaskAsString() {
            return this.mask;
        }

        public int getMaskAsInt() {
            return Integer.parseInt(this.mask);
        }

        public BigInteger getMaskAsBigInt() {
            return new BigInteger(this.mask);
        }

    }

    public static ManagedObject getSubnetMo(PolicyUri current, Subnet s, Tenant t) {

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_SUBNET_RN);
        if (pci == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_SUBNET_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        // convert this once - we'll use it below
        Ipv4PlusSubnet ipv4 = new Ipv4PlusSubnet(s.getIpPrefix().getIpv4Prefix().getValue());

        for (PolicyPropertyInfo ppi : ppil) {

            // use the subnet ID for the subnets (plural) ID
            if (ppi.getPropName().equals(GENIE_SUBNET_NAME) && s.getId() != null) {
                poi.setString(ppi.getPropId(), GENIE_SUBNET_NAME_DEFAULT);
            }
            if (ppi.getPropName().equals(GENIE_SUBNET_ADDRESS) && s.getIpPrefix() != null) {
                /*
                 * We need to strip off the subnet delimiter
                 */
                poi.setString(ppi.getPropId(), ipv4.getPrefixAsString());
            }
            if (ppi.getPropName().equals(GENIE_SUBNET_PREFIX_LEN) && s.getIpPrefix() != null) {
                poi.setUint64(ppi.getPropId(), ipv4.getMaskAsBigInt());
            }
            if (ppi.getPropName().equals(GENIE_SUBNET_VIRTUAL_ROUTER_IP) && s.getVirtualRouterIp() != null) {
                poi.setString(ppi.getPropId(), s.getVirtualRouterIp().getIpv4Address().getValue());
            }
        }

        ManagedObject mo = new ManagedObject();
        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_SUBNET_RN);
        mo.setUri(convertedUri.getUri());

        return mo;
    }

    public static List<ManagedObject> getSubnetNetworkRefMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        if (ndid == null)
            return null;

        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_SUBNETS_TO_NETWORK_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_SUBNETS_TO_NETWORK_RN, false);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_SUBNETS_TO_NETWORK_DOMAIN_TARGET)) {

                PolicyUri uri = new PolicyUri(convertedUri);
                uri.pop();
                uri.pop();
                uri.push(GENIE_FLOOD_DOMAIN_RN);
                uri.push(ndid.getValue());
                mol = getL2FloodDomainMo(uri, ndid, t);
                if (mol == null) {
                    uri.pop();
                    uri.pop();
                    uri.push(GENIE_BRIDGE_DOMAIN_RN);
                    uri.push(ndid.getValue());
                    mol = getL2BridgeDomainMo(uri, ndid, t);
                }
                if (mol == null) {
                    uri.pop();
                    uri.pop();
                    uri.push(GENIE_ROUTING_DOMAIN_RN);
                    uri.push(ndid.getValue());
                    mol = new ArrayList<ManagedObject>();
                    ManagedObject l3cmo = getL3ContextMo(uri, ndid, t);
                    if (l3cmo != null) {
                        mol.add(l3cmo);
                    }
                }
                /*
                 * We default to this being a routing domain reference if the
                 * actual reference can't be resolved.
                 */
                PolicyReference pr = new PolicyReference(pci.getClassId(), uri.getUri());
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_SUBNETS_TO_NETWORK_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;
    }

    public static List<ManagedObject> getSubnetsMo(PolicyUri current, NetworkDomainId nid, Tenant t) {
        if (nid == null)
            return null;
        List<ManagedObject> mol = new ArrayList<ManagedObject>();

        Subnet subnetMatch = null;
        List<Subnet> sl = t.getSubnet();
        if (sl != null) {
            for (Subnet s : sl) {
                if (s.getId().getValue().equals(nid.getValue())) {
                    subnetMatch = s;
                    break;
                }
            }
        }
        if (subnetMatch == null)
            return null;

        ManagedObject mo = new ManagedObject();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_SUBNETS_RN);
        if (pci == null)
            return null;
        List<Uri> childrenUris = new ArrayList<Uri>();

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);
        String prefix = convertedUri.toString();

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_SUBNETS_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            // use the subnet ID for the subnets (plural) ID
            if (ppi.getPropName().equals(GENIE_SUBNETS_NAME) && subnetMatch.getId() != null) {
                poi.setString(ppi.getPropId(), subnetMatch.getId().getValue());
            }
            if (ppi.getPropName().equals(GENIE_SUBNET_RN) && subnetMatch.getId() != null) {
                PolicyUri child = new PolicyUri(prefix);
                child.push(GENIE_SUBNET_RN);
                child.push(GENIE_SUBNET_NAME_DEFAULT);
                ManagedObject snetMo = getSubnetMo(child, subnetMatch, t);
                if (snetMo != null) {
                    mol.add(snetMo);
                }
                childrenUris.add(child.getUri());
            }
            if (ppi.getPropName().equals(GENIE_SUBNETS_TO_NETWORK_RN) && subnetMatch.getParent() != null) {
                PolicyUri child = new PolicyUri(prefix);
                child.push(GENIE_SUBNETS_TO_NETWORK_RN);
                mol = getSubnetNetworkRefMo(child, subnetMatch.getParent(), t);
                childrenUris.add(child.getUri());
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setChildren(childrenUris);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_SUBNETS_RN);
        mo.setUri(convertedUri.getUri());

        mol.add(mo);

        return mol;
    }

    public static List<ManagedObject> getL2FloodDomainRefMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        if (ndid == null)
            return null;
        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_FLOOD_DOMAIN_TO_NETWORK_RN);
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_FLOOD_DOMAIN_TO_NETWORK_RN, false);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_EPG_TO_NETWORK_DOMAIN_TARGET)) {

                /*
                 * We have to move back to the tenant, since all of the
                 * references are relative to the tenant
                 */
                PolicyUri uri = new PolicyUri(GENIE_POLICY_ROOT);
                uri.push(GENIE_TENANT_RN);
                uri.push(t.getId().getValue());
                uri.push(GENIE_BRIDGE_DOMAIN_RN);
                uri.push(ndid.getValue());

                /*
                 * Go chase the network domain references. Look for the
                 * reference in the bridge domain list.
                 */
                mol = getL2BridgeDomainMo(uri, ndid, t);
                if (mol == null) {
                    uri.pop();
                    uri.pop();
                    uri.push(GENIE_ROUTING_DOMAIN_RN);
                    uri.push(ndid.getValue());
                    mol = new ArrayList<ManagedObject>();
                    ManagedObject l3cmo = getL3ContextMo(uri, ndid, t);
                    if (l3cmo != null) {
                        mol.add(l3cmo);
                    }
                }
                /*
                 * We default to this being a routing domain reference if the
                 * actual reference can't be resolved.
                 */
                PolicyReference pr = new PolicyReference(pci.getClassId(), uri.getUri());
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_FLOOD_DOMAIN_TO_NETWORK_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;
    }

    public static List<ManagedObject> getL2FloodDomainMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        if (ndid == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);
        String prefix = convertedUri.toString();
        L2FloodDomain l2fdMatch = null;

        List<L2FloodDomain> l2fdl = t.getL2FloodDomain();
        if (l2fdl != null) {
            for (L2FloodDomain l2fd : l2fdl) {
                if (l2fd.getId().getValue().equals(ndid.getValue())) {
                    l2fdMatch = l2fd;
                    break;
                }
            }
        }

        if (l2fdMatch == null)
            return null;

        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        List<Uri> childrenUris = new ArrayList<Uri>();
        ManagedObject mo = new ManagedObject();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_FLOOD_DOMAIN_RN);
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_FLOOD_DOMAIN_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_FLOOD_DOMAIN_NAME)) {
                poi.setString(ppi.getPropId(), l2fdMatch.getId().getValue());
            }
            if (ppi.getPropName().equals(GENIE_FLOOD_DOMAIN_TO_NETWORK_RN) && l2fdMatch.getParent() != null) {
                /*
                 * Add as a child, not a property, and get the child
                 */
                PolicyUri child = new PolicyUri(prefix);
                child.push(GENIE_FLOOD_DOMAIN_TO_NETWORK_RN);
                mol.addAll(getL2FloodDomainRefMo(child, l2fdMatch.getParent(), t));
                childrenUris.add(child.getUri());
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setChildren(childrenUris);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_FLOOD_DOMAIN_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;
    }

    public static List<ManagedObject> getL2BridgeDomainRefMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        if (ndid == null)
            return null;

        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_BRIDGE_DOMAIN_TO_NETWORK_RN);
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_BRIDGE_DOMAIN_TO_NETWORK_RN, false);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_BRIDGE_DOMAIN_TO_NETWORK_DOMAIN_TARGET)) {

                /*
                 * We have to move back to the tenant, since all of the
                 * references are relative to the tenant
                 */
                PolicyUri uri = new PolicyUri(GENIE_POLICY_ROOT);
                uri.push(GENIE_TENANT_RN);
                uri.push(t.getId().getValue());
                uri.push(GENIE_ROUTING_DOMAIN_RN);
                uri.push(ndid.getValue());
                mol = new ArrayList<ManagedObject>();
                ManagedObject l3cmo = getL3ContextMo(uri, ndid, t);
                if (l3cmo != null) {
                    mol.add(l3cmo);
                }

                /*
                 * We default to this being a routing domain reference if the
                 * actual reference can't be resolved.
                 */
                PolicyReference pr = new PolicyReference(pci.getClassId(), uri.getUri());
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_BRIDGE_DOMAIN_TO_NETWORK_RN);
        mo.setUri(convertedUri.getUri());

        mol.add(mo);

        return mol;
    }

    public static List<ManagedObject> getL2BridgeDomainMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        List<L2BridgeDomain> l2bdl = t.getL2BridgeDomain();
        if (ndid == null || l2bdl == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);
        String prefix = convertedUri.toString();

        L2BridgeDomain l2bdMatch = null;
        for (L2BridgeDomain l2bd : l2bdl) {
            if (l2bd.getId().getValue().equals(ndid.getValue())) {
                l2bdMatch = l2bd;
                break;
            }
        }

        if (l2bdMatch == null)
            return null;

        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        List<Uri> childrenUris = new ArrayList<Uri>();
        ManagedObject mo = new ManagedObject();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_BRIDGE_DOMAIN_RN);
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_BRIDGE_DOMAIN_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_BRIDGE_DOMAIN_NAME)) {
                poi.setString(ppi.getPropId(), l2bdMatch.getId().getValue());
            }
            if (ppi.getPropName().equals(GENIE_BRIDGE_DOMAIN_TO_NETWORK_RN) && l2bdMatch.getParent() != null) {
                /*
                 * Add as a child, not a property, and get the child
                 */
                PolicyUri child = new PolicyUri(prefix);
                child.push(GENIE_BRIDGE_DOMAIN_TO_NETWORK_RN);
                mol.addAll(getL2BridgeDomainRefMo(child, l2bdMatch.getParent(), t));
                childrenUris.add(child.getUri());
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setChildren(childrenUris);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_BRIDGE_DOMAIN_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;
    }

    public static ManagedObject getL3ContextMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        List<L3Context> l3cl = t.getL3Context();
        if (ndid == null || l3cl == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);

        L3Context l3cMatch = null;
        for (L3Context l3c : l3cl) {
            if (l3c.getId().getValue().equals(ndid.getValue())) {
                l3cMatch = l3c;
                break;
            }
        }

        if (l3cMatch == null)
            return null;

        ManagedObject mo = new ManagedObject();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_ROUTING_DOMAIN_RN);
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_ROUTING_DOMAIN_RN, true);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_ROUTING_DOMAIN_NAME)) {
                poi.setString(ppi.getPropId(), l3cMatch.getId().getValue());
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_ROUTING_DOMAIN_RN);
        mo.setUri(convertedUri.getUri());
        return mo;
    }

    /**
     * This is the equivalent of a network reference object in the Genie MIT. We
     * chase the reference to get any other objects in a network hierarchy.
     *
     * @param current
     * @param ndid
     * @param t
     * @return
     */
    public static List<ManagedObject> getNetwokDomainRefMo(PolicyUri current, NetworkDomainId ndid, Tenant t) {
        if (ndid == null)
            return null;
        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        ManagedObject mo = new ManagedObject();

        PolicyUri convertedUri = odlUriToGenieUri(current);

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_EPG_TO_NETWORK_DOMAIN_RN);
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_EPG_TO_NETWORK_DOMAIN_RN, false);
        poi.setUri(convertedUri.getUri());

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_EPG_TO_NETWORK_DOMAIN_TARGET)) {

                /*
                 * We have to move back to the tenant, since all of the
                 * references are relative to the tenant
                 */
                PolicyUri uri = new PolicyUri(GENIE_POLICY_ROOT);
                uri.push(GENIE_TENANT_RN);
                uri.push(t.getId().getValue());
                uri.push(GENIE_SUBNETS_RN);
                uri.push(ndid.getValue());

                /*
                 * Go chase the network domain references. Look first for the
                 * reference in the subnets list.
                 */
                mol = getSubnetsMo(uri, ndid, t);
                if (mol == null) {
                    uri.pop();
                    uri.pop();
                    uri.push(GENIE_FLOOD_DOMAIN_RN);
                    uri.push(ndid.getValue());
                    mol = getL2FloodDomainMo(uri, ndid, t);
                }
                if (mol == null) {
                    uri.pop();
                    uri.pop();
                    uri.push(GENIE_BRIDGE_DOMAIN_RN);
                    uri.push(ndid.getValue());
                    mol = getL2BridgeDomainMo(uri, ndid, t);
                }
                if (mol == null) {
                    uri.pop();
                    uri.pop();
                    uri.push(GENIE_ROUTING_DOMAIN_RN);
                    uri.push(ndid.getValue());
                    mol = new ArrayList<ManagedObject>();
                    ManagedObject l3cmo = getL3ContextMo(uri, ndid, t);
                    if (l3cmo != null) {
                        mol.add(l3cmo);
                    }
                }
                /*
                 * We default to this being a routing domain reference if the
                 * actual reference can't be resolved.
                 */
                PolicyReference pr = new PolicyReference(pci.getClassId(), uri.getUri());
                poi.setReference(ppi.getPropId(), pr);
            }
        }

        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_EPG_TO_NETWORK_DOMAIN_RN);
        mo.setUri(convertedUri.getUri());
        mol.add(mo);

        return mol;

    }

    public static Set<ManagedObject> getEndpointGroupMo(ManagedObject epgMo, PolicyUri current, EndpointGroup epg,
            RuleGroup rg) {
        if (epg == null)
            return null;

        // Convert to Genie URI
        PolicyUri convertedUri = odlUriToGenieUri(current);

        String prefix = convertedUri.toString();

        // Arrays for MOs that follow
        List<ManagedObject> mol = new ArrayList<ManagedObject>();
        List<Uri> childrenUris = new ArrayList<Uri>();

        /*
         * Build up the equivalent Genie object
         */
        PolicyClassInfo pci = mit.getClass(GENIE_EPG_RN);
        if (pci == null)
            return null;
        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());
        setParentFields(convertedUri, poi, GENIE_EPG_RN, true);

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        // Use the name to set the URI of this object
        poi.setUri(convertedUri.getUri());

        Contract c = rg.getRelatedContract();
        for (PolicyPropertyInfo ppi : ppil) {
            if (ppi.getPropName().equals(GENIE_ENDPOINT_GROUP_NAME) && epg.getId() != null) {
                poi.setString(ppi.getPropId(), epg.getId().getValue());
            }
            if (ppi.getPropName().equals(GENIE_INTRA_EPG_RN) && epg.getIntraGroupPolicy() != null) {
                poi.setUint64(ppi.getPropId(), intToBigInt(epg.getIntraGroupPolicy().getIntValue()));
            }

            // TODO: the following only maps named selectors. What about target
            // selectors?
            if (ppi.getPropName().equals(GENIE_CONSUMER_CONTRACT_REF_RN) && epg.getConsumerNamedSelector() != null) {
                // TODO: this does all the selectors -- should we just do those
                // that are in scope?
                for (ConsumerNamedSelector cns : epg.getConsumerNamedSelector()) {
                    for (ContractId cid : cns.getContract()) {
                        if (!cid.getValue().equals(c.getId().getValue())) {
                            PolicyUri child = new PolicyUri(prefix);
                            child.push(GENIE_CONSUMER_CONTRACT_REF_RN);
                            child.push(cns.getName().getValue());
                            ManagedObject conMo = getConsumerNamedSelectorMo(child, c);
                            if (conMo != null) {
                                mol.add(conMo);
                            }
                            childrenUris.add(child.getUri());
                        }
                    }
                }
            }

            // TODO: the following only maps named selectors. What about target
            // selectors?
            if (ppi.getPropName().equals(GENIE_PROVIDER_CONTRACT_REF_RN) && epg.getProviderNamedSelector() != null) {
                // TODO: this does all the selectors -- should we just do those
                // that are in scope?
                for (ProviderNamedSelector pns : epg.getProviderNamedSelector()) {
                    for (ContractId cid : pns.getContract()) {
                        if (cid.getValue().equals(c.getId().getValue())) {
                            PolicyUri child = new PolicyUri(prefix);
                            child.push(GENIE_PROVIDER_CONTRACT_REF_RN);
                            child.push(pns.getName().getValue());
                            ManagedObject provMo = getProviderNamedSelectorMo(child, c);
                            if (provMo != null) {
                                mol.add(provMo);
                            }
                            childrenUris.add(child.getUri());
                        }
                    }
                }
            }
            /*
             * Don't bother getting network references if we have them already
             */
            if (ppi.getPropName().equals(GENIE_EPG_TO_NETWORK_DOMAIN_RN) && epg.getNetworkDomain() != null) {
                PolicyUri child = new PolicyUri(prefix);
                child.push(GENIE_EPG_TO_NETWORK_DOMAIN_RN);
                mol.addAll(MessageUtils.getNetwokDomainRefMo(child, epg.getNetworkDomain(), rg.getContractTenant()));
                childrenUris.add(child.getUri());
            }

        }

        lib.serializeMoProperties(pci, poi, epgMo, mit);

        epgMo.setChildren(childrenUris);

        epgMo.setParent_uri(poi.getParent());
        epgMo.setParent_subject(poi.getParentSubject());
        epgMo.setParent_relation(poi.getParentRelation());
        epgMo.setSubject(GENIE_EPG_RN);
        epgMo.setUri(convertedUri.getUri());

        return Sets.newHashSet(mol);
    }

    /**
     * Deserialize the MO properties, convert them to objects that are used in
     * the ODL tree, and return them as a list of objects.
     *
     * @param mo
     * @return
     */
    public static EprOperation getEprOpFromEpMo(ManagedObject mo, int prr, String agentId) {
        MacAddress mac = null;
        EndpointGroupId epgid = null;
        L2BridgeDomainId l2bdid = null;
        L3ContextId l3cid = null;
        IpAddress ip = null;
        EprOperation op = null;
        TenantId tid = null;
        List<L3Address> l3al = new ArrayList<L3Address>();

        if (mo.getProperties() == null)
            return null;

        // Deserialize the MO properties
        PolicyObjectInstance poi = lib.deserializeMoProperties(mo, mit);
        if (poi == null)
            return null;

        PolicyClassInfo pci = mit.getClass(poi.getClassId());
        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        /*
         * We want to extract the properties we need to map to the corresponding
         * ODL MIT. We have to roll through the list of properties that were
         * present and map each of them independently. We know what type of
         * Endpoint we're mapping by the "subject" field of the MO.
         */
        // TODO: add support for vector values
        for (PolicyPropertyInfo ppi : ppil) {
            if (poi.isSet(ppi.getPropId(), ppi.getType(), ppi.getPropCardinality())) {
                switch (ppi.getPropName()) {
                    case GENIE_ENDPOINT_MAC:
                        mac = poi.getMacAddress(ppi.getPropId());
                        break;
                    case GENIE_ENDPOINT_EPG:
                        /*
                         * This must be a full URI of the EPG -- otherwise, it can't
                         * be uniquely resolved.
                         */
                        String epg = poi.getString(ppi.getPropId());
                        epgid = new EndpointGroupId(epg);
                        break;
                    case GENIE_ENDPOINT_CONTEXT:
                        /*
                         * It seems like this should be scoped by tenant as well,
                         * which means it would have to be a full URI. If that's the
                         * case, then the code below needs fixing.
                         */
                        if (mo.getSubject().equals(GENIE_ENDPOINT_RN)) {
                            l2bdid = new L2BridgeDomainId(poi.getString(ppi.getPropId()));
                        } else if (mo.getSubject().equals(GENIE_ENDPOINT_L3_RN)) {
                            l3cid = new L3ContextId(poi.getString(ppi.getPropId()));
                        }
                        break;
                    case GENIE_ENDPOINT_UUID:
                        String uuid = poi.getString(ppi.getPropId());
                        tid = new TenantId(uuid);
                        break;
                    case GENIE_ENDPOINT_IP:
                        // TODO: support v6
                        Ipv4Address ipv4 = new Ipv4Address(poi.getString(ppi.getPropId()));
                        ip = new IpAddress(ipv4);
                        break;
                    default:
                        break;
                }
            }

        }

        if (ip != null && l3cid != null) {
            L3AddressBuilder l3ab = new L3AddressBuilder();
            l3ab.setIpAddress(ip);
            l3ab.setL3Context(l3cid);
            l3al.add(l3ab.build());
        }

        String epType = mo.getSubject();
        if (epType.equals(GENIE_ENDPOINT_RN)) {
            L2EprOperation l2eo = new L2EprOperation(prr);
            l2eo.setAgentId(agentId);
            l2eo.setContextId(l2bdid);
            l2eo.setEndpointGroupId(epgid);
            l2eo.setL3AddressList(l3al);
            l2eo.setMacAddress(mac);
            l2eo.setTenantId(tid);
            op = l2eo;
        } else if (epType.equals(GENIE_ENDPOINT_L3_RN)) {
            L3EprOperation l3eo = new L3EprOperation(prr);
            l3eo.setAgentId(agentId);
            l3eo.setContextId(l3cid);
            l3eo.setEndpointGroupId(epgid);
            l3eo.setIpAddress(ip);
            l3eo.setL3AddressList(l3al);
            l3eo.setMacAddress(mac);
            l3eo.setTenantId(tid);
            op = l3eo;
        }

        return op;
    }

    /**
     * Get the Endpoint Registry Operation from the Genie URI. The Genie URI
     * must be a URI for an Endpoint in the EPR.
     *
     * @param uri
     * @param subject
     * @return
     */
    public static EprOperation getEprOpFromUri(Uri uri, String subject) {
        PolicyUri convertedUri = genieUriToOdlUri(new PolicyUri(uri.getValue()));
        String convertedSubject = genieToOdlMap.get(subject);

        /*
         * Get the objects that are common to all EPs
         */
        EprOperation op = null;
        String identifier = convertedUri.pop();
        String context = convertedUri.pop();
        Identity id = new Identity(identifier);
        id.setContext(context);

        /*
         * Determine if it's an L2 or L3 EPR Op, and get the EP-specific objects
         */

        if (convertedSubject.equals(ENDPOINT_RN)) {
            L2EprOperation l2eo = new L2EprOperation();
            l2eo.setContextId(new L2BridgeDomainId(context));
            l2eo.setMacAddress(new MacAddress(identifier));

            op = l2eo;
        } else if (convertedSubject.equals(ENDPOINT_L3_RN)) {
            L3EprOperation l3eo = new L3EprOperation();
            l3eo.setContextId(new L3ContextId(context));
            Ipv4Address ipv4 = new Ipv4Address(identifier);
            l3eo.setIpAddress(new IpAddress(ipv4));

            op = l3eo;
        }

        return op;
    }

    public static EprOperation getEprOpFromEpId(EndpointIdentity eid, String subject) {
        EprOperation op = null;
        Uri uri = eid.getContext();
        String rn = eid.getIdentifier();

        PolicyUri convertedUri = genieUriToOdlUri(new PolicyUri(uri.getValue()));
        String convertedSubject = genieToOdlMap.get(subject);

        /*
         * It's not clear if the identifier contains both of the keys for an EP
         * (e.g. context + MAC/IP), or if it's just the last identifier
         * (MAC/IP). From the description in the RFC, it seems to be just the
         * last identifer, so we'll start with that.
         */
        String context = convertedUri.pop();
        Identity id = new Identity(rn);
        id.setContext(context);

        /*
         * Determine if it's an L2 or L3 EPR Op, and get the EP-specific objects
         */

        if (convertedSubject.equals(ENDPOINT_RN)) {
            L2EprOperation l2eo = new L2EprOperation();
            l2eo.setContextId(new L2BridgeDomainId(context));
            l2eo.setMacAddress(new MacAddress(rn));

            op = l2eo;
        } else if (convertedSubject.equals(ENDPOINT_L3_RN)) {
            L3EprOperation l3eo = new L3EprOperation();
            l3eo.setContextId(new L3ContextId(context));
            Ipv4Address ipv4 = new Ipv4Address(rn);
            l3eo.setIpAddress(new IpAddress(ipv4));

            op = l3eo;
        }

        return op;
    }

    public static ManagedObject getMoFromEp(DataObject obj) {
        MacAddress mac = null;
        EndpointGroupId epgid = null;
        String context = null;
        String uuid = null;
        IpAddress ip = null;
        PolicyUri uri = new PolicyUri();

        ManagedObject mo = new ManagedObject();
        String className = null;
        PolicyClassInfo pci = null;
        PolicyObjectInstance poi = null;

        if (obj instanceof Endpoint) {
            Endpoint ep = (Endpoint) obj;
            epgid = ep.getEndpointGroup();
            mac = ep.getMacAddress();
            uuid = ep.getTenant().getValue();
            context = ep.getL2Context().getValue();
            className = GENIE_ENDPOINT_RN;
            pci = mit.getClass(className);
            poi = new PolicyObjectInstance(pci.getClassId());
            uri.push(GENIE_EPR_L2_ROOT);
            uri.push(GENIE_ENDPOINT_RN);
            uri.push(ep.getL2Context().getValue());
            uri.push(ep.getMacAddress().getValue());
            setParentFields(uri, poi, GENIE_ENDPOINT_RN, true);
        } else if (obj instanceof EndpointL3) {
            EndpointL3 ep = (EndpointL3) obj;
            epgid = ep.getEndpointGroup();
            mac = ep.getMacAddress();
            uuid = ep.getTenant().getValue();
            ip = ep.getIpAddress();
            context = ep.getL3Context().getValue();
            className = GENIE_ENDPOINT_L3_RN;
            pci = mit.getClass(className);
            poi = new PolicyObjectInstance(pci.getClassId());
            uri.push(GENIE_EPR_L3_ROOT);
            uri.push(GENIE_ENDPOINT_L3_RN);
            uri.push(ep.getL3Context().getValue());
            uri.push(ep.getIpAddress().getIpv4Address().getValue());
            setParentFields(uri, poi, GENIE_ENDPOINT_L3_RN, true);
        } else {
            return null;
        }

        mo.setSubject(className);

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        /*
         * Set all the properties based on the EP
         */
        // TODO: add support for vector values
        for (PolicyPropertyInfo ppi : ppil) {
            switch (ppi.getPropName()) {
                case GENIE_ENDPOINT_MAC:
                    poi.setMacAddress(ppi.getPropId(), mac);
                    break;
                case GENIE_ENDPOINT_EPG:
                    poi.setString(ppi.getPropId(), epgid.getValue());
                    break;
                case GENIE_ENDPOINT_CONTEXT:
                    poi.setString(ppi.getPropId(), context);
                    break;
                case GENIE_ENDPOINT_IP:
                    // TODO: support v6
                    poi.setString(ppi.getPropId(), ip.toString());
                    break;
                case GENIE_ENDPOINT_UUID:
                    poi.setString(ppi.getPropId(), uuid);
                    break;
                default:
                    break;

            }
        }
        lib.serializeMoProperties(pci, poi, mo, mit);

        mo.setParent_uri(poi.getParent());
        mo.setParent_subject(poi.getParentSubject());
        mo.setParent_relation(poi.getParentRelation());
        mo.setSubject(GENIE_EPG_TO_NETWORK_DOMAIN_RN);
        mo.setUri(uri.getUri());

        return mo;
    }

    public static ManagedObject getMoFromOp(EprOperation op) {
        MacAddress mac = null;
        EndpointGroupId epgid = null;
        String context = null;
        // String uuid = null;
        IpAddress ip = null;

        ManagedObject mo = new ManagedObject();
        String className = null;
        PolicyClassInfo pci = null;
        PolicyObjectInstance poi = null;

        /*
         * The problem is that the op can return different types of things -
         * Endpoint, EndpointL3. I guess I need to return the individual pieces
         * from the op, and use those to construct the MO
         */
        if (op instanceof L2EprOperation) {
            L2EprOperation l2eo = (L2EprOperation) op;
            Endpoint ep = l2eo.getEp();
            if (ep == null)
                return null;
            epgid = ep.getEndpointGroup();
            mac = ep.getMacAddress();
            context = ep.getL2Context().getValue();
            className = GENIE_ENDPOINT_RN;

        } else if (op instanceof L3EprOperation) {
            L3EprOperation l3eo = (L3EprOperation) op;
            EndpointL3 ep = l3eo.getEp();
            if (ep == null)
                return null;

            epgid = ep.getEndpointGroup();
            mac = ep.getMacAddress();
            ip = ep.getIpAddress();
            context = ep.getL3Context().getValue();
            className = GENIE_ENDPOINT_L3_RN;
        }
        pci = mit.getClass(className);
        poi = new PolicyObjectInstance(pci.getClassId());

        mo.setSubject(className);

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return null;

        /*
         * Set all the properties based on the EP
         */
        // TODO: add support for vector values
        for (PolicyPropertyInfo ppi : ppil) {
            switch (ppi.getPropName()) {
                case GENIE_ENDPOINT_MAC:
                    poi.setMacAddress(ppi.getPropId(), mac);
                    break;
                case GENIE_ENDPOINT_EPG:
                    poi.setString(ppi.getPropId(), epgid.getValue());
                    break;
                case GENIE_ENDPOINT_CONTEXT:
                    poi.setString(ppi.getPropId(), context);
                    break;
                case GENIE_ENDPOINT_IP:
                    // TODO: support v6
                    poi.setString(ppi.getPropId(), ip.toString());
                    break;
                case GENIE_ENDPOINT_UUID:
                default:
                    break;

            }
        }
        lib.serializeMoProperties(pci, poi, mo, mit);

        return mo;
    }

    /**
     * Merge the contents of two {@link ManagedObject} objects that represent
     * the same MO. The contents of mo2 are merged into the contents of mo1
     *
     * @param mo1
     * @param mo2
     */
    public static void mergeMos(ManagedObject mo1, ManagedObject mo2) {

        /*
         * Some sanity checks, to make sure we're dealing with the same MO
         */
        if (!mo1.getSubject().equals(mo2.getSubject()) || !mo1.getUri().getValue().equals(mo2.getUri().getValue())) {
            return;
        }

        /*
         * The only things that need merging are the children URIs and the
         * properties.
         */
        List<Property> mo1Props = mo1.getProperties();
        List<Property> mo2Props = mo2.getProperties();
        if (mo2Props != null) {
            if (mo1Props == null) {
                mo1.setProperties(mo2Props);
            } else {
                for (Property prop : mo2Props) {
                    if (!mo1Props.contains(prop)) {
                        mo1Props.add(prop);
                    }
                }
            }
        }

        List<Uri> mo1Children = mo1.getChildren();
        List<Uri> mo2Children = mo2.getChildren();
        if (mo2Children != null) {
            if (mo1Children == null) {
                mo1.setChildren(mo2Children);
            } else {
                for (Uri child : mo2Children) {
                    if (!mo1Children.contains(child)) {
                        mo1Children.add(child);
                    }
                }
            }
        }
    }

}
