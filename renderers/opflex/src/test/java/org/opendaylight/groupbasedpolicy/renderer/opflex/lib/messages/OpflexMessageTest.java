/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.Role;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUndeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUndeclareResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUnresolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUnresolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUpdateResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointResolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointResolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.IdentityRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.IdentityResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyResolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyResolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUnresolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUnresolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUpdateResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.StateReportRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.StateReportResponse;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
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

//    private enum Role {
//        POLICY_REPOSITORY("policy_repository"),
//        ENDPOINT_REGISTRY("endpoint_registry"),
//        OBSERVER("observer"),
//        POLICY_ELEMENT("policy_element");
//
//        private String role;
//        Role(String role) {
//            this.role = role;
//        }
//        @Override
//        public String toString() {
//            return this.role;
//        }
//    }

    // TODO: should just import these from somewhere?
    /*
     * common JSON definitions
     */
    private static final String JSON_OBJECT_OPEN = "{";
    private static final String JSON_OBJECT_CLOSE = "}";
    private static final String JSON_ARRAY_OPEN = "[";
    private static final String JSON_ARRAY_CLOSE = "]";

    /*
     * common JSON-RPC definitions
     */
    // Just use the same UUID for all IDs
    private static final String ID_UUID = "2da9e3d7-0bbe-4099-b343-12783777452f";
    private static final String JSONRPC_ID = "\"id\": [\"send_itentity\", \"" + ID_UUID + "\"],";
    private static final String JSONRPC_METHOD = "  \"method\":";
    private static final String JSONRPC_PARAMS_OPEN = "\"params\": " + JSON_ARRAY_OPEN;
    private static final String JSONRPC_PARAMS_CLOSE = JSON_ARRAY_CLOSE;
    private static final String JSONRPC_RESULT_OPEN = "\"result\": " + JSON_OBJECT_OPEN;
    private static final String JSONRPC_RESULT_CLOSE = JSON_OBJECT_CLOSE;
    private static final String JSONRPC_ERROR_OPEN = "\"error\":  " + JSON_OBJECT_OPEN;
    private static final String JSONRPC_ERROR_CLOSE = JSON_OBJECT_CLOSE;

    /*
     * Common OpFlex message definitions
     */
    public static final String SUBJECT = "\"subject\":";

    /*
     * Identity message-specific definitions
     */
    private static final String DOMAIN_UUID = "75caaff2-cb4f-4509-b45e-47b447cb35a9";
    private static final String TEST_NAME = "vm1";
    private static final String IDENTITY = "192.168.0.1:56732";
    // Use the same protocol version for all test messages
    private static final String OPFLEX_PROTO_VERSION = "1.0";
    private static final String PROTO_VERSION = "\"proto_version\": \"" + OPFLEX_PROTO_VERSION + "\",";
    private static final String MY_ROLE_OPEN = "\"my_role\": " + JSON_ARRAY_OPEN;
    private static final String MY_ROLE_CLOSE = JSON_ARRAY_CLOSE;
    private static final String PEERS_OPEN = "\"peers\":" + JSON_ARRAY_OPEN + JSON_OBJECT_OPEN;
    private static final String PEERS_CLOSE = JSON_OBJECT_CLOSE + JSON_ARRAY_CLOSE;
    private static final String OPFLEX_ROLE_OPEN = "\"role\":" + JSON_ARRAY_OPEN;
    private static final String OPFLEX_ROLE_CLOSE = JSON_ARRAY_CLOSE;
    // Use the same test name for all OpFlex name fields
    private static final String OPFLEX_NAME = "\"name\": \"" + TEST_NAME + "\",";
    // Use the same Domain for all test messages
    private static final String OPFLEX_DOMAIN = "\"domain\":  \"" + DOMAIN_UUID + "\",";

    /*
     * Identity messages
     */
    private static final String opflexIdentityRequest =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + IdentityRequest.IDENTITY_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN + JSON_OBJECT_OPEN +
            PROTO_VERSION +
            OPFLEX_NAME +
            OPFLEX_DOMAIN +
            MY_ROLE_OPEN +
            "\"" + Role.POLICY_ELEMENT.toString() + "\"" +
            MY_ROLE_CLOSE +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexIdentityResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            OPFLEX_NAME +
            OPFLEX_DOMAIN +
            MY_ROLE_OPEN +
            "\"" + Role.POLICY_REPOSITORY.toString() + "\"" +
            MY_ROLE_CLOSE + "," +
            PEERS_OPEN +
            OPFLEX_ROLE_OPEN +
            "\"" + Role.ENDPOINT_REGISTRY.toString() + "\"," +
            "\"" + Role.OBSERVER.toString() + "\"" +
            OPFLEX_ROLE_CLOSE + "," +
            "\"connectivity_info\": \"" + IDENTITY  + "\"" +
            PEERS_CLOSE +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Policy message-specific definitions
     */
    public static final String POLICY_URI = "\"policy_uri\":";
    public static final String DATA = "\"data\":";
    public static final String PRR = "\"prr\":";
    public static final String POLICY_IDENT_OPEN = "\"policy_ident\":" + JSON_OBJECT_OPEN;
    public static final String POLICY_IDENT_CLOSE = JSON_OBJECT_CLOSE;
    public static final String POLICY_IDENT_CONTEXT = "\"context\":";
    public static final String POLICY_IDENT_NAME = "\"name\":";

    public static final Uri POLICY_IDENT_CONTEXT_VALUE =
    		new Uri("/PolicyUniverse/PolicySpace/f4d908bd-2911-43d4-9f22-a09f36ed3ddb");
    public static final Uri POLICY_URI_VALUE =
    		new Uri("/PolicyUniverse/PolicySpace/f4d908bd-2911-43d4-9f22-a09f36ed3ddb" +
    				"/GbpContract/b3506f37-b324-4092-80dd-f8a63bd193db");
    public static final String POLICY_CONTEXT = "Contract";
    public static final String PROP_NAME = "subject";
    public static final String PROP_DATA = "http";
    public static final Uri URI_NAME = new Uri("/foo/bar/t/robot");
    public static final Uri PARENT_URI_NAME = new Uri("/foo/bar/t");
    public static final String SUBJECT_NAME = "webFarmContract";
    public static final String PARENT_RELATION_NAME = "fooboo";
    public static final String PARENT_SUBJECT_NAME = "webFarmContractParent";
    public static final String POLICY_IDENT_URI = "ef130684-ac17-4118-ad36-8dea0babc7b2";
    public static final String DATA_NAME = "condition:notAuthorized";
    public static final String PRR_VALUE = "100";

    /*
     * Policy Request using URI
     */
    private static final String opflexPolicyResolve1 =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + PolicyResolveRequest.RESOLVE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN + JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            POLICY_URI + "\"" + POLICY_URI_VALUE.getValue() + "\"," +
            POLICY_IDENT_OPEN +
            POLICY_IDENT_NAME + "\"\"," +
            POLICY_IDENT_CONTEXT + "\"\"" +
            POLICY_IDENT_CLOSE + "," +
            DATA +  "\"" + DATA_NAME + "\"," +
            PRR + "\"" + PRR_VALUE + "\"" +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Policy Request using policy identity
     */
    private static final String opflexPolicyResolve2 =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + PolicyResolveRequest.RESOLVE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN + JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            POLICY_URI + "\"\"," +
            POLICY_IDENT_OPEN +
            POLICY_IDENT_NAME + "\"" + POLICY_IDENT_URI + "\"," +
            POLICY_IDENT_CONTEXT + "\"" + POLICY_IDENT_CONTEXT_VALUE.getValue() + "\"" +
            POLICY_IDENT_CLOSE + "," +
            DATA +  "\"" + DATA_NAME + "\"," +
            PRR + "\"" + PRR_VALUE + "\"" +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Managed Object definitions
     */
    private static final String URI = "          \"uri\":";
    private static final String PROPERTIES_OPEN = "\"properties\":" + JSON_ARRAY_OPEN;
    private static final String PROPERTIES_CLOSE = JSON_ARRAY_CLOSE;
    private static final String PROPERTY_NAME = "\"name\":";
    private static final String PROPERTY_DATA = "\"data\":";
    private static final String PARENT_SUBJECT = "\"parent_subject\":";
    private static final String PARENT_URI = "\"parent_uri\":";
    private static final String PARENT_RELATION = "\"parent_relation\":";
    private static final String CHILDREN = "\"children\":";
    private static final String POLICY_OPEN = "\"policy\":" + JSON_ARRAY_OPEN;
    private static final String POLICY_CLOSE = JSON_ARRAY_CLOSE;

    private static final String managedObject =
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            URI + "\"" + URI_NAME.getValue() + "\"," +
            PROPERTIES_OPEN +
            JSON_OBJECT_OPEN +
            PROPERTY_NAME + "\"" + PROP_NAME + "\", " +
            PROPERTY_DATA + "\"" + PROP_DATA + "\"" +
            JSON_OBJECT_CLOSE +
            PROPERTIES_CLOSE + ", " +
            PARENT_SUBJECT + "\"" + PARENT_SUBJECT_NAME + "\"," +
            PARENT_URI + "\"" + PARENT_URI_NAME.getValue() + "\"," +
            PARENT_RELATION + "\"" + PARENT_RELATION_NAME + "\"," +
            CHILDREN + JSON_ARRAY_OPEN + JSON_ARRAY_CLOSE;

    private static final String opflexPolicyResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            POLICY_OPEN +
            JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE +
            POLICY_CLOSE +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Policy Unresolve message-specific definitions
     */
    private static final String opflexPolicyUnresolveRequest =
            JSON_OBJECT_OPEN + JSONRPC_ID +
            JSONRPC_METHOD + "\"" + PolicyUnresolveRequest.UNRESOLVE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN + JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            POLICY_URI + "\"\"," +
            POLICY_IDENT_OPEN +
            POLICY_IDENT_NAME + "\"" + POLICY_IDENT_URI + "\"," +
            POLICY_IDENT_CONTEXT + "\"" + POLICY_IDENT_CONTEXT_VALUE.getValue() + "\"" +
            POLICY_IDENT_CLOSE +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexPolicyUnresolveResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Pollicy Update message-specific definitions
     */
    private static final String REPLACE_OPEN = "\"replace\":" + JSON_ARRAY_OPEN;
    private static final String REPLACE_CLOSE = JSON_ARRAY_CLOSE;
    private static final String MERGE_CHILDREN_OPEN = "\"merge_children\":" + JSON_ARRAY_OPEN;
    private static final String MERGE_CHILDREN_CLOSE = JSON_ARRAY_CLOSE;
    private static final String DELETE_URI_OPEN = "\"delete_uri\":" + JSON_ARRAY_OPEN;
    private static final String DELETE_URI_CLOSE = JSON_ARRAY_CLOSE;

    private static final String opflexUpdateRequest =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + PolicyUpdateRequest.UPDATE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            REPLACE_OPEN +
            JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE +
            REPLACE_CLOSE + "," +
            MERGE_CHILDREN_OPEN +
            JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE  +
            MERGE_CHILDREN_CLOSE + "," +
            DELETE_URI_OPEN + "\"" + POLICY_URI_VALUE.getValue() + "\"" + DELETE_URI_CLOSE +
            JSON_OBJECT_CLOSE +
            JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexUpdateResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;


    /*
     * Endoint Declare message-specific definitions
     */
    private static final String ENDPOINT_OPEN = "\"endpoint\":" + JSON_ARRAY_OPEN;
    private static final String ENDPOINT_CLOSE = JSON_ARRAY_CLOSE;


    private static final String opflexEpDeclareRequest =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + EndpointDeclareRequest.DECLARE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            ENDPOINT_OPEN +
            JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE  +
            ENDPOINT_CLOSE + "," +
            PRR + "\"" + PRR_VALUE + "\"" +
            JSON_OBJECT_CLOSE +
            JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexEpDeclareResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Endpoint Resolve message-specific definitions
     */
    public static final String ENDPOINT_IDENT_OPEN = "\"endpoint_ident\":" + JSON_OBJECT_OPEN;
    public static final String ENDPOINT_IDENT_CLOSE = JSON_OBJECT_CLOSE;
    public static final String ENDPOINT_IDENT_CONTEXT = "\"context\":";
    public static final String ENDPOINT_IDENT_NAME = "\"identifier\":";
    public static final Uri ENDPOINT_IDENT_CONTEXT_VALUE =
    		new Uri("/EprL2Universe/EprL2Ep/f4d908bd-2911-43d4-9f22-a09f36ed3ddb");
    public static final Uri ENDPOINT_URI_VALUE =
    		new Uri("/EprL2Universe/EprL2Ep/f4d908bd-2911-43d4-9f22-a09f36ed3ddb/b3506f37-b324-4092-80dd-f8a63bd193db");
    public static final String ENDPOINT_IDENT_URI = "ef130684-ac17-4118-ad36-8dea0babc7b2";
    public static final String ENDPOINT = "\"endpoint\":";


    /*
     * Endpoint Undeclare message-specific definitions
     */
    private static final String ENDPOINT_URI = "\"endpoint_uri\":";
    private static final String opflexEpUndeclareRequest =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + EndpointUndeclareRequest.UNDECLARE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            ENDPOINT_URI + "\"" + ENDPOINT_URI_VALUE.getValue() + "\"" +
            JSON_OBJECT_CLOSE +
            JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexEpUndeclareResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Endpoint Resolve using URI
     */
    private static final String opflexEpResolveRequest1 =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" +  EndpointResolveRequest.EP_RESOLVE_REQUEST_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            ENDPOINT_URI + "\"" + ENDPOINT_URI_VALUE.getValue() + "\"," +
            ENDPOINT_IDENT_OPEN +
            ENDPOINT_IDENT_CLOSE + "," +
            PRR + "\"" + PRR_VALUE + "\"" +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Endpoint Resolve using Identity
     */
    private static final String opflexEpResolveRequest2 =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" +  EndpointResolveRequest.EP_RESOLVE_REQUEST_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            ENDPOINT_URI + "\"\"," +
            ENDPOINT_IDENT_OPEN +
            ENDPOINT_IDENT_NAME + "\"" + ENDPOINT_IDENT_URI + "\"," +
            ENDPOINT_IDENT_CONTEXT + "\"" + ENDPOINT_IDENT_CONTEXT_VALUE.getValue() + "\"" +
            ENDPOINT_IDENT_CLOSE + "," +
            PRR + "\"" + PRR_VALUE + "\"" +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexEpResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            ENDPOINT +
            JSON_ARRAY_OPEN + JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE + JSON_ARRAY_CLOSE +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Endpoint Unresolve message-specific defines
     */

    /*
     * Endpoint Unresolve using URI
     */
    private static final String opflexEpUnresolveRequest1 =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" +  EndpointUnresolveRequest.EP_UNRESOLVE_REQUEST_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            ENDPOINT_URI + "\"" + ENDPOINT_URI_VALUE.getValue() + "\"," +
            ENDPOINT_IDENT_OPEN +
            ENDPOINT_IDENT_CLOSE +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Endpoint Unresolve using Identity
     */
    private static final String opflexEpUnresolveRequest2 =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" +  EndpointUnresolveRequest.EP_UNRESOLVE_REQUEST_MESSAGE+ "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            SUBJECT + "\"" + SUBJECT_NAME + "\"," +
            ENDPOINT_URI + "\"\"," +
            ENDPOINT_IDENT_OPEN +
            ENDPOINT_IDENT_NAME + "\"" + ENDPOINT_IDENT_URI + "\"," +
            ENDPOINT_IDENT_CONTEXT + "\"" + ENDPOINT_IDENT_CONTEXT_VALUE.getValue() + "\"" +
            ENDPOINT_IDENT_CLOSE +
            JSON_OBJECT_CLOSE + JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexEpUnresolveResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * Endpoint Update message-specific definitions
     */


    private static final String opflexEpUpdateRequest =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + EndpointUpdateRequest.EP_UPDATE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            REPLACE_OPEN +
            JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE +
            REPLACE_CLOSE + "," +
            DELETE_URI_OPEN + "\"" + POLICY_URI_VALUE.getValue() + "\"" + DELETE_URI_CLOSE +
            JSON_OBJECT_CLOSE +
            JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;

    private static final String opflexEpUpdateResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

    /*
     * State Report message-specific definitions
     */
    private static final String OBJECT = "\"object\":";
    private static final Uri OBJECT_VALUE =
    		new Uri("/EprL2Universe/EprL2Ep/f4d908bd-2911-43d4-9f22-a09f36ed3ddb");
    private static final String OBSERVABLE = "\"observable\":";
    private static final String opflexStateRequest =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_METHOD + "\"" + StateReportRequest.STATE_MESSAGE + "\"," +
            JSONRPC_PARAMS_OPEN +
            JSON_OBJECT_OPEN +
            OBJECT + "\"" + OBJECT_VALUE.getValue() + "\"," +
            OBSERVABLE +
            JSON_ARRAY_OPEN + JSON_OBJECT_OPEN + managedObject + JSON_OBJECT_CLOSE + JSON_ARRAY_CLOSE +
            JSON_OBJECT_CLOSE +
            JSONRPC_PARAMS_CLOSE +
            JSON_OBJECT_CLOSE;



    private static final String opflexStateResponse =
            JSON_OBJECT_OPEN +
            JSONRPC_ID +
            JSONRPC_ERROR_OPEN +
            JSONRPC_ERROR_CLOSE + "," +
            JSONRPC_RESULT_OPEN +
            JSONRPC_RESULT_CLOSE +
            JSON_OBJECT_CLOSE;

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
        String foo = objectMapper.writeValueAsString(rpcMsg);
        System.out.println(foo);
        assertTrue(rpcMsg instanceof IdentityRequest);
        IdentityRequest opflexRequest = (IdentityRequest)rpcMsg;
        assertTrue(opflexRequest.getId().toString().contains(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(IdentityRequest.IDENTITY_MESSAGE));
        assertTrue(opflexRequest.getParams()
        		.get(0).getProto_version().equals(OPFLEX_PROTO_VERSION));
        assertTrue(opflexRequest.getParams()
                .get(0).getDomain().equals(DOMAIN_UUID));
        assertTrue(opflexRequest.getParams()
                .get(0).getName().equals(TEST_NAME));
        assertTrue(opflexRequest.getParams()
                .get(0).getMy_role().get(0).equals(Role.POLICY_ELEMENT.toString()));
        assertTrue(opflexRequest.getParams()
                .get(0).getName().equals(TEST_NAME));
    }

    @Test
    public void testIdentityResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexIdentityResponse, IdentityResponse.class);
        assertTrue(rpcMsg instanceof IdentityResponse);
        IdentityResponse opflexResponse = (IdentityResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getResult()
                .getDomain().equals(DOMAIN_UUID));
        assertTrue(opflexResponse.getResult()
                .getName().equals(TEST_NAME));
        assertTrue(opflexResponse.getResult()
                .getMy_role().get(0).equals(Role.POLICY_REPOSITORY.toString()));
        assertTrue(opflexResponse.getResult()
                .getPeers().get(0).getRole().get(0).equals(Role.ENDPOINT_REGISTRY.toString()));
        assertTrue(opflexResponse.getResult()
                .getPeers().get(0).getRole().get(1).equals(Role.OBSERVER.toString()));
    }

    /**
     * Test a policy resolve message that uses the URI instead of the
     * policy identity for passing the policy requested.
     *
     * @throws Exception
     */
    @Test
    public void testPolicyResolve1() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyResolve1, PolicyResolveRequest.class);
        assertTrue(rpcMsg instanceof PolicyResolveRequest);
        PolicyResolveRequest opflexRequest = (PolicyResolveRequest)rpcMsg;
        assertTrue(opflexRequest.getId().toString().contains(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(PolicyResolveRequest.RESOLVE_MESSAGE));
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_uri().getValue().equals(POLICY_URI_VALUE.getValue()));
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_ident().getContext().getValue() == "");
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_ident().getName() == "");
        assertTrue(opflexRequest.getParams()
        		.get(0).getPrr() == Integer.parseInt(PRR_VALUE));
        assertTrue(opflexRequest.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));
        assertTrue(opflexRequest.getParams()
                .get(0).getData().equals(DATA_NAME));

    }

    /**
     * Test a policy resolve message that uses he policy identity instead
     * of the URI for passing the policy requested.
     *
     * @throws Exception
     */
    @Test
    public void testPolicyResolve2() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyResolve2, PolicyResolveRequest.class);
        assertTrue(rpcMsg instanceof PolicyResolveRequest);
        PolicyResolveRequest opflexRequest = (PolicyResolveRequest)rpcMsg;
        assertTrue(opflexRequest.getId().toString().contains(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(PolicyResolveRequest.RESOLVE_MESSAGE));
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_uri().getValue() == "");
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_ident().getName().equals(POLICY_IDENT_URI));
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_ident().getContext().getValue()
        		.equals(POLICY_IDENT_CONTEXT_VALUE.getValue()));
        assertTrue(opflexRequest.getParams()
        		.get(0).getPrr() == Integer.parseInt(PRR_VALUE));
        assertTrue(opflexRequest.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));
        assertTrue(opflexRequest.getParams()
                .get(0).getData().equals(DATA_NAME));

    }

    /**
     * Test the response to the policy resolve message
     *
     * @throws Exception
     */
    @Test
    public void testPolicyResolveResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyResponse, PolicyResolveResponse.class);
        assertTrue(rpcMsg instanceof PolicyResolveResponse);
        PolicyResolveResponse opflexResponse = (PolicyResolveResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getResult()
        		.getPolicy().get(0).getChildren().size() == 0);
        assertTrue(opflexResponse.getResult()
        		.getPolicy().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getResult()
        		.getPolicy().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getResult()
        		.getPolicy().get(0).getParent_uri().getValue().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getResult()
        		.getPolicy().get(0).getUri().getValue().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getResult()
                .getPolicy().get(0).getSubject().equals(SUBJECT_NAME));
    }

    /**
     * Test a policy unresolve message that uses the URI instead of the
     * policy identity for passing the policy requested.
     *
     * @throws Exception
     */
    @Test
    public void testPolicyUnresolveRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(opflexPolicyUnresolveRequest);
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyUnresolveRequest, PolicyResolveRequest.class);
        assertTrue(rpcMsg instanceof PolicyResolveRequest);
        PolicyResolveRequest opflexRequest = (PolicyResolveRequest)rpcMsg;
        assertTrue(opflexRequest.getId().toString().contains(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(PolicyUnresolveRequest.UNRESOLVE_MESSAGE));
        System.out.println(POLICY_URI_VALUE.getValue());
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_ident().getContext().getValue().equals(POLICY_IDENT_CONTEXT_VALUE.getValue()));
        assertTrue(opflexRequest.getParams()
        		.get(0).getPolicy_uri().getValue() == "");
        assertTrue(opflexRequest.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));

    }


    @Test
    public void testPolicyUnresolveResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexPolicyUnresolveResponse, PolicyUnresolveResponse.class);
        assertTrue(rpcMsg instanceof PolicyUnresolveResponse);
        PolicyUnresolveResponse opflexResponse = (PolicyUnresolveResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
    }

    @Test
    public void testPolicyUpdateRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(opflexUpdateRequest);
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexUpdateRequest, PolicyUpdateRequest.class);
        assertTrue(rpcMsg instanceof PolicyUpdateRequest);
        PolicyUpdateRequest opflexResponse = (PolicyUpdateRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams().get(0)
        		.getDelete_uri().get(0).getValue().equals(POLICY_URI_VALUE.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
        		.getMerge_children().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getMerge_children().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getMerge_children().get(0).getParent_uri().getValue().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
        		.getMerge_children().get(0).getUri().getValue().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
                .getMerge_children().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getMerge_children().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getMerge_children().get(0).getSubject().equals(SUBJECT_NAME));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getParams().get(0)
        		.getMerge_children().get(0).getChildren().size() == 0);
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getParent_uri().getValue().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getUri().getValue().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
                .getReplace().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getReplace().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getReplace().get(0).getSubject().equals(SUBJECT_NAME));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getChildren().size() == 0);
    }

    @Test
    public void testUpdateResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexUpdateResponse, PolicyUpdateResponse.class);
        assertTrue(rpcMsg instanceof PolicyUpdateResponse);
        PolicyUpdateResponse opflexResponse = (PolicyUpdateResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));

    }


    @Test
    public void testEpDeclareRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpDeclareRequest, EndpointDeclareRequest.class);
        assertTrue(rpcMsg instanceof EndpointDeclareRequest);
        EndpointDeclareRequest opflexResponse = (EndpointDeclareRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams().get(0)
        		.getEndpoint().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getEndpoint().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getEndpoint().get(0).getParent_uri().getValue().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
        		.getEndpoint().get(0).getUri().getValue().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
                .getEndpoint().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getEndpoint().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getEndpoint().get(0).getSubject().equals(SUBJECT_NAME));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getParams().get(0)
        		.getEndpoint().get(0).getChildren().size() == 0);
        assertTrue(opflexResponse.getParams().get(0)
        		.getPrr() == Integer.parseInt(PRR_VALUE));
    }

    @Test
    public void testEpDeclareResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpDeclareResponse, EndpointDeclareResponse.class);
        assertTrue(rpcMsg instanceof EndpointDeclareResponse);
        EndpointDeclareResponse opflexResponse = (EndpointDeclareResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
    }

    @Test
    public void testEndpointUndeclareRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUndeclareRequest, EndpointUndeclareRequest.class);
        assertTrue(rpcMsg instanceof EndpointUndeclareRequest);
        EndpointUndeclareRequest opflexRequest = (EndpointUndeclareRequest)rpcMsg;
        assertTrue(opflexRequest.getId().toString().contains(ID_UUID));
        assertTrue(opflexRequest.getMethod().equals(EndpointUndeclareRequest.UNDECLARE_MESSAGE));
        assertTrue(opflexRequest.getParams()
        		.get(0).getEndpoint_uri().getValue().equals(ENDPOINT_URI_VALUE.getValue()));
        assertTrue(opflexRequest.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));

    }


    @Test
    public void testEpUndeclareResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUndeclareResponse, EndpointUndeclareResponse.class);
        assertTrue(rpcMsg instanceof EndpointUndeclareResponse);
        EndpointUndeclareResponse opflexResponse = (EndpointUndeclareResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
    }

    @Test
    public void testEpResolveRequest1() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpResolveRequest1, EndpointResolveRequest.class);
        assertTrue(rpcMsg instanceof EndpointResolveRequest);
        EndpointResolveRequest opflexResponse = (EndpointResolveRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_uri().getValue().equals(ENDPOINT_URI_VALUE.getValue()));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getContext() == null);
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getIdentifier() == null);
    }


    @Test
    public void testEpResolveRequest2() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(opflexEpResolveRequest2);
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpResolveRequest2, EndpointResolveRequest.class);
        assertTrue(rpcMsg instanceof EndpointResolveRequest);
        EndpointResolveRequest opflexResponse = (EndpointResolveRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_uri().getValue() == "");
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getIdentifier().equals(ENDPOINT_IDENT_URI));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getContext()
        		.getValue().equals(ENDPOINT_IDENT_CONTEXT_VALUE.getValue()));
    }

    @Test
    public void testEpResolveResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpResponse, EndpointResolveResponse.class);
        assertTrue(rpcMsg instanceof EndpointResolveResponse);
        EndpointResolveResponse opflexResponse = (EndpointResolveResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getResult()
        		.getEndpoint().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getResult()
        		.getEndpoint().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getResult()
        		.getEndpoint().get(0).getParent_uri().getValue().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getResult()
        		.getEndpoint().get(0).getUri().getValue().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getResult()
                .getEndpoint().get(0).getSubject().equals(SUBJECT_NAME));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getResult()
        		.getEndpoint().get(0).getChildren().size() == 0);
    }

    @Test
    public void testEpUnresolveRequest1() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUnresolveRequest1, EndpointUnresolveRequest.class);
        assertTrue(rpcMsg instanceof EndpointUnresolveRequest);
        EndpointUnresolveRequest opflexResponse = (EndpointUnresolveRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_uri().getValue().equals(ENDPOINT_URI_VALUE.getValue()));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getContext() == null);
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getIdentifier() == null);
    }

    @Test
    public void testEpUnresolveRequest2() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUnresolveRequest2, EndpointUnresolveRequest.class);
        assertTrue(rpcMsg instanceof EndpointUnresolveRequest);
        EndpointUnresolveRequest opflexResponse = (EndpointUnresolveRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams()
                .get(0).getSubject().equals(SUBJECT_NAME));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_uri().getValue() == "");
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getIdentifier().equals(ENDPOINT_IDENT_URI));
        assertTrue(opflexResponse.getParams()
        		.get(0).getEndpoint_ident().getContext()
        		.getValue().equals(ENDPOINT_IDENT_CONTEXT_VALUE.getValue()));
    }

    @Test
    public void testEpUnresolveResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUnresolveResponse, EndpointUnresolveResponse.class);
        assertTrue(rpcMsg instanceof EndpointUnresolveResponse);
        EndpointUnresolveResponse opflexResponse = (EndpointUnresolveResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
    }

    @Test
    public void testEpUpdateRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUpdateRequest, EndpointUpdateRequest.class);
        assertTrue(rpcMsg instanceof EndpointUpdateRequest);
        EndpointUpdateRequest opflexResponse = (EndpointUpdateRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getParent_uri().getValue().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getUri().getValue().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
                .getReplace().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getReplace().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getReplace().get(0).getSubject().equals(SUBJECT_NAME));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getParams().get(0)
        		.getReplace().get(0).getChildren().size() == 0);
        assertTrue(opflexResponse.getParams().get(0)
        		.getDelete_uri().get(0).getValue().equals(POLICY_URI_VALUE.getValue()));
    }


    @Test
    public void testEpUpdateResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexEpUpdateResponse, EndpointUpdateResponse.class);
        assertTrue(rpcMsg instanceof EndpointUpdateResponse);
        EndpointUpdateResponse opflexResponse = (EndpointUpdateResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
    }


    //@Test
    public void testStateRequest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexStateRequest, StateReportRequest.class);
        assertTrue(rpcMsg instanceof StateReportRequest);
        StateReportRequest opflexResponse = (StateReportRequest)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
        assertTrue(opflexResponse.getParams().get(0)
        		.getObservable().get(0).getParent_relation().equals(PARENT_RELATION_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getObservable().get(0).getParent_subject().equals(PARENT_SUBJECT_NAME));
        assertTrue(opflexResponse.getParams().get(0)
        		.getObservable().get(0).getParent_uri().equals(PARENT_URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
        		.getObservable().get(0).getUri().equals(URI_NAME.getValue()));
        assertTrue(opflexResponse.getParams().get(0)
                .getObservable().get(0).getProperties().get(0).getName().equals(PROP_NAME));
        assertTrue(opflexResponse.getParams().get(0)
                .getObservable().get(0).getProperties().get(0).getData().asText().equals(PROP_DATA));
        assertTrue(opflexResponse.getParams().get(0)
                .getObservable().get(0).getSubject().equals(SUBJECT_NAME));
        // TODO: add support for testing children?
        assertTrue(opflexResponse.getParams().get(0)
        		.getObservable().get(0).getChildren() == null);
        assertTrue(opflexResponse.getParams().get(0)
        		.getObject().getValue().equals(OBJECT_VALUE.getValue()));
    }

    //@Test
    public void testStateResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RpcMessage rpcMsg = objectMapper.
                readValue(opflexStateResponse, StateReportResponse.class);
        assertTrue(rpcMsg instanceof StateReportResponse);
        StateReportResponse opflexResponse = (StateReportResponse)rpcMsg;
        assertTrue(opflexResponse.getId().toString().contains(ID_UUID));
    }



}
