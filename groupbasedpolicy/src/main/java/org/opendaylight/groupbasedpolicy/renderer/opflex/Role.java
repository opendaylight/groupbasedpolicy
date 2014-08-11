/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointDeclarationResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.EndpointRequestResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.IdentityResponse;

/**
 * Enum for OpFlex roles and their supported messages
 *
 * @author tbachman
 *
 */
public enum Role {
    POLICY_REPOSITORY("policy_repository"),
    ENDPOINT_REGISTRY("endpoint_registry"),
    OBSERVER("observer"),
    POLICY_ELEMENT("policy_element");

    static IdentityRequest idReq = new IdentityRequest();
    static IdentityResponse idRsp = new IdentityResponse();
    static EndpointDeclarationRequest epDeclReq = new EndpointDeclarationRequest();
    static EndpointDeclarationResponse epDeclRsp = new EndpointDeclarationResponse();
    static EndpointRequestRequest epReqReq = new EndpointRequestRequest();
    static EndpointRequestResponse epReqRsp = new EndpointRequestResponse();

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
        if (role.equals(POLICY_REPOSITORY.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            msgList.add(idReq);
            msgList.add(idRsp);
            return msgList;
        }
        else if (role.equals(ENDPOINT_REGISTRY.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            msgList.add(idReq);
            msgList.add(idRsp);
            msgList.add(epDeclReq);
            msgList.add(epReqReq);
            return msgList;
        }
        else if (role.equals(OBSERVER.toString())) {
            List<RpcMessage> msgList = new ArrayList<RpcMessage>();
            msgList.add(idReq);
            msgList.add(idRsp);
            return msgList;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.role;
    }
}