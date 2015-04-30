/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.lib;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.renderer.opflex.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointDeclareResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.EndpointUndeclareRequest;
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

/**
 * Enum for OpFlex roles and their supported messages
 *
 * @author tbachman
 */
public enum Role {
    DISCOVERY("discovery"), POLICY_REPOSITORY("policy_repository"), ENDPOINT_REGISTRY("endpoint_registry"), OBSERVER(
            "observer"), POLICY_ELEMENT("policy_element");

    static final IdentityRequest idReq = new IdentityRequest();
    static final IdentityResponse idRsp = new IdentityResponse();
    static final EndpointDeclareRequest epDeclReq = new EndpointDeclareRequest();
    static final EndpointDeclareResponse epDeclRsp = new EndpointDeclareResponse();
    static final EndpointUndeclareRequest epUndeclReq = new EndpointUndeclareRequest();
    static final EndpointResolveRequest epReqReq = new EndpointResolveRequest();
    static final EndpointResolveResponse epReqRsp = new EndpointResolveResponse();
    static final EndpointUnresolveRequest epUnreqReq = new EndpointUnresolveRequest();
    static final EndpointUnresolveResponse epUnreqRsp = new EndpointUnresolveResponse();
    static final EndpointUpdateRequest epPolUpdReq = new EndpointUpdateRequest();
    static final EndpointUpdateResponse epPolUpdRsp = new EndpointUpdateResponse();
    static final PolicyResolveRequest polReq = new PolicyResolveRequest();
    static final PolicyResolveResponse polRsp = new PolicyResolveResponse();
    static final PolicyUpdateRequest polUpdReq = new PolicyUpdateRequest();
    static final PolicyUpdateResponse polUpdRsp = new PolicyUpdateResponse();
    static final PolicyUnresolveRequest polUnReq = new PolicyUnresolveRequest();
    static final PolicyUnresolveResponse polUnRsp = new PolicyUnresolveResponse();

    private final String role;

    Role(String role) {
        this.role = role;
    }

    /**
     * Get the {@link RpcMessage}s supported by this Role
     *
     * @return List of RpcMessages supported for this Role
     */
    public List<RpcMessage> getMessages() {
        if (role.equals(DISCOVERY.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            msgList.add(idReq);
            msgList.add(idRsp);
            return msgList;
        } else if (role.equals(POLICY_REPOSITORY.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            msgList.add(polReq);
            msgList.add(polUpdReq);
            msgList.add(polUpdRsp);
            msgList.add(polUnReq);
            msgList.add(polUnRsp);
            return msgList;
        } else if (role.equals(ENDPOINT_REGISTRY.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            msgList.add(epDeclReq);
            msgList.add(epUndeclReq);
            msgList.add(epReqReq);
            msgList.add(epUnreqReq);
            msgList.add(epPolUpdReq);
            msgList.add(epPolUpdRsp);
            return msgList;
        } else if (role.equals(OBSERVER.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            return msgList;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.role;
    }
}
