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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.groupbasedpolicy.renderer.opflex.messages.ManagedObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.Quality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.Requirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQuality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstance;



public class MessageUtils {
    /*
     * Endpoint Groups in ODL's Group Based Policy are
     * specified in the following format:
     *
     *    /policy/tenants/tenant/<tenant UUID>/endpoint-group/<endpoint-group UUID>
     */
    public static final String URI_SEP = "/";
    public static final String POLICY_ROOT = URI_SEP + "policy";
    public static final String TENANTS_RN = "tenants";
    public static final String TENANT_RN = "tenant";
    public static final String CONTRACT_RN = "contract";
    public static final String SUBJECT_RN = "subject";
    public static final String CLAUSE_RN = "clause";
    public static final String EPG_RN = "endpoint-group";

    public static final String TENANT_PREFIX = POLICY_ROOT +
            URI_SEP + TENANTS_RN + URI_SEP + TENANT_RN + URI_SEP;

    /*
     * MO Names
     */
    private static final String QUALITY_MO = "QualityMo";
    private static final String CONTRACT_MO = "ContractMo";
    private static final String CLAUSE_MO = "ClauseMo";
    private static final String TARGET_MO = "TargetMo";
    private static final String SUBJECT_MO = "SubjectMo";
    private static final String RULE_MO = "RuleMo";
    private static final String CLASSIFIER_REF_MO = "ClassifierRefMo";
    private static final String ACTION_REF_MO = "ActionRefMo";
    private static final String CONSUMER_MATCHERS_MO = "ConsumerMatchersMo";
    private static final String PROVIDER_MATCHERS_MO = "ProviderMatchersMo";
    private static final String REQUIREMENT_MATCHER_MO = "RequirementMatcherMo";
    private static final String MATCHER_CAPABILITY_MO = "MacherCapabilityMo";
    private static final String QUALITY_MATCHER_MO = "QualityMatcherMo";
    private static final String CAPABILITY_MATCHER_MO = "CapabilityMatcherMo";
    private static final String MATCHER_REQUIREMENT_MO = "MatcherRequirementMo";
    private static final String CONDITION_MO = "ConditionMo";
    private static final String CONDITION_MATCHER_MO = "ConditionMatcherMo";
    private static final String REQUIREMENT_MO = "RequirementMo";
    private static final String CAPABILITY_MO = "CapabilityMo";
    private static final String CONSUMER_NAMED_SELCTOR_MO = "ConsumerNamedSelectorMo";
    private static final String CONSUMER_TARGET_SELCTOR_MO = "ConsumerTargetSelectorMo";
    private static final String MATCHER_QUALITY_MO = "MatcherQualityMo";
    private static final String PROVIDER_NAMED_SELCTOR_MO = "ProviderNamedSelectorMo";
    private static final String PROVIDER_TARGET_SELCTOR_MO = "ProviderTargetSelectorMo";
    private static final String ENDPOINT_GROUP_MO = "EndpointGroupMo";
    private static final String PARAMETER_VALUE_MO = "ParameterValueMo";
    private static final String ACTION_INSTANCE_MO = "ActionInstanceMo";
    private static final String CLASSIFIER_INSTANCE_MO = "ClassifierInstanceMo";


    /*
     * MO Property Names
     */
    private static final String NAME = "Name";
    private static final String INT_VALUE_NAME = "IntValue";
    private static final String STRING_VALUE_NAME = "StringValue";
    private static final String ID_NAME = "Id";
    private static final String TARGET_NAME = "Target";
    private static final String CONTRACT_NAME = "Contract";
    private static final String SUBJECT_NAME = "Subject";
    private static final String ORDER_NAME = "Order";
    private static final String CONNECTION_TRACKING_NAME = "ConnectionTracking";
    private static final String DIRECTION_NAME = "Direction";
    private static final String INSTANCE_NAME = "Instance";
    private static final String QUALITY_NAME = "Quality";
    private static final String INCLUSION_RULE_NAME = "InclusionRule";
    private static final String MATCH_TYPE_NAME = "MatchType";
    private static final String SELECTOR_NAMESPACE_NAME = "SelectorNamespace";
    private static final String TARGET_NAMESPACE_NAME = "TargetNamespace";
    private static final String INTRAGROUP_POLICY_NAME = "IntragroupPolicy";
    private static final String NETWORK_DOMAIN_NAME = "NetworkDomain";
    private static final String ACTION_DEFINITION_ID_NAME = "ActionDefinitionId";
    private static final String CLASSIFIER_DEFINITION_ID_NAME = "ClassifierDefinitionId";

    /**
     * A Parsed URI
     *
     * NB: This could be replaced with either a Path or
     * a URI object if we want something more general.
     *
     * @author tbachman
     *
     */
    public static class ParsedUri {

        private final String uri;
        private final Set<String> uriSet;
        private final String [] splitUri;


        ParsedUri ( String uri ) {
            this.uri = uri;
            String [] tmpUri = uri.split("/");
            this.uriSet = new HashSet<String>();
            if (tmpUri.length > 0) {
                // gets rid of leading empty element
                this.splitUri = Arrays.copyOfRange(tmpUri, 1, tmpUri.length);
                this.uriSet.addAll(Arrays.asList(this.splitUri));
            }
            else {
                this.splitUri = null;
            }
        }

        public boolean valid() {
            if (splitUri.length > 0) {
                return true;
            }
            return false;
        }

        public int getHierarchy() {
            return splitUri.length;
        }

        public String getElement(int level) {
            if (level <= (splitUri.length -1)) {
                return splitUri[level];
            }
            return null;
        }

        public int totalElements() {
            return splitUri.length;
        }

        public boolean contains(String needle) {
            return uriSet.contains(needle);
        }

        public int whichElement(String needle) {
            int index = 0;
            for (String rn: splitUri) {
                if (rn.equals(needle)){
                    return index;
                }
                index++;
            }
            return -1;
        }

        public String originalPath() {
            return uri;
        }
    }

    public static ParsedUri parseUri ( String uri ) {
        ParsedUri u = new ParsedUri(uri);
        if (u.valid()) return u;
        return null;
    }

    public static String getTenantFromUri(String uri) {
        // TODO: Sanity checks (use real URI?)
        String [] elements = uri.split("/");
        if (elements.length >= 5)
            return elements[4];
        return null;
    }

    public static String getEndpointGroupFromUri(String uri) {
        ParsedUri pu = new ParsedUri(uri);
        if (!pu.contains(EPG_RN)) {
            return null;
        }
        int epgIdx = pu.whichElement(EPG_RN);
        /*
         * subtract 1 to compare between total elements
         * and an array index; it's an EPG URI if it's the
         * second to the last element
         */
        if (epgIdx == pu.totalElements()-1-1) {
            return pu.getElement(epgIdx + 1);
        }
        return null;
    }

    public static String createEpgUri(String tenantId, String epgId) {
        return TENANT_PREFIX + tenantId + URI_SEP + EPG_RN + URI_SEP + epgId;
    }

    public static boolean hasEpg(String uri) {
        return new ParsedUri(uri).contains(EPG_RN);
    }

    public static boolean isEpgUri(String uri) {
        ParsedUri pu = new ParsedUri(uri);
        if (!pu.contains(EPG_RN)) {
            return false;
        }
        int epgIdx = pu.whichElement(EPG_RN);
        /*
         * subtract 1 to compare between total elements
         * and an array index; it's an EPG URI if it's the
         * second to the last element
         */
        return (epgIdx == pu.totalElements()-1-1);
    }

    public static ManagedObject getQualityMo(Quality q) {
        if (q == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();

        if(q.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(q.getName().getValue());
            propList.add(moProp);
        }

        if (q.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(q.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(QUALITY_MO);

        return mo;
    }

    public static ManagedObject getTargetMo(Target t) {
        if (t == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (t.getName() != null) {
            moProp= new ManagedObject.Properties();
            moProp.setName(TARGET_NAME);
            moProp.setData(t.getName().getValue());
            propList.add(moProp);
        }

        if (t.getQuality() != null) {
            for (Quality q: t.getQuality()) {
                children.add(getQualityMo(q));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(TARGET_MO);
        if (children.size() > 0)
            mo.setChildren(children);

        return mo;
    }

    public static ManagedObject getActionRefMo(ActionRef ar) {
        if (ar == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (ar.getOrder() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(ORDER_NAME);
            moProp.setData(ar.getOrder().toString());
            propList.add(moProp);
        }

        // TODO: The Action ref's name should be followed to
        //       the actual instance and used in the
        //       "to_relations"
        //
        if (ar.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(ar.getName().getValue());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(ACTION_REF_MO);
        if (children.size() > 0)
            mo.setChildren(children);

        return mo;
    }

    public static ManagedObject getClassifierRefMos(ClassifierRef cr) {
        if (cr == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (cr.getConnectionTracking() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(CONNECTION_TRACKING_NAME);
            moProp.setData(Integer.
                    toString(cr.getConnectionTracking().getIntValue()));
            propList.add(moProp);
        }

        if (cr.getDirection() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(DIRECTION_NAME);
            moProp.setData(cr.getDirection().toString());
            propList.add(moProp);
        }

        // TODO: instance name should be followed
        //       to get the classifier instance and
        //       used in the "to_relations"
        //
        if (cr.getInstanceName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INSTANCE_NAME);
            moProp.setData(cr.getInstanceName().getValue());
            propList.add(moProp);
        }

        if (cr.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(cr.getName().getValue());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CLASSIFIER_REF_MO);

        return mo;
    }


    public static ManagedObject getConditionMo(Condition c) {
        if (c == null) return null;
        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();

        if (c.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(c.getName().getValue());
            propList.add(moProp);
        }

        if (c.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(c.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(CONDITION_MO);

        return mo;
    }

    public static ManagedObject getConditionMatcherMo(ConditionMatcher cm) {
        if (cm == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (cm.getCondition() != null) {
            for (Condition c: cm.getCondition()) {
                children.add(getConditionMo(c));
            }
        }

        if (cm.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(cm.getName().getValue());
            propList.add(moProp);
        }

        if (cm.getMatchType() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(MATCH_TYPE_NAME);
            moProp.setData(cm.getMatchType().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CONDITION_MATCHER_MO);

        return mo;

    }

    public static ManagedObject getMatcherRequirementMo(MatcherRequirement mr) {
        if (mr == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();

        if (mr.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(mr.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (mr.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(mr.getName().getValue());
            propList.add(moProp);
        }

        if (mr.getSelectorNamespace() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(SELECTOR_NAMESPACE_NAME);
            moProp.setData(mr.getSelectorNamespace().getValue());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(MATCHER_REQUIREMENT_MO);

        return mo;
    }

    public static ManagedObject getRequirementMatcherMo(RequirementMatcher rm) {
        if (rm == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (rm.getMatcherRequirement() != null) {
            for (MatcherRequirement mr: rm.getMatcherRequirement()) {
                children.add(getMatcherRequirementMo(mr));
            }
        }

        if (rm.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(rm.getName().getValue());
            propList.add(moProp);
        }

        if (rm.getMatchType() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(MATCH_TYPE_NAME);
            moProp.setData(rm.getMatchType().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(REQUIREMENT_MATCHER_MO);

        return mo;
    }

    public static ManagedObject getMatcherCapabilityMo(MatcherCapability mc) {
        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();

        if (mc.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(mc.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (mc.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(mc.getName().getValue());
            propList.add(moProp);
        }

        if (mc.getSelectorNamespace() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(SELECTOR_NAMESPACE_NAME);
            moProp.setData(mc.getSelectorNamespace().getValue());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(MATCHER_CAPABILITY_MO);

        return mo;
    }

    public static ManagedObject getCapabilityMatcherMo(CapabilityMatcher cm) {
        if (cm == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (cm.getMatcherCapability() != null) {
            for (MatcherCapability mc: cm.getMatcherCapability()) {
                children.add(getMatcherCapabilityMo(mc));
            }
        }

        if (cm.getMatchType() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(MATCH_TYPE_NAME);
            moProp.setData(cm.getMatchType().toString());
            propList.add(moProp);
        }

        if (cm.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(cm.getName().getValue());
            propList.add(moProp);
        }


        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CAPABILITY_MATCHER_MO);

        return mo;
    }

    public static ManagedObject getProviderMatchersMo(ProviderMatchers p) {
        if (p == null) return null;
        ManagedObject mo = new ManagedObject();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (p.getCapabilityMatcher() != null) {
            for (CapabilityMatcher cm : p.getCapabilityMatcher()) {
                children.add(getCapabilityMatcherMo(cm));
            }
        }
        if (p.getConditionMatcher() != null) {
            for (ConditionMatcher cm: p.getConditionMatcher()) {
                children.add(getConditionMatcherMo(cm));
            }
        }

        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(PROVIDER_MATCHERS_MO);

        return mo;

    }

    public static ManagedObject getConsumerMatchersMo(ConsumerMatchers c) {
        if (c == null) return null;

        ManagedObject mo = new ManagedObject();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (c.getRequirementMatcher() != null) {
            for (RequirementMatcher rm: c.getRequirementMatcher()) {
                children.add(getRequirementMatcherMo(rm));
            }
        }
        if (c.getConditionMatcher() != null) {
            for (ConditionMatcher cm : c.getConditionMatcher()) {
                children.add(getConditionMatcherMo(cm));
            }
        }

        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CONSUMER_MATCHERS_MO);

        return mo;
    }

    public static ManagedObject getSubjectMo(Subject s) {
        if (s == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (s.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(SUBJECT_NAME);
            moProp.setData(s.getName().getValue());
            propList.add(moProp);
        }

        if (s.getOrder() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(ORDER_NAME);
            moProp.setData(s.getOrder().toString());
            propList.add(moProp);
        }

        if (s.getRule() != null) {
            for (Rule r: s.getRule()) {
                children.add(getRuleMo(r));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(SUBJECT_MO);

        return mo;
    }

    public static ManagedObject getRuleMo(Rule r) {
        if (r == null) return null;

        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();

        if (r.getActionRef() != null) {
            for (ActionRef ar: r.getActionRef()) {
                children.add(getActionRefMo(ar));
            }
        }
        if (r.getClassifierRef() != null) {
            for (ClassifierRef cr: r.getClassifierRef()) {
                children.add(getClassifierRefMos(cr));
            }
        }

        if (r.getOrder() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(ORDER_NAME);
            moProp.setData(r.getOrder().toString());
            propList.add(moProp);
        }

        if (r.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(r.getName().getValue());
            propList.add(moProp);
        }

        if (propList.size() >0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(RULE_MO);

        return mo;
    }

    public static ManagedObject getClauseMo(Clause c) {
        if (c == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList =
                new ArrayList<ManagedObject.Properties>();
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        if (c.getSubjectRefs() != null) {
            for (SubjectName sn: c.getSubjectRefs()) {
                // TODO: follow subject refs to get subjects
                sn.getValue();
            }
        }

        children.add(getConsumerMatchersMo(c.getConsumerMatchers()));
        children.add(getProviderMatchersMo(c.getProviderMatchers()));

        if (c.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(c.getName().getValue());
            propList.add(moProp);
        }

        if (propList.size() >0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CLAUSE_MO);

        return mo;

    }

    public static ManagedObject getContractMo(Contract c) {
        if (c == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (c.getId() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(ID_NAME);
            moProp.setData(c.getId().getValue());
            propList.add(moProp);
        }

        if (c.getSubject() != null) {
            for (Subject s: c.getSubject()) {
                children.add(getSubjectMo(s));
            }
        }

        if (c.getClause() != null) {
            for (Clause cl: c.getClause()) {
                children.add(getClauseMo(cl));
            }
        }

        if (c.getQuality() != null) {
            for (Quality q: c.getQuality()) {
                children.add(getQualityMo(q));
            }
        }

        if (c.getTarget() != null) {
            for (Target t: c.getTarget()) {
                children.add(getTargetMo(t));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(CONTRACT_MO);

        if (children.size() > 0)
            mo.setChildren(children);

        return mo;
    }

    public static ManagedObject getRequirementMo(Requirement r) {
        if (r == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (r.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(r.getName().getValue());
            propList.add(moProp);
        }

        if (r.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(r.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(REQUIREMENT_MO);

        return mo;
    }

    public static ManagedObject getCapabilityMo(Capability c) {
        if (c == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (c.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(c.getName().getValue());
            propList.add(moProp);
        }

        if (c.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(c.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(CAPABILITY_MO);

        return mo;
    }

    public static ManagedObject getConsumerNamedSelectorMo(ConsumerNamedSelector cns) {
        if (cns == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (cns.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(cns.getName().getValue());
            propList.add(moProp);
        }

        if (cns.getContract() != null) {
            for (ContractId c: cns.getContract()){
                if (c.getValue() != null) {
                    moProp = new ManagedObject.Properties();
                    moProp.setName(CONTRACT_NAME);
                    moProp.setData(c.getValue());
                    propList.add(moProp);
                }
                // TODO: these are references -- follow to get contracts?
                // Also note that these are all properties with the same name
            }
        }

        if (cns.getRequirement() != null) {
            for (Requirement r: cns.getRequirement()) {
                children.add(getRequirementMo(r));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CONSUMER_NAMED_SELCTOR_MO);

        return mo;
    }

    public static ManagedObject getMatcherQualityMo(MatcherQuality mq) {
        if (mq == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();


        if (mq.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(mq.getName().getValue());
            propList.add(moProp);
        }

        if (mq.getInclusionRule() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INCLUSION_RULE_NAME);
            moProp.setData(mq.getInclusionRule().toString());
            propList.add(moProp);
        }

        if (mq.getTargetNamespace() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(TARGET_NAMESPACE_NAME);
            moProp.setData(mq.getTargetNamespace().getValue());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(MATCHER_QUALITY_MO);

        return mo;
    }

    public static ManagedObject getQualityMatcherMo(QualityMatcher qm) {
        if (qm == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (qm.getMatcherQuality() != null) {
            for (MatcherQuality mq: qm.getMatcherQuality()) {
                children.add(getMatcherQualityMo(mq));
            }
        }

        if (qm.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(qm.getName().getValue());
            propList.add(moProp);
        }

        if (qm.getMatchType() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(MATCH_TYPE_NAME);
            moProp.setData(qm.getMatchType().toString());
            propList.add(moProp);
        }

        if (propList.size() >0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(QUALITY_MATCHER_MO);

        return mo;
    }

    public static ManagedObject getConsumerTargetSelectorMo(ConsumerTargetSelector cts) {
        if (cts == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (cts.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(cts.getName().getValue());
            propList.add(moProp);
        }

        if (cts.getQualityMatcher() != null) {
            for (QualityMatcher qm: cts.getQualityMatcher()) {
                children.add(getQualityMatcherMo(qm));
            }
        }

        if (cts.getRequirement() != null) {
            for (Requirement r: cts.getRequirement()) {
                children.add(getRequirementMo(r));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CONSUMER_TARGET_SELCTOR_MO);

        return mo;
    }

    public static ManagedObject getProviderNamedSelectorMo(ProviderNamedSelector pns) {
        if (pns == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (pns.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(pns.getName().getValue());
            propList.add(moProp);
        }

        if (pns.getContract() != null) {
            for (ContractId c: pns.getContract()){
                if (c.getValue() != null) {
                    moProp = new ManagedObject.Properties();
                    moProp.setName(CONTRACT_NAME);
                    moProp.setData(c.getValue());
                    propList.add(moProp);
                    // TODO: these are references -- follow to get contracts?
                    // Also note that these are all properties with the same name
                }
            }
        }

        if (pns.getCapability() != null) {
            for (Capability c: pns.getCapability()) {
                children.add(getCapabilityMo(c));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(PROVIDER_NAMED_SELCTOR_MO);

        return mo;
    }

    public static ManagedObject getProviderTargetSelectorMo(ProviderTargetSelector pts) {
        if (pts == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (pts.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(pts.getName().getValue());
            propList.add(moProp);
        }

        if (pts.getQualityMatcher() != null) {
            for (QualityMatcher qm: pts.getQualityMatcher()) {
                children.add(getQualityMatcherMo(qm));
            }
        }

        if (pts.getCapability() != null) {
            for (Capability c: pts.getCapability()) {
                children.add(getCapabilityMo(c));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(PROVIDER_TARGET_SELCTOR_MO);

        return mo;
    }

    public static ManagedObject getClassifierInstanceMo(ClassifierInstance ci) {
        if (ci == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (ci.getClassifierDefinitionId() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(CLASSIFIER_DEFINITION_ID_NAME);
            moProp.setData(ci.getClassifierDefinitionId().getValue());
            propList.add(moProp);
        }

        if (ci.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(ci.getName().getValue());
            propList.add(moProp);
        }

        if (ci.getParameterValue() != null) {
            for (ParameterValue pv: ci.getParameterValue()) {
                children.add(getParameterMo(pv));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(CLASSIFIER_INSTANCE_MO);

        return mo;
    }

    public static ManagedObject getParameterMo(ParameterValue pv) {
        if (pv == null) return null;

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (pv.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(pv.getName().getValue());
            propList.add(moProp);
        }

        if (pv.getStringValue() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(STRING_VALUE_NAME);
            moProp.setData(pv.getStringValue());
            propList.add(moProp);
        }
        if (pv.getIntValue() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INT_VALUE_NAME);
            moProp.setData(pv.getIntValue().toString());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        mo.setName(PARAMETER_VALUE_MO);
        return mo;
    }

    public static ManagedObject getActionInstanceMo(ActionInstance ai) {
        if (ai == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (ai.getActionDefinitionId() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(ACTION_DEFINITION_ID_NAME);
            moProp.setData(ai.getActionDefinitionId().getValue());
            propList.add(moProp);
        }

        if (ai.getName() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NAME);
            moProp.setData(ai.getName().getValue());
            propList.add(moProp);
        }

        if (ai.getParameterValue() != null) {
            for (ParameterValue pv: ai.getParameterValue()) {
                children.add(getParameterMo(pv));
            }
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(ACTION_INSTANCE_MO);

        return mo;
    }

    public static ManagedObject getEndpointGroupMo(EndpointGroup epg) {
        if (epg == null) return null;
        List<ManagedObject> children = new ArrayList<ManagedObject>();

        ManagedObject mo = new ManagedObject();
        ManagedObject.Properties moProp;
        List<ManagedObject.Properties> propList = new ArrayList<ManagedObject.Properties>();

        if (epg.getRequirement() != null) {
            for (Requirement r: epg.getRequirement()) {
                children.add(getRequirementMo(r));
            }
        }
        if (epg.getCapability() != null) {
            for (Capability c: epg.getCapability()) {
                children.add(getCapabilityMo(c));
            }
        }
        if (epg.getConsumerNamedSelector() != null) {
            for (ConsumerNamedSelector cns: epg.getConsumerNamedSelector()) {
                children.add(getConsumerNamedSelectorMo(cns));
            }
        }
        if (epg.getConsumerTargetSelector() != null) {
            for (ConsumerTargetSelector cts: epg.getConsumerTargetSelector()) {
                children.add(getConsumerTargetSelectorMo(cts));
            }
        }
        if (epg.getProviderNamedSelector() != null) {
            for (ProviderNamedSelector pns: epg.getProviderNamedSelector()) {
                children.add(getProviderNamedSelectorMo(pns));
            }
        }
        if (epg.getProviderTargetSelector() != null) {
            for (ProviderTargetSelector pts: epg.getProviderTargetSelector()) {
                children.add(getProviderTargetSelectorMo(pts));
            }
        }

        if (epg.getId() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(ID_NAME);
            moProp.setData(epg.getId().getValue());
            propList.add(moProp);
        }

        if (epg.getIntraGroupPolicy() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(INTRAGROUP_POLICY_NAME);
            moProp.setData(epg.getIntraGroupPolicy().toString());
            propList.add(moProp);
        }

        if (epg.getNetworkDomain() != null) {
            moProp = new ManagedObject.Properties();
            moProp.setName(NETWORK_DOMAIN_NAME);
            moProp.setData(epg.getNetworkDomain().getValue());
            propList.add(moProp);
        }

        if (propList.size() > 0)
            mo.setProperties(propList);
        if (children.size() > 0)
            mo.setChildren(children);
        mo.setName(ENDPOINT_GROUP_MO);

        return mo;
    }

}
