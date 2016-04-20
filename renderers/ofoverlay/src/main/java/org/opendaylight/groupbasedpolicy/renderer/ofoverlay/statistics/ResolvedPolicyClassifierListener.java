/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.ResolvedPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public class ResolvedPolicyClassifierListener extends DataTreeChangeHandler<ResolvedPolicy> {

    private static final Logger LOG = LoggerFactory.getLogger(ResolvedPolicyClassifierListener.class);
    private final OFStatisticsManager ofStatsManager;

    public ResolvedPolicyClassifierListener(DataBroker dataProvider, OFStatisticsManager ofStatsManager) {
        super(dataProvider);
        this.ofStatsManager = checkNotNull(ofStatsManager);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(ResolvedPolicies.class).child(ResolvedPolicy.class).build()));
    }

    @Override
    protected void onWrite(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        ResolvedPolicy resolvedPolicy = rootNode.getDataAfter();
        Map<InstanceIdentifier<Classifier>, Classifier> classifierByIid =
                resolveClassifiers(resolvedPolicy, rootIdentifier);
        for (Entry<InstanceIdentifier<Classifier>, Classifier> classfierEntry : classifierByIid.entrySet()) {
            LOG.trace("New classifier created: {}\n{}", classfierEntry.getKey(), classfierEntry.getValue());
            ofStatsManager.pullStatsForClassifier(classfierEntry.getKey(), classfierEntry.getValue());
        }
    }

    @Override
    protected void onDelete(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        LOG.debug("Delete is not supported yet.");
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<ResolvedPolicy> rootNode,
            InstanceIdentifier<ResolvedPolicy> rootIdentifier) {
        ResolvedPolicy resolvedPolicyAfter = rootNode.getDataAfter();
        ResolvedPolicy resolvedPolicyBefore = rootNode.getDataBefore();
        Map<InstanceIdentifier<Classifier>, Classifier> classifierByIidAfter =
                resolveClassifiers(resolvedPolicyAfter, rootIdentifier);
        Map<InstanceIdentifier<Classifier>, Classifier> classifierByIidBefore =
                resolveClassifiers(resolvedPolicyBefore, rootIdentifier);
        MapDifference<InstanceIdentifier<Classifier>, Classifier> difference =
                Maps.difference(classifierByIidBefore, classifierByIidAfter);
        Map<InstanceIdentifier<Classifier>, Classifier> createdClassifierByIid = difference.entriesOnlyOnRight();
        for (Entry<InstanceIdentifier<Classifier>, Classifier> createdClassfierEntry : createdClassifierByIid
            .entrySet()) {
            LOG.trace("New classifier created: {}\n{}", createdClassfierEntry.getKey(),
                    createdClassfierEntry.getValue());
            ofStatsManager.pullStatsForClassifier(createdClassfierEntry.getKey(), createdClassfierEntry.getValue());
        }
        // TODO missing impl for case when classifier is changed or removed
    }

    private Map<InstanceIdentifier<Classifier>, Classifier> resolveClassifiers(ResolvedPolicy resolvedPolicy,
            InstanceIdentifier<ResolvedPolicy> resolvedPolicyIid) {
        List<PolicyRuleGroupWithEndpointConstraints> policyRgWithEcs =
                resolvedPolicy.getPolicyRuleGroupWithEndpointConstraints();
        if (policyRgWithEcs == null) {
            return Collections.emptyMap();
        }
        Map<InstanceIdentifier<Classifier>, Classifier> result = new HashMap<>();
        for (PolicyRuleGroupWithEndpointConstraints policyRgWithEc : policyRgWithEcs) {
            List<PolicyRuleGroup> policyRuleGroups = policyRgWithEc.getPolicyRuleGroup();
            if (policyRuleGroups == null) {
                continue;
            }
            for (PolicyRuleGroup policyRuleGroup : policyRuleGroups) {
                List<ResolvedRule> resolvedRules = policyRuleGroup.getResolvedRule();
                if (resolvedRules == null) {
                    continue;
                }
                for (ResolvedRule resolvedRule : resolvedRules) {
                    List<Classifier> classifiers = resolvedRule.getClassifier();
                    if (classifiers == null) {
                        continue;
                    }
                    for (Classifier classifier : classifiers) {
                        InstanceIdentifier<Classifier> classifierIid = resolvedPolicyIid.builder()
                            .child(PolicyRuleGroupWithEndpointConstraints.class)
                            .child(PolicyRuleGroup.class, policyRuleGroup.getKey())
                            .child(ResolvedRule.class, resolvedRule.getKey())
                            .child(Classifier.class, classifier.getKey())
                            .build();
                        result.put(classifierIid, classifier);
                    }
                }
            }
        }
        return result;
    }

}
