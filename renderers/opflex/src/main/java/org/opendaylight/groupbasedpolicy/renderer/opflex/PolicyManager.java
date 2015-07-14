/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.groupbasedpolicy.renderer.opflex.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.renderer.opflex.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.renderer.opflex.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.renderer.opflex.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.OpflexAgent;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.OpflexConnectionService;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.Role;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.OpflexError;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyResolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyResolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUnresolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUnresolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.PolicyUpdateRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.MitLib;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyUri;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.Policy;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Manage policies on agents by subscribing to updates from the
 * policy resolver and information about endpoints from the endpoint
 * registry
 * 
 * @author tbachman
 */
public class PolicyManager implements PolicyListener, RpcBroker.RpcCallback, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManager.class);

    private static final String UKNOWN_POLICY = "unknown policy name";

    /*
     * The tables below are used to look up Managed Objects (MOs)
     * that have been subscribed to. The table is indexed as
     * <String:Managed Object DN> <String:agent ID> <Policy:policy>
     */
    final PolicyResolver policyResolver;
    final OpflexConnectionService connectionService;
    final ScheduledExecutorService executor;
    private final MitLib mitLibrary;
    private final PolicyScope policyScope;

    private final ConcurrentHashMap<EgKey, Set<String>> epgSubscriptions = new ConcurrentHashMap<>();
    private RpcMessageMap messageMap = null;

    public PolicyManager(PolicyResolver policyResolver, OpflexConnectionService connectionService,
            ScheduledExecutorService executor, MitLib mitLibrary) {
        super();
        this.executor = executor;
        this.policyResolver = policyResolver;
        this.connectionService = connectionService;
        this.mitLibrary = mitLibrary;

        /* Subscribe to PR messages */
        messageMap = new RpcMessageMap();
        List<RpcMessage> messages = Role.POLICY_REPOSITORY.getMessages();
        messageMap.addList(messages);
        for (RpcMessage msg : messages) {
            this.connectionService.subscribe(msg, this);
        }

        policyScope = policyResolver.registerListener(this);

        LOG.debug("Initialized OpFlex policy manager");
    }

    /**
     * Shut down the {@link PolicyManager}. Implemented from the
     * AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {

    }

    // **************
    // PolicyListener
    // **************

    @Override
    public void policyUpdated(Set<EgKey> updatedConsumers) {

        sendPolicyUpdates(updatedConsumers);
    }

    // **************
    // Implementation
    // **************

    /**
     * Update all policy on all agents as needed. Note that this will block
     * one of the threads on the executor.
     * 
     * @author tbachman
     */
    private void sendPolicyUpdates(Set<EgKey> updatedConsumers) {
        Map<String, Set<EgKey>> agentMap = new HashMap<String, Set<EgKey>>();

        PolicyInfo info = policyResolver.getCurrentPolicy();
        if (info == null)
            return;

        /*
         * First build a per-agent set of EPGs that need updating
         */
        for (EgKey cepg : updatedConsumers) {

            /*
             * Find the set of agents that have subscribed to
             * updates for this EPG
             */
            for (String agentId : epgSubscriptions.get(cepg)) {
                Set<EgKey> egSet = agentMap.get(agentId);
                if (egSet == null) {
                    egSet = Collections.newSetFromMap(new ConcurrentHashMap<EgKey, Boolean>());
                    agentMap.put(agentId, egSet);
                }
                egSet.add(cepg);
            }
        }

        /*
         * Go through each agent and provide a single update for all EPGs
         */
        for (Map.Entry<String, Set<EgKey>> entry : agentMap.entrySet()) {
            OpflexAgent agent = connectionService.getOpflexAgent(entry.getKey());
            if (agent == null)
                continue;

            sendPolicyUpdate(agent.getEndpoint(), entry.getValue(), info);

        }
    }

    /**
     * This implements Runnable, which allows the {@link ScheduledExecutorService} to execute the
     * run() method to implement the update
     *
     * @author tbachman
     */
    private class PolicyUpdate implements Runnable {

        private final JsonRpcEndpoint agent;
        private final Set<EgKey> epgSet;
        private final PolicyInfo info;

        PolicyUpdate(JsonRpcEndpoint agent, Set<EgKey> epgSet, PolicyInfo info) {
            this.agent = agent;
            this.epgSet = epgSet;
            this.info = info;
        }

        @Override
        public void run() {
            List<ManagedObject> subtrees = new ArrayList<ManagedObject>();

            PolicyUpdateRequest request = new PolicyUpdateRequest();
            List<PolicyUpdateRequest.Params> paramsList = new ArrayList<>();
            PolicyUpdateRequest.Params params = new PolicyUpdateRequest.Params();

            /*
             * We may need to optimize this in the future. Currently
             * we send down the EPG MOs and all the related policy
             * that's in scope from the PolicyResolver. If we want
             * to optimize this in the future to only send the policy
             * objects that changed, we'd either have to change the
             * PolicyResolver to provide this delta, or we'd have to
             * keep cached state for each node.
             */
            for (EgKey epg : epgSet) {
                /*
                 * Get EPGs from the IndexedTenant, as the EPGs from
                 * the IndexedTenenat alread has collapsed the EPGs
                 * (i.e. inheritance accounted for)
                 * 
                 * TODO: needed?
                 */

                IndexedTenant it = policyResolver.getTenant(epg.getTenantId());
                List<ManagedObject> relatedMos = getPolicy(epg, info, it);
                subtrees.addAll(relatedMos);
            }

            /*
             * Currently not using delete URI or merge_children MOs
             */
            params.setDelete_uri(new ArrayList<Uri>());
            params.setMerge_children(new ArrayList<ManagedObject>());
            params.setReplace(subtrees);
            paramsList.add(params);
            request.setParams(paramsList);
            try {
                agent.sendRequest(request);
            } catch (Exception e) {

            }
        }
    }

    void sendPolicyUpdate(JsonRpcEndpoint agent, Set<EgKey> epgSet, PolicyInfo info) {
        executor.execute(new PolicyUpdate(agent, epgSet, info));
    }

    /**
     * This method creates {@link ManagedObject} POJOs for all of the
     * policy objects that need to be sent as a result of policy
     * resolution for the given EPG.
     *
     * @param epg The Endpoint Group that was resolved
     * @param policySnapshot A snapshot of the current resolved policy
     */
    private List<ManagedObject> getPolicy(EgKey epg, PolicyInfo policySnapshot, IndexedTenant it) {
        if (policySnapshot == null)
            return null;

        Set<ManagedObject> policyMos = Sets.newHashSet();
        Set<EgKey> peers = policySnapshot.getPeers(epg);

        if (peers == null || peers.size() <= 0)
            return null;

        // Allocate an MO for the requested EPG
        ManagedObject epgMo = new ManagedObject();
        for (EgKey depg : peers) {
            /*
             * Construct the base URI, so that we can
             * continue adding on to create child MOs.
             * We use the peer EPG for getting the policy
             */
            PolicyUri uri = new PolicyUri();
            uri.push(MessageUtils.TENANTS_RN);
            uri.push(MessageUtils.TENANT_RN);
            uri.push(depg.getTenantId().getValue());

            Policy policy = policySnapshot.getPolicy(epg, depg);
            if (policy == null || policy == Policy.EMPTY)
                continue;

            /*
             * We now have a policy that we need to send to the agent.
             * Provide empty condition lists for now - need to be
             * an actual empty list, instead of null
             * 
             * TODO: get actual condition groups
             */
            List<ConditionName> conds = new ArrayList<ConditionName>();
            ConditionGroup cgSrc = policySnapshot.getEgCondGroup(epg, conds);
            ConditionGroup cgDst = policySnapshot.getEgCondGroup(depg, conds);
            List<RuleGroup> rgl = policy.getRules(cgSrc, cgDst);

            /*
             * RuleGroups can refer to the same contract. As result,
             * we need to keep track of contracts returned and merge
             * the results into a single ManagedObject
             */
            Map<Contract, ManagedObject> contracts = new ConcurrentHashMap<Contract, ManagedObject>();

            for (RuleGroup rg : rgl) {

                /*
                 * Construct a new URI for the EPG requested.
                 * In this case, we want the requested EPG, not
                 * the peer EPG
                 */
                PolicyUri puri = new PolicyUri();
                puri.push(MessageUtils.TENANTS_RN);
                puri.push(MessageUtils.TENANT_RN);
                puri.push(epg.getTenantId().getValue());
                puri.push(MessageUtils.EPG_RN);
                puri.push(epg.getEgId().getValue());
                Set<ManagedObject> epgMos = MessageUtils.getEndpointGroupMo(epgMo, puri,
                        it.getEndpointGroup(epg.getEgId()), rg);
                if (epgMos != null) {
                    policyMos.addAll(epgMos);
                }

                Contract c = rg.getRelatedContract();
                /*
                 * This cmol list is used as a container to pass
                 * an out parameter for the contract MO. This MO
                 * is returned separately from the others because
                 * may require merging -- different RuleGroup
                 * objects can refer to the same contract
                 */
                List<ManagedObject> cmol = new ArrayList<>();

                uri.push(MessageUtils.CONTRACT_RN);
                uri.push(c.getId().getValue());
                List<ManagedObject> mol = MessageUtils.getContractAndSubMos(cmol, uri, c, rg, it);
                if (mol == null)
                    continue;

                // walk back to the tenant for next contract URI
                uri.pop();
                uri.pop();

                if (contracts.get(c) != null) {
                    /*
                     * Aggregate the child URIs and properties.
                     */
                    MessageUtils.mergeMos(contracts.get(c), cmol.remove(0));
                } else {
                    contracts.put(c, cmol.remove(0));
                }
                policyMos.addAll(mol);
            }
            // add in the EPG
            policyMos.add(epgMo);
            // add in the contracts
            policyMos.addAll(contracts.values());
        }
        return Lists.newArrayList(policyMos);
    }

    private void addPolicySubscription(JsonRpcEndpoint endpoint, EgKey epgId) {
        policyScope.addToScope(epgId.getTenantId(), epgId.getEgId());

        Set<String> agents = epgSubscriptions.get(epgId);
        if (agents == null) {
            agents = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            Set<String> result = epgSubscriptions.putIfAbsent(epgId, agents);
            if (result != null) {
                agents = result;
            }
        }
        agents.add(endpoint.getIdentifier());

    }

    private void removePolicySubscription(JsonRpcEndpoint endpoint, EgKey epgId) {
        Set<String> agents = epgSubscriptions.get(epgId);
        if (agents != null) {
            agents.remove(endpoint.getIdentifier());
        }
        policyScope.removeFromScope(epgId.getTenantId(), epgId.getEgId());
    }

    @Override
    public void callback(JsonRpcEndpoint endpoint, RpcMessage request) {

        if (messageMap.get(request.getMethod()) == null) {
            LOG.warn("message {} was not subscribed to, but was delivered.", request);
            return;
        }

        RpcMessage response = null;

        if (request instanceof PolicyResolveRequest) {
            PolicyResolveRequest req = (PolicyResolveRequest) request;
            PolicyResolveResponse msg = new PolicyResolveResponse();
            PolicyResolveResponse.Result result = new PolicyResolveResponse.Result();
            msg.setId(request.getId());

            if (!req.valid()) {
                LOG.warn("Invalid resolve request: {}", req);
                OpflexError error = new OpflexError();
                error.setCode(OpflexError.ErrorCode.ERROR.toString());
                // error.setData(data);
                // error.setMessage(message);
                // error.setTrace(trace);
                msg.setError(error);
            }

            List<ManagedObject> mol = new ArrayList<>();

            for (PolicyResolveRequest.Params p : req.getParams()) {

                // Skip this if we already have an error
                if (msg.getError() != null)
                    break;

                /*
                 * Only Policy Identity or Policy URI is present.
                 * Convert Policy Identities to a URI that we can use
                 */
                Uri policyUri = p.getPolicy_uri();
                if (policyUri == null) {
                    Uri rn = p.getPolicy_ident().getContext();
                    String name = p.getPolicy_ident().getName();
                    PolicyUri puri = new PolicyUri(rn.getValue());
                    puri.push(name);
                    policyUri = puri.getUri();
                }

                // See if the request has an EPG in the URI
                if (MessageUtils.hasEpg(policyUri.getValue())) {
                    /*
                     * Keep track of EPGs requested by agents.
                     */
                    EndpointGroupId egid = new EndpointGroupId(
                            MessageUtils.getEndpointGroupFromUri(policyUri.getValue()));
                    TenantId tid = new TenantId(MessageUtils.getTenantFromUri(policyUri.getValue()));
                    EgKey epgId = new EgKey(tid, egid);

                    addPolicySubscription(endpoint, epgId);

                    IndexedTenant it = policyResolver.getTenant(tid);
                    if (it != null) {
                        List<ManagedObject> relatedMos = getPolicy(epgId, policyResolver.getCurrentPolicy(), it);
                        if (relatedMos != null) {
                            mol.addAll(relatedMos);
                        }
                    }
                } else {
                    OpflexError error = new OpflexError();
                    error.setMessage(UKNOWN_POLICY);
                    error.setCode(OpflexError.ErrorCode.EUNSUPPORTED.toString());
                    // error.setData(data);
                    // error.setTrace(trace);
                    msg.setError(error);
                }

            }
            result.setPolicy(mol);
            msg.setResult(result);
            response = msg;
        } else if (request instanceof PolicyUnresolveRequest) {
            PolicyUnresolveRequest req = (PolicyUnresolveRequest) request;
            PolicyUnresolveResponse msg = new PolicyUnresolveResponse();
            msg.setId(request.getId());
            Uri policyUri;

            if (!req.valid()) {
                OpflexError error = new OpflexError();
                error.setCode(OpflexError.ErrorCode.ERROR.toString());
                // error.setData(data);
                // error.setMessage(message);
                // error.setTrace(trace);
                msg.setError(error);
            }

            for (PolicyUnresolveRequest.Params p : req.getParams()) {

                // Skip this if we already have an error
                if (msg.getError() != null)
                    break;

                /*
                 * Only Policy Identity or Policy URI is present.
                 * Convert to a URI that we'll use
                 */
                policyUri = p.getPolicy_uri();
                if (policyUri == null) {
                    // Convert the RN/name to DN
                    Uri rn = p.getPolicy_ident().getContext();
                    String name = p.getPolicy_ident().getName();
                    PolicyUri puri = new PolicyUri(rn.getValue());
                    puri.push(name);
                    policyUri = puri.getUri();
                }

                if (MessageUtils.hasEpg(policyUri.getValue())) {
                    /*
                     * Keep track of EPGs requested by agents.
                     */
                    EndpointGroupId egid = new EndpointGroupId(
                            MessageUtils.getEndpointGroupFromUri(policyUri.getValue()));
                    TenantId tid = new TenantId(MessageUtils.getTenantFromUri(policyUri.getValue()));
                    EgKey epgId = new EgKey(tid, egid);

                    removePolicySubscription(endpoint, epgId);
                } else {
                    OpflexError error = new OpflexError();
                    error.setMessage(UKNOWN_POLICY);
                    msg.setError(error);
                }
            }
            response = msg;

        }
        if (response != null) {
            try {
                endpoint.sendResponse(response);
            } catch (Exception e) {
                LOG.warn("Error sending response {}", e);
            }
        }

    }
}
