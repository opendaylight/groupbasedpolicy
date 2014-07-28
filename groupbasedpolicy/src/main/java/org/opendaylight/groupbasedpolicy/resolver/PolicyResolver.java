/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.ConsumerSelectionRelator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.ProviderSelectionRelator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * The policy resolver is a utility for renderers to help in resolving
 * group-based policy into a form that is easier to apply to the actual network.
 *
 * <p>For any pair of endpoint groups, there is a set of rules that could apply
 * to the endpoints on that group based on the policy configuration.  The exact
 * list of rules that apply to a given pair of endpoints depends on the
 * conditions that are active on the endpoints.
 *
 * <p>We need to be able to query against this policy model, enumerate the
 * relevant classes of traffic and endpoints, and notify renderers when there
 * are changes to policy as it applies to active sets of endpoints and
 * endpoint groups.
 *
 * <p>The policy resolver will maintain the necessary state for all tenants
 * in its control domain, which is the set of tenants for which
 * policy listeners have been registered.
 *
 * @author readams
 */
public class PolicyResolver implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PolicyResolver.class);

    private final DataBroker dataProvider;
    private final ScheduledExecutorService executor;

    /**
     *  Keep track of the current relevant policy scopes.
     */
    protected CopyOnWriteArrayList<PolicyScope> policyListenerScopes;

    protected ConcurrentMap<TenantId, TenantContext> resolvedTenants;

    /**
     * Store a policy object for each endpoint group pair.  The table
     * is stored with the key as (consumer, provider).  Two endpoints could
     * appear in both roles at the same time, in which case both policies would
     * apply.
     */
    AtomicReference<PolicyInfo> policy = new AtomicReference<>();

    public PolicyResolver(DataBroker dataProvider,
                          ScheduledExecutorService executor) {
        super();
        this.dataProvider = dataProvider;
        this.executor = executor;
        policyListenerScopes = new CopyOnWriteArrayList<>();
        resolvedTenants = new ConcurrentHashMap<>();
        LOG.debug("Initialized renderer common policy resolver");
    }

    // *************
    // AutoCloseable
    // *************

    @Override
    public void close() throws Exception {
        for (TenantContext ctx : resolvedTenants.values()) {
            if (ctx.registration != null)
                ctx.registration.close();
        }
    }

    // *************************
    // PolicyResolver public API
    // *************************

    /**
     * Get a snapshot of the current policy
     * @return the {@link PolicyInfo} object representing an immutable
     * snapshot of the policy state
     */
    public PolicyInfo getCurrentPolicy() {
        return policy.get();
    }

    /**
     * Get the normalized tenant for the given ID
     * @param tenant the tenant ID
     * @return the {@link Tenant}
     */
    public IndexedTenant getTenant(TenantId tenant) {
        TenantContext tc = resolvedTenants.get(tenant);
        if (tc == null) return null;
        return tc.tenant.get();
    }

    /**
     * Register a listener to receive update events.
     * @param listener the {@link PolicyListener} object to receive the update
     * events
     */
    public PolicyScope registerListener(PolicyListener listener) {
        PolicyScope ps = new PolicyScope(this, listener);
        policyListenerScopes.add(ps);

        return ps;
    }

    /**
     * Remove the listener registered for the given {@link PolicyScope}.
     * @param scope the scope to remove
     * @see PolicyResolver#registerListener(PolicyListener)
     */
    public void removeListener(PolicyScope scope) {
        policyListenerScopes.remove(scope);
    }

    // **************
    // Implementation
    // **************

    /**
     * Atomically update the active policy and notify policy listeners 
     * of relevant changes
     * @param policyMap the new policy to set
     * @param egConditions the map of endpoint groups to relevant condition sets
     * @return the set of groups with updated policy
     */
    protected Set<EgKey> updatePolicy(Table<EgKey, EgKey, Policy> policyMap,
                                      Map<EgKey, Set<ConditionSet>> egConditions,
                                      List<PolicyScope> policyListenerScopes) {
        PolicyInfo newPolicy = new PolicyInfo(policyMap, egConditions);
        PolicyInfo oldPolicy = policy.getAndSet(newPolicy);
        
        HashSet<EgKey> notifySet = new HashSet<>(); 
        
        for (Cell<EgKey, EgKey, Policy> cell : newPolicy.getPolicyMap().cellSet()) {
            Policy newp = cell.getValue();
            Policy oldp = null;
            if (oldPolicy != null)
                oldp = oldPolicy.getPolicyMap().get(cell.getRowKey(),
                                                    cell.getColumnKey());
            if (oldp == null || !newp.equals(oldp)) {
                notifySet.add(cell.getRowKey());
                notifySet.add(cell.getColumnKey());
            }
        }
        if (oldPolicy != null) {
            for (Cell<EgKey, EgKey, Policy> cell : oldPolicy.getPolicyMap().cellSet()) {
                if (!newPolicy.getPolicyMap().contains(cell.getRowKey(),
                                                       cell.getColumnKey())) {
                    notifySet.add(cell.getRowKey());
                    notifySet.add(cell.getColumnKey());
                }
            }
        }
        return notifySet;
    }
    
    /**
     * Notify the policy listeners about a set of updated groups
     */
    private void notifyListeners(Set<EgKey> updatedGroups) {
        for (final PolicyScope scope : policyListenerScopes) {
            Set<EgKey> filtered =
                    Sets.filter(updatedGroups, new Predicate<EgKey>() {
                        @Override
                        public boolean apply(EgKey input) {
                            return scope.contains(input.getTenantId(),
                                                  input.getEgId());
                        }
                    });
            if (!filtered.isEmpty()) {
                scope.getListener().policyUpdated(filtered);
            }
        }
    }

    /**
     * Subscribe the resolver to updates related to a particular tenant
     * Make sure that this can't be called concurrently with subscribe
     * @param tenantId the tenant ID to subscribe to
     */
    protected void subscribeTenant(TenantId tenantId) {
        if (!resolvedTenants.containsKey(tenantId))
            updateTenant(tenantId);
    }

    /**
     * Unsubscribe the resolver from updates related to a particular tenant
     * Make sure that this can't be called concurrently with subscribe
     * @param tenantId the tenant ID to subscribe to
     */
    protected void unsubscribeTenant(TenantId tenantId) {
        TenantContext context = resolvedTenants.get(tenantId);
        if (context != null) {
            resolvedTenants.remove(tenantId);
            context.registration.close();
        }
    }

    private void updateTenant(final TenantId tenantId) {
        if (dataProvider == null) return;

        TenantContext context = resolvedTenants.get(tenantId);
        if (context == null) {
            ListenerRegistration<DataChangeListener> registration = null;
            registration = dataProvider
                    .registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                TenantUtils.tenantIid(tenantId),
                                                new PolicyChangeListener(tenantId),
                                                DataChangeScope.SUBTREE);

            context = new TenantContext(registration);
            TenantContext oldContext =
                    resolvedTenants.putIfAbsent(tenantId, context);
            if (oldContext != null) {
                // already registered in a different thread; just use the other
                // context
                registration.close();
                context = oldContext;
            } else {
                LOG.info("Added tenant {} to policy scope", tenantId);
            }
        }

        // Resolve the new tenant and update atomically
        final AtomicReference<IndexedTenant> tenantRef = context.tenant;
        final IndexedTenant ot = tenantRef.get();
        ReadOnlyTransaction transaction =
                dataProvider.newReadOnlyTransaction();
        final InstanceIdentifier<Tenant> tiid = TenantUtils.tenantIid(tenantId);
        ListenableFuture<Optional<Tenant>> unresolved;

        unresolved = transaction.read(LogicalDatastoreType.CONFIGURATION, tiid);

        Futures.addCallback(unresolved, new FutureCallback<Optional<Tenant>>() {
            @Override
            public void onSuccess(Optional<Tenant> result) {
                if (!result.isPresent()) {
                    LOG.warn("Tenant {} not found", tenantId);
                }

                Tenant t = InheritanceUtils.resolveTenant((Tenant)result.get());
                IndexedTenant it = new IndexedTenant(t);
                if (!tenantRef.compareAndSet(ot, it)) {
                    // concurrent update of tenant policy.  Retry
                    updateTenant(tenantId);
                } else {
                    // Update the policy cache and notify listeners
                    WriteTransaction wt = dataProvider.newWriteOnlyTransaction();
                    wt.put(LogicalDatastoreType.OPERATIONAL, tiid, t, true);
                    wt.submit();
                    updatePolicy();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Count not get tenant {}", tenantId, t);
            }
        }, executor);
    }

    protected void updatePolicy() {
        try {
            Map<EgKey, Set<ConditionSet>> egConditions = new HashMap<>();
            Table<EgKey, EgKey, Policy> policyMap = 
                    resolvePolicy(resolvedTenants.values(), 
                                  egConditions);
            Set<EgKey> updatedGroups =
                    updatePolicy(policyMap,
                                 egConditions, 
                                 policyListenerScopes);

            notifyListeners(updatedGroups);
        } catch (Exception e) {
            LOG.error("Failed to update policy", e);
        }
    }
    
    /**
     * Resolve the policy in three phases:
     * (1) select contracts that in scope based on contract selectors.
     * (2) select subjects that are in scope for each contract based on
     * matchers in clauses
     * (3) resolve the set of in-scope contracts into a list of subjects that
     * apply for each pair of endpoint groups and the conditions that can
     * apply for for each endpoint in those groups.
     */
    protected Table<EgKey, EgKey, Policy> 
            resolvePolicy(Collection<TenantContext> tenants,
                          Map<EgKey, Set<ConditionSet>> egConditions) {
        // select contracts that apply for the given tenant
        Table<EgKey, EgKey, List<ContractMatch>> contractMatches =
                selectContracts(tenants);

        // select subjects for the matching contracts and resolve the policy
        // for endpoint group pairs.  This does phase (2) and (3) as one step
        return selectSubjects(contractMatches, egConditions);
    }

    /**
     * Choose the contracts that are in scope for each pair of endpoint
     * groups, then perform subject selection for the pair
     */
    protected Table<EgKey, EgKey, List<ContractMatch>>
        selectContracts(Collection<TenantContext> tenants) {
        Table<TenantId, ContractId, 
              List<ConsumerContractMatch>> consumerMatches =
                HashBasedTable.create();
        Table<EgKey, EgKey, List<ContractMatch>> contractMatches =
                HashBasedTable.create();

        for (TenantContext tenant : tenants) {
            IndexedTenant t = tenant.tenant.get();
            if (t == null) continue;
            selectContracts(consumerMatches, 
                            contractMatches, 
                            t.getTenant());
        }
        return contractMatches;
    }
    protected void selectContracts(Table<TenantId, 
                                         ContractId, 
                                         List<ConsumerContractMatch>> consumerMatches,
                                   Table<EgKey, EgKey, 
                                         List<ContractMatch>> contractMatches,
                                   Tenant tenant) {
        // For each endpoint group, match consumer selectors
        // against contracts to get a set of matching consumer selectors
        if (tenant.getEndpointGroup() == null) return;
        for (EndpointGroup group : tenant.getEndpointGroup()) {
            List<ConsumerContractMatch> r =
                    matchConsumerContracts(tenant, group);
            for (ConsumerContractMatch ccm : r) {
                List<ConsumerContractMatch> cms =
                        consumerMatches.get(tenant.getId(),
                                            ccm.contract.getId());
                if (cms == null) {
                    cms = new ArrayList<>();
                    consumerMatches.put(tenant.getId(),
                                        ccm.contract.getId(), cms);
                }
                cms.add(ccm);
            }
        }

        // Match provider selectors, and check each match for a corresponding
        // consumer selector match.
        for (EndpointGroup group : tenant.getEndpointGroup()) {
            List<ContractMatch> matches =
                    matchProviderContracts(tenant, group, consumerMatches);
            for (ContractMatch cm : matches) {
                EgKey consumerKey = new EgKey(cm.consumerTenant.getId(),
                                              cm.consumer.getId());
                EgKey providerKey = new EgKey(cm.providerTenant.getId(),
                                              cm.provider.getId());
                List<ContractMatch> egPairMatches =
                        contractMatches.get(consumerKey, providerKey);
                if (egPairMatches == null) {
                    egPairMatches = new ArrayList<>();
                    contractMatches.put(consumerKey, providerKey,
                                        egPairMatches);
                }

                egPairMatches.add(cm);
            }
        }
    }

    private boolean clauseMatches(Clause clause, ContractMatch match) {
        if (clause.getConsumerMatchers() != null) {
            List<RequirementMatcher> reqMatchers =
                    clause.getConsumerMatchers().getRequirementMatcher();
            if (reqMatchers != null) {
                for (RequirementMatcher reqMatcher : reqMatchers) {
                    if (!MatcherUtils.applyReqMatcher(reqMatcher,
                                                      match.consumerRelator)) {
                        return false;
                    }
                }
            }
        }
        if (clause.getProviderMatchers() != null) {
            List<CapabilityMatcher> capMatchers =
                    clause.getProviderMatchers().getCapabilityMatcher();
            if (capMatchers != null) {
                for (CapabilityMatcher capMatcher : capMatchers) {
                    if (!MatcherUtils.applyCapMatcher(capMatcher,
                                                      match.providerRelator)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private ConditionSet buildConditionSet(List<ConditionMatcher> condMatchers) {
        if (condMatchers == null) return ConditionSet.EMPTY;

        ImmutableSet.Builder<ConditionName> allb = ImmutableSet.builder();
        ImmutableSet.Builder<ConditionName> noneb = ImmutableSet.builder();
        ImmutableSet.Builder<Set<ConditionName>> anyb =
                ImmutableSet.builder();
        for (ConditionMatcher condMatcher : condMatchers) {
            if (condMatcher.getCondition() == null)
                continue;
            MatchType type = condMatcher.getMatchType();
            if (type == null) type = MatchType.All;
            if (type.equals(MatchType.Any)) {
                ImmutableSet.Builder<ConditionName> a =
                        ImmutableSet.builder();
                for (Condition c : condMatcher.getCondition()) {
                    a.add(c.getName());
                }
                anyb.add(a.build());
            } else {
                for (Condition c : condMatcher.getCondition()) {
                    switch (type) {
                    case Any:
                        break;
                    case None:
                        noneb.add(c.getName());
                        break;
                    case All:
                    default:
                        allb.add(c.getName());
                        break;
                    }
                }
            }
        }
        return new ConditionSet(allb.build(), noneb.build(), anyb.build());
    }

    private ConditionSet buildConsConditionSet(Clause clause) {
        if (clause.getConsumerMatchers() != null) {
            List<ConditionMatcher> condMatchers =
                    clause.getConsumerMatchers().getConditionMatcher();
            return buildConditionSet(condMatchers);
        }
        return ConditionSet.EMPTY;
    }

    private ConditionSet buildProvConditionSet(Clause clause) {
        if (clause.getProviderMatchers() != null) {
            List<ConditionMatcher> condMatchers =
                    clause.getProviderMatchers().getConditionMatcher();
            return buildConditionSet(condMatchers);
        }
        return ConditionSet.EMPTY;
    }

    private Policy resolvePolicy(Tenant contractTenant,
                                 Contract contract,
                                 boolean reverse,
                                 Policy merge,
                                 Table<ConditionSet, ConditionSet,
                                       List<Subject>> subjectMap) {
        Table<ConditionSet, ConditionSet, List<RuleGroup>> ruleMap =
                HashBasedTable.create();
        if (merge != null) {
            ruleMap.putAll(merge.ruleMap);
        }
        for (Cell<ConditionSet, ConditionSet, List<Subject>> entry :
                subjectMap.cellSet()) {
            List<RuleGroup> rules = new ArrayList<>();
            ConditionSet rowKey = entry.getRowKey();
            ConditionSet columnKey = entry.getColumnKey();
            if (reverse) {
                rowKey = columnKey;
                columnKey = entry.getRowKey();
            }
            List<RuleGroup> oldrules =
                    ruleMap.get(rowKey, columnKey);
            if (oldrules != null) {
                rules.addAll(oldrules);
            }
            for (Subject s : entry.getValue()) {
                if (s.getRule() == null) continue;
                List<Rule> srules;
                if (reverse)
                    srules = reverseRules(s.getRule());
                else
                    srules = Ordering
                            .from(TenantUtils.RULE_COMPARATOR)
                            .immutableSortedCopy(s.getRule());

                RuleGroup rg = new RuleGroup(srules, s.getOrder(),
                                             contractTenant, contract,
                                             s.getName());
                rules.add(rg);
            }
            Collections.sort(rules);
            ruleMap.put(rowKey, columnKey,
                        Collections.unmodifiableList(rules));
        }
        return new Policy(ruleMap);
    }
    
    private List<Rule> reverseRules(List<Rule> rules) {
        ArrayList<Rule> nrules = new ArrayList<>();
        for (Rule input : rules) {
            if (input.getClassifierRef() == null ||
                input.getClassifierRef().size() == 0) {
                nrules.add(input);
                continue;
            }

            List<ClassifierRef> classifiers = new ArrayList<>();
            for (ClassifierRef clr : input.getClassifierRef()) {
                Direction nd = Direction.Bidirectional;
                if (clr.getDirection() != null) {
                    switch (clr.getDirection()) {
                    case In:
                        nd = Direction.Out;
                        break;
                    case Out:
                        nd = Direction.In;
                        break;
                    case Bidirectional:
                    default:
                        nd = Direction.Bidirectional;
                    }
                }
                classifiers.add(new ClassifierRefBuilder(clr)
                    .setDirection(nd).build());
            }
            nrules.add(new RuleBuilder(input)
                .setClassifierRef(Collections.unmodifiableList(classifiers))
                .build());
        }
        Collections.sort(nrules, TenantUtils.RULE_COMPARATOR);
        return Collections.unmodifiableList(nrules);
    }

    /**
     * Get the "natural" direction for the policy for the given pair of 
     * endpoint groups.
     * @param one The first endpoint group
     * @param two The second endpoint group
     * @return true if the order should be reversed in the index
     */
    protected static boolean shouldReverse(EgKey one, EgKey two) {
        if (one.compareTo(two) < 0) {
            return true;
        }
        return false;
    }
    
    private void addConditionSet(EgKey eg, ConditionSet cs, 
                                 Map<EgKey, Set<ConditionSet>> egConditions) {
        if (egConditions == null) return;
        Set<ConditionSet> cset = egConditions.get(eg);
        if (cset == null) {
            egConditions.put(eg, cset = new HashSet<>());
        }
        cset.add(cs);
    }
    
    /**
     * Choose the set of subjects that in scope for each possible set of
     * endpoint conditions
     */
    protected Table<EgKey, EgKey, Policy>
            selectSubjects(Table<EgKey, EgKey,
                                 List<ContractMatch>> contractMatches,
                           Map<EgKey, Set<ConditionSet>> egConditions) {
        // Note that it's possible to further simplify the resulting policy
        // in the case of things like repeated rules, condition sets that
        // cover other condition sets, etc.  This would be a good thing to do
        // at some point
        Table<EgKey, EgKey, Policy> policy = HashBasedTable.create();

        for (List<ContractMatch> matches : contractMatches.values()) {
            for (ContractMatch match : matches) {
                List<Clause> clauses = match.contract.getClause();
                if (clauses == null) continue;

                List<Subject> subjectList = match.contract.getSubject();
                if (subjectList == null) continue;

                EgKey ckey = new EgKey(match.consumerTenant.getId(),
                                       match.consumer.getId());
                EgKey pkey = new EgKey(match.providerTenant.getId(),
                                       match.provider.getId());
                EgKey one = ckey;
                EgKey two = pkey;
                boolean reverse = shouldReverse(ckey, pkey);
                if (reverse) {
                    one = pkey;
                    two = ckey;
                }
                Policy existing = policy.get(one, two);

                HashMap<SubjectName, Subject> subjects = new HashMap<>();
                for (Subject s : subjectList) {
                    subjects.put(s.getName(), s);
                }

                Table<ConditionSet, ConditionSet, List<Subject>> subjectMap =
                        HashBasedTable.create();

                for (Clause clause : clauses) {
                    if (clause.getSubjectRefs() != null &&
                        clauseMatches(clause, match)) {
                        ConditionSet consCSet = buildConsConditionSet(clause);
                        addConditionSet(ckey, consCSet, egConditions);
                        ConditionSet provCSet = buildProvConditionSet(clause);
                        addConditionSet(pkey, provCSet, egConditions);
                        List<Subject> clauseSubjects =
                                subjectMap.get(consCSet, provCSet);
                        if (clauseSubjects == null) {
                            clauseSubjects = new ArrayList<>();
                            subjectMap.put(consCSet, provCSet, clauseSubjects);
                        }
                        for (SubjectName sn : clause.getSubjectRefs()) {
                            Subject s = subjects.get(sn);
                            if (s != null) clauseSubjects.add(s);
                        }
                    }
                }

                policy.put(one, two,
                           resolvePolicy(match.contractTenant,
                                         match.contract,
                                         reverse,
                                         existing,
                                         subjectMap));
            }
        }

        return policy;
    }

    private List<ConsumerContractMatch> matchConsumerContracts(Tenant tenant,
                                                               EndpointGroup consumer) {
        List<ConsumerContractMatch> matches = new ArrayList<>();
        if (consumer.getConsumerNamedSelector() != null) {
            for (ConsumerNamedSelector cns : consumer.getConsumerNamedSelector()) {
                if (cns.getContract() == null) continue;
                for (ContractId contractId : cns.getContract()) {
                    Contract contract =
                            TenantUtils.findContract(tenant, contractId);
                    if (contract == null) continue;
                    matches.add(new ConsumerContractMatch(tenant, contract,
                                                          tenant, consumer,
                                                          cns));
                }
            }
        }
        if (consumer.getConsumerTargetSelector() != null) {
            for (ConsumerTargetSelector cts : consumer.getConsumerTargetSelector()) {
                if (tenant.getContract() == null) continue;
                for (Contract contract : tenant.getContract()) {
                    if (contract.getTarget() == null) continue;
                    for (Target t : contract.getTarget()) {
                        boolean match = true;
                        if (cts.getQualityMatcher() != null) {
                            for (QualityMatcher m : cts.getQualityMatcher()) {
                                if (!MatcherUtils.applyQualityMatcher(m, t)) {
                                    match = false;
                                    break;
                                }
                            }
                        }
                        if (match) {
                            matches.add(new ConsumerContractMatch(tenant,
                                                                  contract,
                                                                  tenant,
                                                                  consumer,
                                                                  cts));
                        }
                    }
                }
            }
        }
        // TODO match selectors also against contract references
//        for (ConsumerTargetSelector cts : consumer.getConsumerTargetSelector()) {
//            if (tenant.getContractRef() == null) continue;
//            for (ContractRef c : tenant.getContractRef()) {
//
//            }
//        }
        return matches;
    }

    private void amendContractMatches(List<ContractMatch> matches,
                                      List<ConsumerContractMatch> cMatches,
                                      Tenant tenant, EndpointGroup provider,
                                      ProviderSelectionRelator relator) {
        if (cMatches == null) return;
        for (ConsumerContractMatch cMatch : cMatches) {
            matches.add(new ContractMatch(cMatch, tenant, provider, relator));
        }
    }

    private List<ContractMatch>
        matchProviderContracts(Tenant tenant, EndpointGroup provider,
                               Table<TenantId,
                                     ContractId,
                                     List<ConsumerContractMatch>> consumerMatches) {
        List<ContractMatch> matches = new ArrayList<>();
        if (provider.getProviderNamedSelector() != null) {
            for (ProviderNamedSelector pns : provider.getProviderNamedSelector()) {
                if (pns.getContract() == null) continue;
                for (ContractId contractId : pns.getContract()) {
                    Contract c = TenantUtils.findContract(tenant, contractId);
                    if (c == null) continue;
                    List<ConsumerContractMatch> cMatches =
                            consumerMatches.get(tenant.getId(), c.getId());
                    amendContractMatches(matches, cMatches, tenant, provider, pns);
                }
            }
        }
        if (provider.getProviderTargetSelector() != null) {
            for (ProviderTargetSelector pts : provider.getProviderTargetSelector()) {
                if (tenant.getContract() == null) continue;
                for (Contract c : tenant.getContract()) {
                    if (c.getTarget() == null) continue;
                    for (Target t : c.getTarget()) {
                        boolean match = true;
                        if (pts.getQualityMatcher() != null) {
                            for (QualityMatcher m : pts.getQualityMatcher()) {
                                if (!MatcherUtils.applyQualityMatcher(m, t)) {
                                    match = false;
                                    break;
                                }
                            }
                        }
                        if (match) {
                            List<ConsumerContractMatch> cMatches =
                                    consumerMatches.get(tenant.getId(),
                                                        c.getId());
                            amendContractMatches(matches, cMatches, tenant,
                                                 provider, pts);

                        }
                    }
                }
            }
        }
        return matches;
    }

    protected static class TenantContext {
        ListenerRegistration<DataChangeListener> registration;

        AtomicReference<IndexedTenant> tenant = new AtomicReference<>();

        public TenantContext(ListenerRegistration<DataChangeListener> registration) {
            super();
            this.registration = registration;
        }
    }

    /**
     * Represents a selected contract made by endpoint groups matching it
     * using selection relators.  This is the result of the contract selection
     * phase.
     * @author readams
     *
     */
    @Immutable
    protected static class ContractMatch extends ConsumerContractMatch {
        /**
         * The tenant ID of the provider endpoint group
         */
        final Tenant providerTenant;

        /**
         * The provider endpoint group
         */
        final EndpointGroup provider;

        /**
         * The provider selection relator that was used to match the contract
         */
        final ProviderSelectionRelator providerRelator;

        public ContractMatch(ConsumerContractMatch consumerMatch,
                             Tenant providerTenant, EndpointGroup provider,
                             ProviderSelectionRelator providerRelator) {
            super(consumerMatch.contractTenant,
                  consumerMatch.contract,
                  consumerMatch.consumerTenant,
                  consumerMatch.consumer,
                  consumerMatch.consumerRelator);
            this.providerTenant = providerTenant;
            this.provider = provider;
            this.providerRelator = providerRelator;
        }
    }

    @Immutable
    private static class ConsumerContractMatch {
        /**
         * The tenant of the matching contract
         */
        final Tenant contractTenant;

        /**
         * The matching contract
         */
        final Contract contract;

        /**
         * The tenant for the endpoint group
         */
        final Tenant consumerTenant;

        /**
         * The consumer endpoint group
         */
        final EndpointGroup consumer;

        /**
         * The consumer selection relator that was used to match the contract
         */
        final ConsumerSelectionRelator consumerRelator;


        public ConsumerContractMatch(Tenant contractTenant,
                                     Contract contract,
                                     Tenant consumerTenant,
                                     EndpointGroup consumer,
                                     ConsumerSelectionRelator consumerRelator) {
            super();
            this.contractTenant = contractTenant;
            this.contract = contract;
            this.consumerTenant = consumerTenant;
            this.consumer = consumer;
            this.consumerRelator = consumerRelator;
        }
    }

    @Immutable
    private class PolicyChangeListener implements DataChangeListener {
        final TenantId tenantId;

        public PolicyChangeListener(TenantId tenantId) {
            super();
            this.tenantId = tenantId;
        }

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
            updateTenant(tenantId);
        }

    }
}
