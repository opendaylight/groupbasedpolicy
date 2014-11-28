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

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.groupbasedpolicy.renderer.opflex.MessageUtils.ParsedUri;
import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.ManagedObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.NetworkDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Label.InclusionRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.CapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef.ConnectionTracking;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.ConditionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.Quality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.QualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.Requirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.RequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQuality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQualityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup.IntraGroupPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.TargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MessageUtilsTest {
    protected static final Logger logger = LoggerFactory.getLogger(MessageUtilsTest.class);
    public static final String SEP = "/";
    public static final String TENANT_PREFIX = "/policy/tenants/tenant/";
    public static final String CONTRACT_RN = "contract";
    public static final String EPG_RN = "endpoint-group";
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
    private static final String TEST_URI1 = TENANT_PREFIX + TENANT_UUID + SEP + EPG_RN + SEP + EPG_UUID;
    private static final String TEST_SUBJECT_NAME1 = "subject1";
    private static final String TEST_SUBJECT_NAME2 = "subject2";
    private static final String TEST_CLAUSE_NAME1 = "clause1";
    private static final Integer TEST_SUBJECT_ORDER1 = 1;
    private static final String TEST_SELECTOR_NAME1 = "selector1";
    private static final String TEST_MATCHER_NAME1 = "matcher1";
    private static final String TEST_REQUIREMENT_NAME1 = "requirement1";
    private static final String TEST_REQUIREMENT_MATCHER_NAME1 = "requirement-matcher-name1";
    private static final String TEST_CONDITION_MATCHER_NAME1 = "condition-matcher-name1";
    private static final String TEST_CONDITION_NAME1 = "condition1";
    private static final String TEST_CAPABILITY_MATCHER_NAME1 = "capability-matcher-name1";
    private static final String TEST_CAPABILITY_NAME1 = "capabilit-name1";
    private static final String TEST_NETWORK_DOMAIN_ID = "9AF7B4EF-1C5B-4FA9-A769-F368F781C4E6";
    private static final String TEST_QUALITY_NAME1 = "quality-name1";
    private static final String TEST_QUALITY_MATCHER_NAME1 = "quality-matcher-name1";
    private static final String TEST_INTRAGROUP_POLICY_NAME1 = "intragroup-policy1";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

    }


    @Test
    public void testUri() throws Exception {

        ParsedUri uri = MessageUtils.parseUri(TEST_URI1);
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

    private List<Quality> getTestQualityList() {
        List<Quality> tql = new ArrayList<Quality>();

        QualityBuilder qb = new QualityBuilder();
        qb.setInclusionRule(InclusionRule.Include);
        qb.setName(new QualityName("foo"));
        tql.add(qb.build());

        qb.setInclusionRule(InclusionRule.Exclude);
        qb.setName(new QualityName("boo"));
        tql.add(qb.build());
        return tql;

    }

    @Test
    public void testGetTargetQualities() throws Exception {

        List<ManagedObject> children = new ArrayList<ManagedObject>();
        TargetBuilder tb = new TargetBuilder();
        tb.setName(new TargetName("bar"));
        tb.setQuality(getTestQualityList());
        Target t = tb.build();

        for (Quality q: t.getQuality()) {
            ManagedObject mo = MessageUtils.getQualityMo(q);
            assertTrue(mo != null);
            children.add(mo);
        }
        for (ManagedObject mo: children) {
            System.out.println("MO Name:" + mo.getName());
            for (ManagedObject.Properties p : mo.getProperties()) {
                assertTrue(p != null);
                System.out.println("\t" + p.getName() + ": " + p.getData());
            }
        }
    }

    private List<Target> getTestTargetList() {
        List<Target> tl = new ArrayList<Target>();

        TargetBuilder tb = new TargetBuilder();
        tb.setQuality(getTestQualityList());
        tb.setName(new TargetName(TEST_TARGET_NAME1));
        tl.add(tb.build());
        tb.setName(new TargetName(TEST_TARGET_NAME2));
        tl.add(tb.build());

        return tl;
    }

    private List<ActionRef> getTestActionRefList() {
        List<ActionRef> arl = new ArrayList<ActionRef>();

        ActionRefBuilder arb = new ActionRefBuilder();

        arb.setName(new ActionName(TEST_ACTION_NAME1));
        arb.setOrder(TEST_ACTION_ORDER1);
        arl.add(arb.build());

        arb.setName(new ActionName(TEST_ACTION_NAME2));
        arb.setOrder(TEST_ACTION_ORDER2);
        arl.add(arb.build());

        return arl;
    }

    private List<ClassifierRef> getTestClassifierRefList() {
        List<ClassifierRef> crl = new ArrayList<ClassifierRef>();

        ClassifierRefBuilder crb = new ClassifierRefBuilder();
        crb.setConnectionTracking(ConnectionTracking.Normal);
        crb.setDirection(Direction.Bidirectional);
        crb.setInstanceName(new ClassifierName(TEST_CLASSIFIER_INSTANCE_NAME1));
        crb.setName(new ClassifierName(TEST_CLASSIFIER_NAME1));


        crl.add(crb.build());

        return crl;
    }

    private List<Rule> getTestRuleList() {
        List<Rule> rl = new ArrayList<Rule>();

        RuleBuilder rb = new RuleBuilder();
        rb.setActionRef(getTestActionRefList());
        rb.setClassifierRef(getTestClassifierRefList());
        rb.setOrder(TEST_RULE_ORDER);
        rb.setName(new RuleName(TEST_RULE_NAME1));
        rl.add(rb.build());

        return rl;
    }

    private List<Subject> getTestSubjectList() {
        List<Subject> sl = new ArrayList<Subject>();

        SubjectBuilder sb = new SubjectBuilder();
        sb.setRule(getTestRuleList());
        sb.setName(new SubjectName(TEST_SUBJECT_NAME1));
        sb.setOrder(TEST_SUBJECT_ORDER1);
        sl.add(sb.build());

        return sl;
    }

    private List<SubjectName> getTestSubjectRefs() {
        List<SubjectName> srl = new ArrayList<SubjectName>();

        srl.add(new SubjectName(TEST_SUBJECT_NAME1));
        srl.add(new SubjectName(TEST_SUBJECT_NAME2));

        return srl;
    }

    private List<MatcherRequirement> getTestMatcherRequirement() {
        List<MatcherRequirement> mrl = new ArrayList<MatcherRequirement>();
        MatcherRequirementBuilder mrb = new MatcherRequirementBuilder();

        mrb.setInclusionRule(InclusionRule.Include);
        mrb.setSelectorNamespace(new SelectorName(TEST_SELECTOR_NAME1));
        mrb.setName(new RequirementName(TEST_REQUIREMENT_NAME1));

        mrl.add(mrb.build());

        return mrl;
    }

    private List<RequirementMatcher> getTestRequirementMatcher() {
        List<RequirementMatcher> rml = new ArrayList<RequirementMatcher>();

        RequirementMatcherBuilder rmb = new RequirementMatcherBuilder();
        rmb.setMatchType(MatchType.All);
        rmb.setMatcherRequirement(getTestMatcherRequirement());
        rmb.setName(new RequirementMatcherName(TEST_REQUIREMENT_MATCHER_NAME1));

        rml.add(rmb.build());

        return rml;
    }

    private List<Condition> getTestConditions() {
        List<Condition> cl = new ArrayList<Condition>();

        ConditionBuilder cb = new ConditionBuilder();
        cb.setInclusionRule(InclusionRule.Exclude);
        cb.setName(new ConditionName(TEST_CONDITION_NAME1));
        cl.add(cb.build());

        return cl;
    }

    private List<ConditionMatcher> getTestConditionMatcher() {
        List<ConditionMatcher> cml = new ArrayList<ConditionMatcher>();
        ConditionMatcherBuilder cmb = new ConditionMatcherBuilder();

        cmb.setMatchType(MatchType.None);
        cmb.setCondition(getTestConditions());
        cmb.setName(new ConditionMatcherName(TEST_CONDITION_MATCHER_NAME1));

        cml.add(cmb.build());

        return cml;
    }

    private List<MatcherCapability> getTestMatcherCapability() {
        List<MatcherCapability> mcl = new ArrayList<MatcherCapability>();
        MatcherCapabilityBuilder mcb = new MatcherCapabilityBuilder();

        mcb.setInclusionRule(InclusionRule.Include);
        mcb.setSelectorNamespace(new SelectorName(TEST_SELECTOR_NAME1));
        mcb.setName(new CapabilityName(TEST_CAPABILITY_NAME1));


        mcl.add(mcb.build());

        return mcl;
    }

    private List<CapabilityMatcher> getTestCapabilityMatcher() {
        List<CapabilityMatcher> cml = new ArrayList<CapabilityMatcher>();
        CapabilityMatcherBuilder cmb = new CapabilityMatcherBuilder();

        cmb.setMatcherCapability(getTestMatcherCapability());
        cmb.setMatchType(MatchType.All);
        cmb.setName(new CapabilityMatcherName(TEST_CAPABILITY_MATCHER_NAME1));

        cml.add(cmb.build());

        return cml;
    }

    private List<Capability> getTestCapabilityList() {
        List<Capability> c = new ArrayList<Capability>();

        CapabilityBuilder cb = new CapabilityBuilder();
        cb.setInclusionRule(InclusionRule.Include);
        cb.setName(new CapabilityName(TEST_CAPABILITY_NAME1));
        c.add(cb.build());

        return c;
    }

    private List<Requirement> getTestRequirementList() {
        List<Requirement> r = new ArrayList<Requirement>();

        RequirementBuilder rb = new RequirementBuilder();
        rb.setInclusionRule(InclusionRule.Exclude);
        rb.setName(new RequirementName(TEST_REQUIREMENT_NAME1));
        r.add(rb.build());

        return r;
    }

    private List<ConsumerNamedSelector> getTestConsumerNamedSelectorList() {
        List<ConsumerNamedSelector> cns = new ArrayList<ConsumerNamedSelector>();

        ConsumerNamedSelectorBuilder cnsb = new ConsumerNamedSelectorBuilder();
        cnsb.setContract(getTestContractIdList());
        cnsb.setRequirement(getTestRequirementList());
        cnsb.setName(new SelectorName(TEST_SELECTOR_NAME1));
        cns.add(cnsb.build());

        return cns;
    }

    private List<MatcherQuality> getTestMatcherQualityList() {
        List<MatcherQuality> mq = new ArrayList<MatcherQuality>();

        MatcherQualityBuilder mqb = new MatcherQualityBuilder();
        mqb.setInclusionRule(InclusionRule.Include);
        mqb.setTargetNamespace(new TargetName(TEST_TARGET_NAME1));
        mqb.setName(new QualityName(TEST_QUALITY_NAME1));

        mq.add(mqb.build());

        return mq;
    }

    private List<QualityMatcher> getTestQualityMatcherList() {
        List<QualityMatcher> qm = new ArrayList<QualityMatcher>();

        QualityMatcherBuilder qmb = new QualityMatcherBuilder();
        qmb.setMatcherQuality(getTestMatcherQualityList());
        qmb.setMatchType(MatchType.Any);
        qmb.setName(new QualityMatcherName(TEST_QUALITY_MATCHER_NAME1));

        qm.add(qmb.build());

        return qm;
    }

    private List<ConsumerTargetSelector> getTestConsumerTargetSelectorList() {
        List<ConsumerTargetSelector> cts = new ArrayList<ConsumerTargetSelector>();

        ConsumerTargetSelectorBuilder ctsb = new ConsumerTargetSelectorBuilder();
        ctsb.setName(new SelectorName(TEST_SELECTOR_NAME1));
        ctsb.setQualityMatcher(getTestQualityMatcherList());
        ctsb.setRequirement(getTestRequirementList());
        cts.add(ctsb.build());

        return cts;
    }

    private List<ProviderTargetSelector> getTestProviderTargetSelectorList() {
        List<ProviderTargetSelector> pts = new ArrayList<ProviderTargetSelector>();

        ProviderTargetSelectorBuilder ptsb = new ProviderTargetSelectorBuilder();
        ptsb.setName(new SelectorName(TEST_SELECTOR_NAME1));
        ptsb.setQualityMatcher(getTestQualityMatcherList());
        ptsb.setCapability(getTestCapabilityList());

        pts.add(ptsb.build());

        return pts;
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
        pnsb.setCapability(getTestCapabilityList());
        pnsb.setContract(getTestContractIdList());
        pnsb.setName(new SelectorName(TEST_SELECTOR_NAME1));

        pns.add(pnsb.build());

        return pns;
    }

    private List<Clause> getTestClauseList() {
        List<Clause> cl = new ArrayList<Clause>();

        ConsumerMatchersBuilder cmb = new ConsumerMatchersBuilder();
        cmb.setRequirementMatcher(getTestRequirementMatcher());
        cmb.setConditionMatcher(getTestConditionMatcher());

        ProviderMatchersBuilder pmb = new ProviderMatchersBuilder();
        pmb.setCapabilityMatcher(getTestCapabilityMatcher());
        pmb.setConditionMatcher(getTestConditionMatcher());

        ClauseBuilder cb = new ClauseBuilder();
        cb.setConsumerMatchers(cmb.build());
        cb.setProviderMatchers(pmb.build());
        cb.setSubjectRefs(getTestSubjectRefs());
        cb.setName(new ClauseName(TEST_CLAUSE_NAME1));
        cl.add(cb.build());

        return cl;
    }

    private void printMos(ManagedObject mo) {
        if (mo == null) return;

        System.out.println("MO Name:" + mo.getName());
        if (mo.getProperties() != null) {
            for (ManagedObject.Properties p : mo.getProperties()) {
                assertTrue(p != null);
                System.out.println("\t" + p.getName() + ": " + p.getData());
            }
        }
        if (mo.getChildren() == null) return;

        for (ManagedObject children: mo.getChildren()) {
            printMos(children);
        }
    }

    @Test
    public void testGetContractTargets() throws Exception {
        ContractBuilder cb = new ContractBuilder();
        cb.setClause(getTestClauseList());
        cb.setQuality(getTestQualityList());
        cb.setSubject(getTestSubjectList());
        cb.setTarget(getTestTargetList());
        cb.setId(new ContractId(TEST_CONTRACT_ID1));

        Contract c = cb.build();
        assertTrue(c != null);
        ManagedObject children = MessageUtils.getContractMo(c);
        printMos(children);
    }

    @Test
    public void testGetEndpointGroup() throws Exception {
        EndpointGroupBuilder epgb = new EndpointGroupBuilder();
        epgb.setCapability(getTestCapabilityList());
        epgb.setConsumerNamedSelector(getTestConsumerNamedSelectorList());
        epgb.setConsumerTargetSelector(getTestConsumerTargetSelectorList());
        epgb.setRequirement(getTestRequirementList());
        epgb.setProviderTargetSelector(getTestProviderTargetSelectorList());
        epgb.setProviderNamedSelector(getTestProviderNamedSelectorList());

        epgb.setIntraGroupPolicy(IntraGroupPolicy.Allow);
        epgb.setNetworkDomain(new NetworkDomainId(TEST_NETWORK_DOMAIN_ID));
        epgb.setName(new Name(EPG_NAME));
        epgb.setId(new EndpointGroupId(EPG_UUID));

        EndpointGroup epg = epgb.build();
        assertTrue(epg != null);
        ManagedObject children = MessageUtils.getEndpointGroupMo(epg);
        printMos(children);
    }
}
