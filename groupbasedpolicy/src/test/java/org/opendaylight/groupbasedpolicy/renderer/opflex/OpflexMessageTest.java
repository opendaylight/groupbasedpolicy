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

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointPolicyUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointPolicyUpdateResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyResolutionRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyResolutionResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyTriggerRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyTriggerResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyUpdateResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.StateReportRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.StateReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * Test the serialization and deserialization of RPC Messages,
 * and check against expected structure and values.
 */
public class OpflexMessageTest {
    protected static final Logger logger = LoggerFactory.getLogger(OpflexMessageTest.class);

    private enum Role {
        POLICY_REPOSITORY("policy_repository"),
        ENDPOINT_REGISTRY("endpoint_registry"),
        OBSERVER("observer"),
        POLICY_ELEMENT("policy_element");

        private String role;
        Role(String role) {
            this.role = role;
        }
        @Override
        public String toString() {
            return this.role;
        }
    }

    private static final String ID_UUID = "2da9e3d7-0bbe-4099-b343-12783777452f";
    private static final String SEND_IDENTITY = "send_identity";
    private static final String POLICY_REQUEST = "resolve_policy";
    private static final String DOMAIN_UUID = "75caaff2-cb4f-4509-b45e-47b447cb35a9";
    private static final String NAME = "vm1";
    private static final String IDENTITY = "192.168.0.1:56732";
    private static final String opflexIdentityRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + SEND_IDENTITY + "\"," +
            "  \"params\": [ {" +
            "      \"name\":    \"" + NAME + "\"," +
            "      \"domain\":  \"" + DOMAIN_UUID + "\"," +
            "      \"my_role\": [\"" + Role.POLICY_ELEMENT.toString() + "\"]" +
            "   }] }";

    private static final String opflexIdentityResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {" +
            "      \"name\":    \"" + NAME + "\"," +
            "      \"domain\":  \"" + DOMAIN_UUID + "\"," +
            "      \"my_role\": [\"" + Role.POLICY_REPOSITORY.toString() + "\"]," +
            "      \"peers\": [" +
            "           { \"role\": \"" + Role.ENDPOINT_REGISTRY.toString() + "\"," +
            "             \"connectivity_info\": \"" + IDENTITY + "\"}," +
            "           { \"role\": \"" + Role.OBSERVER.toString() + "\"," +
            "             \"connectivity_info\": \"" + IDENTITY + "\"}" +
            "       ]" +
            "   }}";

    public static final String SUBJECT = "webContract";
    public static final String CONTEXT = "353786fd-7327-41dd-b7de-5d672e303730";
    public static final String POLICY_NAME = "webFarmEpg";
    public static final String PROP_NAME = "subject";
    public static final String PROP_DATA = "http";
    public static final String MO_NAME = "webFarmContract";
    public static final String URI = "ef130684-ac17-4118-ad36-8dea0babc7b2";
    public static final String DATA = "condition:notAuthorized";
    public static final String PRR = "100";

    private static final String opflexPolicyRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + POLICY_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"subject\":    \"" + SUBJECT + "\"," +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"policy_name\":  \"" + POLICY_NAME + "\"," +
            "      \"on_behalf_of\":  \"" + URI + "\"," +
            "      \"data\":  \"" + DATA + "\"" +
            "   }] }";

    private static final String emptyMo = 
            "{         \"name\": \"" + MO_NAME + "\"," +
            "          \"properties\": [ {\"name\": \"" + PROP_NAME + "\", " +
            "                             \"data\": \"" + PROP_DATA + "\" }]," +
            "          \"children\": []," +
            "          \"statistics\": []," +
            "          \"from_relations\": []," +
            "          \"to_relations\": []," +
            "          \"faults\": []," +
            "          \"health\": [] }";
            
    private static final String managedObject = 
            "{ \"name\": \"" + MO_NAME + "\", " +
            "  \"properties\": [ { \"name\": \"" + PROP_NAME + "\", " +
            "                      \"data\": \"" + PROP_DATA + "\" }]," +                    
            "  \"children\":   [ " + emptyMo + " ], " +
            "  \"statistics\":   [ " + emptyMo + " ], " +
            "  \"from_relations\":   [ " + emptyMo + " ], " +
            "  \"to_relations\":   [ " + emptyMo + " ], " +
            "  \"faults\":   [ " + emptyMo + " ], " +
            "  \"health\":   [ " + emptyMo + " ]}";
            
    private static final String opflexPolicyResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {" +
            "      \"policy\": [ " + managedObject + "], " +
            "      \"prr\":  \"" + PRR + "\"" +
            "   }}";

    private static final String opflexUpdateRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + POLICY_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"subtree\":  [" + managedObject + "]," +
            "      \"prr\":  \"" + PRR + "\"" +
            "   }] }";

    private static final String opflexUpdateResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {}}";

    private static final String TRIGGER_REQUEST = "trigger_policy";
    private static final String TYPE = "someType";

    private static final String opflexTriggerRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + TRIGGER_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"policy_type\":    \"" + TYPE + "\"," +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"policy_name\":  \"" + POLICY_NAME + "\"," +
            "      \"prr\":  \"" + PRR + "\"" +
            "   }] }";


    private static final String opflexTriggerResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {} }";

    private static final String EP_DECLARE_REQUEST = "endpoint_declaration";
    private static final String LOCATION = "sw3/p12";
    private static final String EP_ID_UUID = "d90173aa-621a-4009-b5da-c930cce0918f";
    private static final String STATUS = "attach";

    private static final String opflexEpDeclareRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + EP_DECLARE_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"subject\":    \"" + SUBJECT + "\"," +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"policy_name\":  \"" + POLICY_NAME + "\"," +
            "      \"location\":  \"" + LOCATION + "\"," +
            "      \"identifier\":  [\"" + EP_ID_UUID + "\"]," +
            "      \"data\":  [\"" + DATA + "\"]," +
            "      \"status\":  \"" + STATUS + "\"," +
            "      \"prr\":  \"" + PRR + "\"" +
            "   }] }";

    private static final String opflexEpDeclareResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {} }";

    private static final String ENDPOINT_REQUEST = "endpoint_request";
    private static final String opflexEpRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + ENDPOINT_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"subject\":    \"" + SUBJECT + "\"," +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"identifier\":  [\"" + EP_ID_UUID + "\"]" +
            "   }] }";

    private static final String opflexEpResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {" +
            "      \"endpoint\": [{" +
            "           \"subject\":    \"" + SUBJECT + "\"," +
            "           \"context\":  \"" + CONTEXT + "\"," +
            "           \"policy_name\":  \"" + POLICY_NAME + "\"," +
            "           \"location\":  \"" + LOCATION + "\"," +
            "           \"identifier\":  [\"" + EP_ID_UUID + "\"]," +
            "           \"data\":  [\"" + DATA + "\"]," +
            "           \"status\":  \"" + STATUS + "\"," +
            "           \"prr\":  \"" + PRR + "\"" +
            "       }]" +
            "   }}";

    private static final String EP_POLICY_UDPATE_REQUEST = "endpoint_update_policy";
    private static final String opflexEpPolicyUpdateRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + EP_POLICY_UDPATE_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"subject\":    \"" + SUBJECT + "\"," +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"policy_name\":  \"" + POLICY_NAME + "\"," +
            "      \"location\":  \"" + LOCATION + "\"," +
            "      \"identifier\":  [\"" + EP_ID_UUID + "\"]," +
            "      \"data\":  [\"" + DATA + "\"]," +
            "      \"status\":  \"" + STATUS + "\"," +
            "      \"ttl\":  \"" + PRR + "\"" +
            "   }] }";


    private static final String opflexEpPolicyUpdateResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {} }";

    private static final String STATE_REQUEST = "report_state";
    private static final String opflexStateRequest =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"method\": \"" + STATE_REQUEST + "\"," +
            "  \"params\": [ {" +
            "      \"subject\":    \"" + SUBJECT + "\"," +
            "      \"context\":  \"" + CONTEXT + "\"," +
            "      \"object\":  " + managedObject + "," +
            "      \"fault\":  [" + managedObject + "]," +
            "      \"event\":  [" + managedObject + "]," +
            "      \"statistics\":  [" + managedObject + "]," +
            "      \"health\":  [" + managedObject + "]" +
            "   }] }";



    private static final String opflexStateResponse =
            "{ \"id\":     \"" + ID_UUID + "\"," +
            "  \"error\":  {}," +
            "  \"result\": {} }";

    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @Test
    public void testIdentityRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexIdentityRequest, IdentityRequest.class);
        assertTrue(rpcMsg instanceof IdentityRequest);
        IdentityRequest opflexRequest = (IdentityRequest)rpcMsg;
        assertTrue(opflexRequest.getId().equals(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(SEND_IDENTITY));
        assertTrue(opflexRequest.getParams()
                .get(0).getDomain().equals(DOMAIN_UUID));
        assertTrue(opflexRequest.getParams()
                .get(0).getName().equals(NAME));
        assertTrue(opflexRequest.getParams()
                .get(0).getMy_role().get(0).equals(Role.POLICY_ELEMENT.toString()));
        assertTrue(opflexRequest.getParams()
                .get(0).getName().equals(NAME));
    }

    @Test
    public void testIdentityResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexIdentityResponse, IdentityResponse.class);
        assertTrue(rpcMsg instanceof IdentityResponse);
        IdentityResponse opflexResponse = (IdentityResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getResult()
                .getDomain().equals(DOMAIN_UUID));
        assertTrue(opflexResponse.getResult()
                .getName().equals(NAME));
        assertTrue(opflexResponse.getResult()
                .getMy_role().get(0).equals(Role.POLICY_REPOSITORY.toString()));
        assertTrue(opflexResponse.getResult()
                .getPeers().get(0).getRole().equals(Role.ENDPOINT_REGISTRY.toString()));
        assertTrue(opflexResponse.getResult()
                .getPeers().get(1).getRole().equals(Role.OBSERVER.toString()));
    }

    @Test
    public void testPolicyRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyRequest, PolicyResolutionRequest.class);
        assertTrue(rpcMsg instanceof PolicyResolutionRequest);
        PolicyResolutionRequest opflexRequest = (PolicyResolutionRequest)rpcMsg;
        assertTrue(opflexRequest.getId().equals(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(POLICY_REQUEST));
        assertTrue(opflexRequest.getParams()
                .get(0).getContext().equals(CONTEXT));
        assertTrue(opflexRequest.getParams()
                .get(0).getOn_behalf_of().equals(URI));
        assertTrue(opflexRequest.getParams()
                .get(0).getPolicy_name().equals(POLICY_NAME));
        assertTrue(opflexRequest.getParams()
                .get(0).getSubject().equals(SUBJECT));
        assertTrue(opflexRequest.getParams()
                .get(0).getData().equals(DATA));

    }

    @Test
    public void testPolicyResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyResponse, PolicyResolutionResponse.class);
        assertTrue(rpcMsg instanceof PolicyResolutionResponse);
        PolicyResolutionResponse opflexResponse = (PolicyResolutionResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getTo_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getResult()
                .getPrr() == Integer.parseInt(PRR));
    }

    @Test
    public void testUpdateRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexUpdateRequest, PolicyUpdateRequest.class);
        assertTrue(rpcMsg instanceof PolicyUpdateRequest);
        PolicyUpdateRequest opflexResponse = (PolicyUpdateRequest)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getSubtree().get(0).getTo_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams()
                .get(0).getPrr() == Integer.parseInt(PRR));
    }

    @Test
    public void testUpdateResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexUpdateResponse, PolicyUpdateResponse.class);
        assertTrue(rpcMsg instanceof PolicyUpdateResponse);
        PolicyUpdateResponse opflexResponse = (PolicyUpdateResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        
    }

    @Test
    public void testTriggerRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexTriggerRequest, PolicyTriggerRequest.class);
        assertTrue(rpcMsg instanceof PolicyTriggerRequest);
        PolicyTriggerRequest opflexResponse = (PolicyTriggerRequest)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getPolicy_name().equals(POLICY_NAME));
        assertTrue(opflexResponse.getParams()
                .get(0).getPolicy_type().equals(TYPE));
        assertTrue(opflexResponse.getParams()
                .get(0).getContext().equals(CONTEXT));
        assertTrue(opflexResponse.getParams()
                .get(0).getPrr() == Integer.parseInt(PRR));
    }


    @Test
    public void testTriggerResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexTriggerResponse, PolicyTriggerResponse.class);
        assertTrue(rpcMsg instanceof PolicyTriggerResponse);
        PolicyTriggerResponse opflexResponse = (PolicyTriggerResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
    }


    @Test
    public void testEpDeclareRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpDeclareRequest, EndpointDeclarationRequest.class);
        assertTrue(rpcMsg instanceof EndpointDeclarationRequest);
        EndpointDeclarationRequest opflexResponse = (EndpointDeclarationRequest)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT));
        assertTrue(opflexResponse.getParams()
                .get(0).getContext().equals(CONTEXT));
        assertTrue(opflexResponse.getParams()
                .get(0).getPolicy_name().equals(POLICY_NAME));
        assertTrue(opflexResponse.getParams()
                .get(0).getLocation().equals(LOCATION));
        assertTrue(opflexResponse.getParams()
                .get(0).getIdentifier().get(0).equals(EP_ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getData().get(0).equals(DATA));
        assertTrue(opflexResponse.getParams()
                .get(0).getStatus()
                .equals(STATUS));
        assertTrue(opflexResponse.getParams()
                .get(0).getPrr() == Integer.parseInt(PRR));
    }

    @Test
    public void testEpDeclareResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpDeclareResponse, EndpointDeclarationResponse.class);
        assertTrue(rpcMsg instanceof EndpointDeclarationResponse);
        EndpointDeclarationResponse opflexResponse = (EndpointDeclarationResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
    }


    @Test
    public void testEpRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpRequest, EndpointRequestRequest.class);
        assertTrue(rpcMsg instanceof EndpointRequestRequest);
        EndpointRequestRequest opflexResponse = (EndpointRequestRequest)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT));
        assertTrue(opflexResponse.getParams()
                .get(0).getContext().equals(CONTEXT));
        assertTrue(opflexResponse.getParams()
                .get(0).getIdentifier().get(0).equals(EP_ID_UUID));
    }


    @Test
    public void testEpResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpResponse, EndpointRequestResponse.class);
        assertTrue(rpcMsg instanceof EndpointRequestResponse);
        EndpointRequestResponse opflexResponse = (EndpointRequestResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getSubject().equals(SUBJECT));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getContext().equals(CONTEXT));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getPolicy_name().equals(POLICY_NAME));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getLocation().equals(LOCATION));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getIdentifier().get(0).equals(EP_ID_UUID));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getData().get(0).equals(DATA));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getStatus().equals(STATUS));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getPrr() == Integer.parseInt(PRR));

    }

    @Test
    public void testEpPolicyUpdateRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpPolicyUpdateRequest, EndpointPolicyUpdateRequest.class);
        assertTrue(rpcMsg instanceof EndpointPolicyUpdateRequest);
        EndpointPolicyUpdateRequest opflexResponse = (EndpointPolicyUpdateRequest)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT));
        assertTrue(opflexResponse.getParams()
                .get(0).getContext().equals(CONTEXT));
        assertTrue(opflexResponse.getParams()
                .get(0).getPolicy_name().equals(POLICY_NAME));
        assertTrue(opflexResponse.getParams()
                .get(0).getLocation().equals(LOCATION));
        assertTrue(opflexResponse.getParams()
                .get(0).getIdentifier().get(0).equals(EP_ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getData().get(0).equals(DATA));
        assertTrue(opflexResponse.getParams()
                .get(0).getStatus()
                .equals(STATUS));
        assertTrue(opflexResponse.getParams()
                .get(0).getTtl() == Integer.parseInt(PRR));
    }


    @Test
    public void testEpPolicyUpdateResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpPolicyUpdateResponse, EndpointPolicyUpdateResponse.class);
        assertTrue(rpcMsg instanceof EndpointPolicyUpdateResponse);
        EndpointPolicyUpdateResponse opflexResponse = (EndpointPolicyUpdateResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
    }


    @Test
    public void testStateRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexStateRequest, StateReportRequest.class);
        assertTrue(rpcMsg instanceof StateReportRequest);
        StateReportRequest opflexResponse = (StateReportRequest)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT));
        assertTrue(opflexResponse.getParams()
                .get(0).getContext().equals(CONTEXT));
        assertTrue(opflexResponse.getParams()
                .get(0).getObject().getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObject().getTo_relations().get(0).getName().equals(MO_NAME));           
        
        assertTrue(opflexResponse.getParams()
                .get(0).getFault().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getFault().get(0).getTo_relations().get(0).getName().equals(MO_NAME));   

        assertTrue(opflexResponse.getParams()
                .get(0).getEvent().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEvent().get(0).getTo_relations().get(0).getName().equals(MO_NAME));          
        
        
        assertTrue(opflexResponse.getParams()
                .get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getStatistics().get(0).getTo_relations().get(0).getName().equals(MO_NAME));           
        
        assertTrue(opflexResponse.getParams()
                .get(0).getHealth().get(0).getName().equals(MO_NAME));        
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getProperties().get(0).getData().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getFaults().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getFrom_relations().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getHealth().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getStatistics().get(0).getName().equals(MO_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getHealth().get(0).getTo_relations().get(0).getName().equals(MO_NAME));   
    }

    @Test
    public void testStateResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexStateResponse, StateReportResponse.class);
        assertTrue(rpcMsg instanceof StateReportResponse);
        StateReportResponse opflexResponse = (StateReportResponse)rpcMsg;
        assertTrue(opflexResponse.getId().equals(ID_UUID));
    }
}
