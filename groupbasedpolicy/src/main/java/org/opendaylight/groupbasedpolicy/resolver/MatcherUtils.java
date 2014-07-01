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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.CapabilityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.LabelName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.QualityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RelatorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RequirementName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TargetName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.ConsumerSelectionRelator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.ProviderSelectionRelator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Relator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.capabilities.Capability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.qualities.Quality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.requirements.Requirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.quality.matcher.MatcherQuality;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.RequirementMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.consumer.matchers.requirement.matcher.MatcherRequirement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.CapabilityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.provider.matchers.capability.matcher.MatcherCapability;

/**
 * Utilities related to matchers and labels
 * @author readams
 */
public class MatcherUtils {
    /**
     * Apply a quality matcher to a normalized target
     * @param matcher the {@link QualityMatcher} to apply
     * @param target the {@link Target} to match against
     * @return <code>true</code> if the matcher matches the target
     */
    public static boolean applyQualityMatcher(QualityMatcher matcher,
                                              Target target) {
        List<MatcherLabel<QualityName,TargetName>> mls = new ArrayList<>();
        if (matcher.getMatcherQuality() != null) {
            for (MatcherQuality ml : matcher.getMatcherQuality()) {
                mls.add(new MatcherLabel<>(ml.getName(), 
                        ml.getTargetNamespace()));
            }
        }
        Set<QualityName> toMatch = new HashSet<>();
        for (Quality q : target.getQuality()) {
            toMatch.add(q.getName());
        }
        return applyLabelMatcher(mls, matcher.getMatchType(), 
                                 toMatch, target.getName());
    }

    /**
     * Apply a requirement matcher to a normalized consumer selection relator
     * @param matcher the {@link RequirementMatcher} to apply
     * @param target the {@link ConsumerSelectionRelator} to match against
     * @return <code>true</code> if the matcher matches the target
     */
    public static boolean applyReqMatcher(RequirementMatcher matcher,
                                          ConsumerSelectionRelator relator) {
        List<MatcherLabel<RequirementName,SelectorName>> mls = new ArrayList<>();
        if (matcher.getMatcherRequirement() != null) {
            for (MatcherRequirement ml : matcher.getMatcherRequirement()) {
                mls.add(new MatcherLabel<>(ml.getName(), 
                                           ml.getSelectorNamespace()));
            }
        }
        Set<RequirementName> toMatch = new HashSet<>();
        for (Requirement q : relator.getRequirement()) {
            toMatch.add(q.getName());
        }
        return applyLabelMatcher(mls, matcher.getMatchType(), 
                                 toMatch, relator.getName());
    }

    /**
     * Apply a capability matcher to a normalized provider selection relator
     * @param matcher the {@link RequirementMatcher} to apply
     * @param target the {@link ProviderSelectionRelator} to match against
     * @return <code>true</code> if the matcher matches the target
     */
    public static boolean applyCapMatcher(CapabilityMatcher matcher,
                                          ProviderSelectionRelator relator) {
        List<MatcherLabel<CapabilityName,SelectorName>> mls = new ArrayList<>();
        if (matcher.getMatcherCapability() != null) { 
            for (MatcherCapability ml : matcher.getMatcherCapability()) {
                mls.add(new MatcherLabel<>(ml.getName(), 
                                           ml.getSelectorNamespace()));
            }
        }
        Set<CapabilityName> toMatch = new HashSet<>();
        for (Capability q : relator.getCapability()) {
            toMatch.add(q.getName());
        }
        return applyLabelMatcher(mls, matcher.getMatchType(), 
                                 toMatch, relator.getName());
    }
   
    /**
     * Functional interface used for generic label methods
     * @author readams
     *
     * @param <L> The specific label type
     * @param <LN> the related label name type
     */
    public interface GetLabelName<L extends Label, LN extends LabelName> {
        /**
         * Get the appropriate typed name for the given label
         * @param label the label
         * @return the name
         */
        public LN getName(L label);
    }
    
    /**
     * A {@link GetLabelName} for qualities
     */
    public static final GetLabelName<Quality, QualityName> getQualityName = 
        new GetLabelName<Quality, QualityName>() {
        @Override
        public QualityName getName(Quality label) {
            return label.getName();
        }
    };
    
    /**
     * A {@link GetLabelName} for matcher qualities
     */
    public static final GetLabelName<MatcherQuality, QualityName> getMatcherQualityName = 
            new GetLabelName<MatcherQuality, QualityName>() {
        @Override
        public QualityName getName(MatcherQuality label) {
            return label.getName();
        }
    };
    
    /**
     * A {@link GetLabelName} for requirements
     */
    public static final GetLabelName<Requirement, RequirementName> getRequirementName = 
            new GetLabelName<Requirement, RequirementName>() {
        @Override
        public RequirementName getName(Requirement label) {
            return label.getName();
        }
    };
    
    /**
     * A {@link GetLabelName} for matcher requirements
     */
    public static final GetLabelName<MatcherRequirement, RequirementName> getMatcherRequirementName = 
            new GetLabelName<MatcherRequirement, RequirementName>() {
        @Override
        public RequirementName getName(MatcherRequirement label) {
            return label.getName();
        }
    };

    /**
     * A {@link GetLabelName} for capabilities
     */
    public static final GetLabelName<Capability, CapabilityName> getCapabilityName = 
            new GetLabelName<Capability, CapabilityName>() {
        @Override
        public CapabilityName getName(Capability label) {
            return label.getName();
        }
    };

    /**
     * A {@link GetLabelName} for matcher capabilities
     */
    public static final GetLabelName<MatcherCapability, CapabilityName> getMatcherCapabilityName = 
            new GetLabelName<MatcherCapability, CapabilityName>() {
        @Override
        public CapabilityName getName(MatcherCapability label) {
            return label.getName();
        }
    };

    /**
     * A {@link GetLabelName} for capabilities
     */
    public static final GetLabelName<Condition, ConditionName> getConditionName = 
            new GetLabelName<Condition, ConditionName>() {
        @Override
        public ConditionName getName(Condition label) {
            return label.getName();
        }
    };
    
    @Immutable
    private static class MatcherLabel<LN extends LabelName, 
                                      NS extends RelatorName> {
        final LN name;
        final NS namespace;

        public MatcherLabel(LN name, NS namespace) {
            super();
            this.name = name;
            this.namespace = namespace;
        }
    }
    private static <LN extends LabelName, L extends Label, 
             NS extends RelatorName, R extends Relator>         
        boolean applyLabelMatcher(Collection<MatcherLabel<LN, NS>> matcherLabels,
                                  MatchType matchType,
                                  Set<LN> toMatch,
                                  NS matchNamespace) {
        int matches = 0;
        int matchersize = 0;
        if (matcherLabels != null) {
            matchersize = matcherLabels.size();
            for (MatcherLabel<LN, NS> matcherLabel : matcherLabels) {
                if (matcherLabel.namespace != null && 
                    !matcherLabel.namespace.equals(matchNamespace))
                    continue;
                if (!toMatch.contains(matcherLabel.name))
                    continue;
                matches += 1;
            }
        }
        if (matchType == null) matchType = MatchType.All;
        switch (matchType) {
        case Any:
            return matches > 0;
        case None:
            return matches == 0;
        case All:
        default:
            return matches == matchersize;
        }
    }
}
