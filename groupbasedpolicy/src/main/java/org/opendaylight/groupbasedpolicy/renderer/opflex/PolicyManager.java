/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.groupbasedpolicy.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcBroker;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessage;
import org.opendaylight.groupbasedpolicy.jsonrpc.RpcMessageMap;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.ManagedObject;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyResolutionRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyResolutionResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyUnresolveRequest;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyUnresolveResponse;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.PolicyUpdateRequest;
import org.opendaylight.groupbasedpolicy.resolver.ConditionGroup;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.resolver.IndexedTenant;
import org.opendaylight.groupbasedpolicy.resolver.Policy;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyListener;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.groupbasedpolicy.resolver.PolicyScope;
import org.opendaylight.groupbasedpolicy.resolver.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayConfig.LearningMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Manage policies on agents by subscribing to updates from the
 * policy resolver and information about endpoints from the endpoint
 * registry
 * @author tbachman
 */
public class PolicyManager
     implements PolicyListener, RpcBroker.RpcCallback {
    private static final Logger LOG =
            LoggerFactory.getLogger(PolicyManager.class);
    /*
     * The tables below are used to look up Managed Objects (MOs)
     * that have been subscribed to. The table is indexed as
     * <String:Managed Object DN> <String:agent ID> <Policy:policy>
     */
    Table<String, String, Boolean> moMap;
    Table<String, EgKey, Boolean> agentEpgTable;
    final PolicyResolver policyResolver;
    final OpflexConnectionService connectionService;

    private final PolicyScope policyScope;
    private ConcurrentHashMap<EgKey, Set<String>> epgSubscriptions =
            new ConcurrentHashMap<>();

    final ScheduledExecutorService executor;
    private RpcMessageMap messageMap = null;

    // TODO: FIXME
    private static final int DEFAULT_PRR = 1000;
    private static final String UKNOWN_POLICY = "unknown policy name";

    public PolicyManager(PolicyResolver policyResolver,
                         OpflexConnectionService connectionService,
                         ScheduledExecutorService executor) {
        super();
        this.executor = executor;
        this.policyResolver = policyResolver;
        this.connectionService = connectionService;


        /* Subscribe to PR messages */
        messageMap = new RpcMessageMap();
        List<RpcMessage> messages = Role.POLICY_REPOSITORY.getMessages();
        messageMap.addList(messages);
        for (RpcMessage msg: messages) {
            this.connectionService.subscribe(msg, this);
        }

        policyScope = policyResolver.registerListener(this);

        moMap = HashBasedTable.create();
        agentEpgTable = HashBasedTable.create();

        LOG.debug("Initialized OpFlex policy manager");
    }

    // **************
    // PolicyListener
    // **************

    @Override
    public void policyUpdated(Set<EgKey> updatedConsumers) {

        sendPolicyUpdates(updatedConsumers);
    }

    // *************
    // PolicyManager
    // *************

    /**
     * Set the learning mode to the specified value
     * @param learningMode the learning mode to set
     */
    public void setLearningMode(LearningMode learningMode) {
        // No-op for now
    }

    // **************
    // Implementation
    // **************

    /**
     * Update all policy on all agents as needed.  Note that this will block
     * one of the threads on the executor.
     * @author tbachman
     */
    private void sendPolicyUpdates(Set<EgKey> updatedConsumers) {
        Map<String, Set<EgKey>> agentMap = new HashMap<String, Set<EgKey>>();

        PolicyInfo info = policyResolver.getCurrentPolicy();
        if (info == null) return;

        /*
         * First build a per-agent set of EPGs that need updating
         */
        for (EgKey cepg: updatedConsumers) {
            for (String agentId: epgSubscriptions.get(cepg)) {
                Set<EgKey> egSet = agentMap.get(agentId);
                if (egSet == null) {
                    egSet =
                          Collections.
                          newSetFromMap(new ConcurrentHashMap<EgKey, Boolean>());
                }
                egSet.add(cepg);
                agentMap.put(agentId, egSet);
            }
        }

        /*
         * Go through each agent and provide a single update for all EPGs
         */
        for (Map.Entry<String,Set<EgKey>> entry: agentMap.entrySet()) {
            OpflexAgent agent = connectionService.
                    getOpflexAgent(entry.getKey());
            if (agent == null) continue;

            sendPolicyUpdate(agent.getEndpoint(), entry.getValue());

        }
    }

    private class PolicyUpdate implements Runnable {
        private final JsonRpcEndpoint agent;
        private final Set<EgKey> epgSet;
        PolicyUpdate(JsonRpcEndpoint agent, Set<EgKey> epgSet) {
            this.agent = agent;
            this.epgSet = epgSet;
        }
        @Override
        public void run() {
            List<ManagedObject> subtrees =
                    new ArrayList<ManagedObject>();

            PolicyUpdateRequest request =
                    new PolicyUpdateRequest();
            List<PolicyUpdateRequest.Params> paramsList =
                    new ArrayList<>();
            PolicyUpdateRequest.Params params =
                    new PolicyUpdateRequest.Params();

            for (EgKey epg: epgSet) {
                IndexedTenant it = policyResolver.getTenant(epg.getTenantId());
                ManagedObject epgMo = MessageUtils.
                            getEndpointGroupMo(it.getEndpointGroup(epg.getEgId()));
                List<ManagedObject> relatedMos =
                        getPolicy(epg, policyResolver.getCurrentPolicy(), it);

                if (epgMo != null && relatedMos != null && relatedMos.size() > 0) {
                    epgMo.setTo_relations(relatedMos);
                }
                subtrees.add(epgMo);
            }

            //params.setContext();
            params.setPrr(DEFAULT_PRR);
            params.setSubtree(subtrees);
            paramsList.add(params);
            request.setParams(paramsList);
            try {
                agent.sendRequest(request);
            }
            catch (Throwable t) {

            }
        }
    }

    void sendPolicyUpdate(JsonRpcEndpoint agent, Set<EgKey> epgSet) {
        executor.execute(new PolicyUpdate(agent, epgSet));
    }




    // TODO: clean this up
    /**
     * This method creates {@link ManagedObject} POJOs for all of the
     * policy objects that need to be sent as a result of policy
     * resolution for the given EPG.
     *
     * @param epg The Endpoint Group that was resolved
     * @param allPolicy A snapshot of the current resolved policy
     */
    private List<ManagedObject> getPolicy(EgKey epg,
            PolicyInfo allPolicy, IndexedTenant it) {
        if (allPolicy == null) return null;

        List<ManagedObject> children = new ArrayList<ManagedObject>();


        Set<EgKey> peers = allPolicy.getPeers(epg);
        if (peers == null || peers.size() <= 0) return null;

        for (EgKey depg: peers) {
            Map<Contract, ManagedObject> contracts =
                    new ConcurrentHashMap<Contract, ManagedObject>();
            Policy policy = allPolicy.getPolicy(epg, depg);
            if (policy == null || policy == Policy.EMPTY) continue;

            /*
             * We now have a policy that we need to send to the agent.
             * However, we have to construct the set of related MOs.
             * Also, this MO belongs to the set of MOs for the EPGs.
             */

            /*
             * get all the conditions for the EP. The EP is declared
             * in the "on behalf of" field
             */
            // TODO: get actual condition groups
            List<ConditionName> conds = new ArrayList<ConditionName>();
            ConditionGroup cgSrc = allPolicy.getEgCondGroup(epg, conds);
            ConditionGroup cgDst = allPolicy.getEgCondGroup(depg, conds);
            List<RuleGroup> rgl = policy.getRules(cgSrc, cgDst);

            /*
             * RuleGroups can refer to the same contract. As result,
             * we want to collect all of the subelements under the
             * same contract.
             */
            for (RuleGroup rg: rgl) {
                List<ManagedObject> contractChildren =
                        new ArrayList<ManagedObject>();
                List<ManagedObject> subjectChildren =
                        new ArrayList<ManagedObject>();

                ManagedObject cmo = null, smo = null;
                Contract c = rg.getRelatedContract();

                cmo = MessageUtils.getContractMo(c);
                if (cmo == null) return null;


                /*
                 * Get the subject for this contract
                 */
                SubjectName sn = rg.getRelatedSubject();
                for (Subject s: c.getSubject()) {
                    if (s.getName().toString().equals(sn.toString())) {
                        smo = MessageUtils.getSubjectMo(s);
                        if (smo == null)  return null;

                        /*
                         * Add this subject as a child MO to the
                         * contract.
                         */
                        if (!contractChildren.contains(smo)) {
                            contractChildren.add(smo);
                            cmo.setChildren(contractChildren);
                            break;
                        }
                    }
                }

                if (smo != null) {
                    List<ManagedObject> subjectFeatureMos =
                            new ArrayList<ManagedObject>();
                    /*
                     * Each subject has a set of resolved rules.
                     * Add those as children.
                     */
                    for (Rule r: rg.getRules()) {
                        /*
                         * For rules, we need to get the actual
                         * classifier and actions from their references
                         */
                        if (r.getClassifierRef() != null) {
                            for (ClassifierRef cr: r.getClassifierRef()) {
                                ClassifierInstance ci = it.getClassifier(cr.getName());
                                if (ci != null) {
                                    subjectFeatureMos.
                                    add(MessageUtils.getClassifierInstanceMo(ci));
                                }
                            }
                        }
                        if (r.getActionRef() != null) {
                            for (ActionRef ar: r.getActionRef()) {
                                ActionInstance ai = it.getAction(ar.getName());
                                if (ai != null) {
                                    subjectFeatureMos.
                                    add(MessageUtils.getActionInstanceMo(ai));
                                }
                            }
                        }
                        subjectChildren.add(MessageUtils.getRuleMo(r));
                    }
                    if (subjectChildren.size() > 0) {
                        smo.setChildren(subjectChildren);
                    }
                    if (subjectFeatureMos.size() > 0) {
                        smo.setTo_relations(subjectFeatureMos);
                    }
                }
                ManagedObject contractMo = contracts.get(c);
                if(contractMo != null) {
                    /*
                     * Aggregate the child objects
                     */
                    List<ManagedObject> deps =
                            contractMo.getChildren();
                    deps.addAll(cmo.getChildren());
                    contractMo.setChildren(deps);
                }
                else {
                    contracts.put(c, cmo);
                    children.add(cmo);
                }
            }
        }
        return children;
    }




    // TODO: clean this up
    @Override
    public void callback(JsonRpcEndpoint endpoint, RpcMessage request) {

        if (messageMap.get(request.getMethod()) == null) {
            LOG.warn("message {} was not subscribed to, but was delivered.", request);
            return;
        }

        if (request instanceof PolicyResolutionRequest) {
            ManagedObject epgMo = null;
            PolicyResolutionRequest req = (PolicyResolutionRequest)request;
            PolicyResolutionResponse msg = new PolicyResolutionResponse();
            msg.setId(request.getId());

            if (!req.valid()) {
                LOG.warn("Invalid resolve request: {}", req);
                // TODO: should return error reply?
                return;
            }
            PolicyResolutionRequest.Params params =
                    req.getParams().get(0);

            String policyUri = params.getPolicy_name();

            if (!moMap.contains(endpoint.getIdentifier(), policyUri)) {
                /*
                 * Add this to the list of tracked MOs
                 */
                moMap.put(endpoint.getIdentifier(), params.getPolicy_name(), true);
            }


            List<ManagedObject> relatedMos = null;

            if (MessageUtils.hasEpg(policyUri)) {
                /*
                 * Keep track of EPGs requested by agents.
                 */
                EndpointGroupId egid = new EndpointGroupId(MessageUtils.
                        getEndpointGroupFromUri(policyUri));
                TenantId tid = new TenantId(MessageUtils.
                        getTenantFromUri(policyUri));
                EgKey epgId = new EgKey(tid, egid);
                policyScope.addToScope(epgId.getTenantId(), epgId.getEgId());

                Set<String> agents = epgSubscriptions.get(epgId);
                if (agents == null) {
                    agents = Collections.
                            newSetFromMap(new ConcurrentHashMap<String, Boolean>());
                }
                agents.add(endpoint.getIdentifier());
                epgSubscriptions.put(epgId, agents);

                IndexedTenant it = policyResolver.getTenant(tid);
                if (it != null) {

                    relatedMos =
                            getPolicy(epgId, policyResolver.getCurrentPolicy(), it);

                    epgMo = MessageUtils.
                            getEndpointGroupMo(it.getEndpointGroup(epgId.getEgId()));
                    if (epgMo != null && relatedMos != null && relatedMos.size() > 0) {
                        epgMo.setTo_relations(relatedMos);
                    }
                }
            }
            else {
                PolicyResolutionResponse.Error error =
                        new PolicyResolutionResponse.Error();
                error.setMessage(UKNOWN_POLICY);
                msg.setError(error);
            }


            /*
             * TODO: other fields we'll want/need
            PolicyResolutionRequest.Params params = req.getParams().get(0);
            params.getContext();
            params.getOn_behalf_of();
            params.getSubject();
            params.getData();
            */
            PolicyResolutionResponse.Result result =
                    new PolicyResolutionResponse.Result();

            result.setPrr(DEFAULT_PRR);
            if (epgMo != null) {
                List<ManagedObject> moList = new ArrayList<ManagedObject>();
                moList.add(epgMo);
                result.setPolicy(moList);
            }
            msg.setResult(result);
            try {
                endpoint.sendResponse(msg);
            }
            catch (Throwable t) {

            }
            /*
             * Use the first identifier to determine the type of
             * identifier being passed to us, so we can install the
             * EP into the appropriate EPR list
             */
        }
        else if (request instanceof PolicyUnresolveRequest) {
            PolicyUnresolveRequest req = (PolicyUnresolveRequest)request;
            PolicyUnresolveResponse msg = new PolicyUnresolveResponse();
            msg.setId(request.getId());

            if (!req.valid()) {
                LOG.warn("Invalid unresolve request: {}", req);
                // TODO: should return error reply?
                return;
            }
            PolicyUnresolveRequest.Params params =
                    req.getParams().get(0);
            String policyUri = params.getPolicy_name();

            if (MessageUtils.hasEpg(policyUri)) {
                /*
                 * Keep track of EPGs requested by agents.
                 */
                EndpointGroupId egid = new EndpointGroupId(MessageUtils.
                        getEndpointGroupFromUri(policyUri));
                TenantId tid = new TenantId(MessageUtils.
                        getTenantFromUri(policyUri));
                EgKey epgId = new EgKey(tid, egid);
                Set<String> agents = epgSubscriptions.get(epgId);
                if (agents == null) {
                    agents = Collections.
                            newSetFromMap(new ConcurrentHashMap<String, Boolean>());
                }
                agents.remove(endpoint.getIdentifier());
                epgSubscriptions.put(epgId, agents);
                policyScope.removeFromScope(epgId.getTenantId(), epgId.getEgId());
            }
            else {
                PolicyUnresolveResponse.Error error =
                        new PolicyUnresolveResponse.Error();
                error.setMessage(UKNOWN_POLICY);
                msg.setError(error);
            }

            try {
                endpoint.sendResponse(msg);
            }
            catch (Throwable t) {

            }


        }

    }
}
