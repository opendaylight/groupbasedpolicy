/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class IidSflowNameUtil {

    public static final String DELIMETER = "_-_";
    public static final String KEY_DELIMETER = "-_-";

    public static String createFlowCacheName(InstanceIdentifier<Classifier> classifierIid, FlowCacheCons.Value value) {
        PolicyRuleGroupKey policyRuleGroup = classifierIid.firstKeyOf(PolicyRuleGroup.class);
        ResolvedRuleKey resolvedRule = classifierIid.firstKeyOf(ResolvedRule.class);
        ClassifierKey classifier = classifierIid.firstKeyOf(Classifier.class);
        StringBuilder sb = new StringBuilder();
        sb.append(createStringFromCompositeKey(policyRuleGroup.getTenantId().getValue(),
                policyRuleGroup.getContractId().getValue(), policyRuleGroup.getSubjectName().getValue()))
            .append(DELIMETER)
            .append(resolvedRule.getName().getValue())
            .append(DELIMETER)
            .append(classifier.getName().getValue())
            .append(DELIMETER)
            .append(value.get());
        return sb.toString();
    }

    private static String createStringFromCompositeKey(String... keys) {
        return Joiner.on(KEY_DELIMETER).join(keys);
    }

    public static ContractId resolveContractIdFromFlowCacheName(String flowCacheName) {
        List<String> keys = Splitter.on(DELIMETER).splitToList(flowCacheName);
        String policyRuleGroupKey = keys.get(0);
        String contractId = Splitter.on(KEY_DELIMETER).splitToList(policyRuleGroupKey).get(1);
        return new ContractId(contractId);
    }

    public static SubjectName resolveSubjectNameFromFlowCacheName(String flowCacheName) {
        List<String> keys = Splitter.on(DELIMETER).splitToList(flowCacheName);
        String policyRuleGroupKey = keys.get(0);
        String subjectName = Splitter.on(KEY_DELIMETER).splitToList(policyRuleGroupKey).get(2);
        return new SubjectName(subjectName);
    }

    public static RuleName resolveRuleNameFromFlowCacheName(String flowCacheName) {
        List<String> keys = Splitter.on(DELIMETER).splitToList(flowCacheName);
        String ruleName = keys.get(1);
        return new RuleName(ruleName);
    }

    public static ClassifierName resolveClassifierNameFromFlowCacheName(String flowCacheName) {
        List<String> keys = Splitter.on(DELIMETER).splitToList(flowCacheName);
        String classifierName = keys.get(2);
        return new ClassifierName(classifierName);
    }

    public static String resolveFlowCacheValue(String flowCacheName) {
        List<String> keys = Splitter.on(DELIMETER).splitToList(flowCacheName);
        return keys.get(3);
    }
}
