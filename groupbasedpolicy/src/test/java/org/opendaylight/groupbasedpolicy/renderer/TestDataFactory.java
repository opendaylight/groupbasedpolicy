/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.AddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoint.locations.ContainmentEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.containment.endpoints.ContainmentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.InternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.AddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.rev160427.ContextType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.ResolvedPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.PolicyRuleGroupWithEndpointConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.resolved.policies.resolved.policy.policy.rule.group.with.endpoint.constraints.PolicyRuleGroupBuilder;

import com.google.common.collect.ImmutableList;

public class TestDataFactory {

    public static final TenantId TENANT_ID = new TenantId("cisco_tenant");
    public static final ActionDefinitionId AD_1 = new ActionDefinitionId("ad_1");
    public static final ActionName AN_1 = new ActionName("an_1");
    public static final ClassifierDefinitionId CD_1 = new ClassifierDefinitionId("cd_1");
    public static final ClassifierName CN_1 = new ClassifierName("cn_1");
    public static final ContextId CTX_1 = new ContextId("ctx_1");

    public static AddressEndpointLocationBuilder defaultAdrEpLoc(AddressEndpointKey adrEpKey, LocationType realLoc,
            InternalLocation... relativeLocs) {
        AddressEndpointLocationBuilder result =
                new AddressEndpointLocationBuilder().setContextType(adrEpKey.getContextType())
                    .setContextId(adrEpKey.getContextId())
                    .setAddressType(adrEpKey.getAddressType())
                    .setAddress(adrEpKey.getAddress())
                    .setAbsoluteLocation(new AbsoluteLocationBuilder().setLocationType(realLoc).build());
        if (relativeLocs != null) {
            result.setRelativeLocations(
                    new RelativeLocationsBuilder().setInternalLocation(Arrays.asList(relativeLocs)).build());
        }
        return result;
    }

    public static ContainmentEndpointLocationBuilder defaultContEpLoc(ContainmentEndpointKey contEpKey,
            InternalLocation... relativeLocs) {
        ContainmentEndpointLocationBuilder result = new ContainmentEndpointLocationBuilder()
            .setContextType(contEpKey.getContextType()).setContextId(contEpKey.getContextId()).setRelativeLocations(
                    new RelativeLocationsBuilder().setInternalLocation(Arrays.asList(relativeLocs)).build());
        return result;
    }

    public static AddressEndpointBuilder defaultAdrEp(String address, EndpointGroupId... epgs) {
        return new AddressEndpointBuilder().setContextType(ContextType.class)
            .setContextId(CTX_1)
            .setAddressType(AddressType.class)
            .setAddress(address)
            .setTenant(TENANT_ID)
            .setEndpointGroup(Arrays.asList(epgs))
            .setNetworkContainment(
                    new NetworkContainmentBuilder().setContainment(new ForwardingContextContainmentBuilder()
                        .setContextType(ContextType.class).setContextId(CTX_1).build()).build());
    }

    public static ContainmentEndpointBuilder defaultContEp(EndpointGroupId... epgs) {
        return new ContainmentEndpointBuilder().setContextType(ContextType.class)
            .setContextId(CTX_1)
            .setTenant(TENANT_ID)
            .setEndpointGroup(Arrays.asList(epgs))
            .setNetworkContainment(
                    new NetworkContainmentBuilder().setContainment(new ForwardingContextContainmentBuilder()
                        .setContextType(ContextType.class).setContextId(CTX_1).build()).build());
    }

    public static ResolvedRuleBuilder defaultResolvedRule(RuleName ruleName) {
        Action action = new ActionBuilder().setActionDefinitionId(AD_1).setName(AN_1).setOrder(0).build();
        Classifier classifier = new ClassifierBuilder().setClassifierDefinitionId(CD_1)
            .setName(CN_1)
            .setDirection(Direction.In)
            .build();
        return new ResolvedRuleBuilder().setName(ruleName)
            .setOrder(0)
            .setAction(ImmutableList.of(action))
            .setClassifier(ImmutableList.of(classifier));
    }

    public static PolicyRuleGroupBuilder defaultPolicyRuleGrp(ContractId contractId, SubjectName subjectName,
            ResolvedRule... resolvedRules) {
        return new PolicyRuleGroupBuilder().setTenantId(TENANT_ID)
            .setContractId(contractId)
            .setSubjectName(subjectName)
            .setResolvedRule(Arrays.asList(resolvedRules));
    }

    public static ResolvedPolicyBuilder defaultResolvedPolicy(EndpointGroupId consEpg, EndpointGroupId provEpg,
            PolicyRuleGroup... policyRuleGrps) {
        PolicyRuleGroupWithEndpointConstraints blueRuleGrpWithoutCons =
                new PolicyRuleGroupWithEndpointConstraintsBuilder().setPolicyRuleGroup(Arrays.asList(policyRuleGrps))
                    .build();
        return new ResolvedPolicyBuilder().setConsumerEpgId(consEpg)
            .setConsumerTenantId(TENANT_ID)
            .setProviderEpgId(provEpg)
            .setProviderTenantId(TENANT_ID)
            .setPolicyRuleGroupWithEndpointConstraints(ImmutableList.of(blueRuleGrpWithoutCons));
    }

}
