/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase.CHAIN;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.DsAction.Create;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.DsAction.Delete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.ClassNameType;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.PolicyActionType;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.SecurityGroup;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.SecurityGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.Destination;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.Source;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.ServicePolicy;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.ServicePolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.TypeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.Match;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.ActionList;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.ActionListBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.ActionListKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.ForwardCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.ForwardBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.forward.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.forward.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.forward.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyManagerUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerUtil.class);
    private static final String DEFAULT = "class-default";

    public static void syncPolicyEntities(final Sgt sourceSgt, final Sgt destinationSgt, final PolicyConfigurationContext context,
                                          final Configuration dataAfter, final PeerEndpoint peerEndpoint,
                                          final DataBroker dataBroker, final PolicyManagerImpl.DsAction action) {
        // Action
        final Map<PolicyManagerImpl.ActionCase, Action> actionMap = PolicyManagerUtil.getActionInDirection(dataAfter, peerEndpoint);
        if (actionMap == null || actionMap.isEmpty()) {
            LOG.debug("no usable action found for EP-sgt[{}] | peerEP-sgt[{}]",
                    sourceSgt, destinationSgt);
            return;
        }

        // TODO allow action not supported

        // Resolve chain action - create
        if (actionMap.containsKey(PolicyManagerImpl.ActionCase.CHAIN) && action.equals(Create)) {
            ServiceChainingUtil.resolveNewChainAction(peerEndpoint, sourceSgt, destinationSgt, actionMap, context,
                    dataBroker);
        }
        if (actionMap.containsKey(PolicyManagerImpl.ActionCase.CHAIN) && action.equals(Delete)) {
            ServiceChainingUtil.removeChainAction(peerEndpoint, sourceSgt, destinationSgt, actionMap,
                    context.getPolicyWriter());
        }
    }

    public static Sgt findSgtTag(final AddressEndpointKey endpointKey,
                                 final List<AddressEndpointWithLocation> endpointsWithLocation) {
        if (endpointKey == null || endpointsWithLocation == null) {
            return null;
        }
        final AddressEndpointWithLocation endpointWithLocation = RendererPolicyUtil.lookupEndpoint(endpointKey,
                endpointsWithLocation);
        final AddressEndpointWithLocationAug augmentation = endpointWithLocation.getAugmentation(AddressEndpointWithLocationAug.class);
        if (augmentation == null) {
            return null;
        }

        return augmentation.getSgt();
    }

    static Match createSecurityGroupMatch(final int sourceTag, final int destinationTag) {
        final SecurityGroupBuilder sgBuilder = new SecurityGroupBuilder();
        final Source source = new SourceBuilder().setTag(sourceTag).build();
        final Destination destination = new DestinationBuilder().setTag(destinationTag).build();
        sgBuilder.setDestination(destination)
                .setSource(source);
        final SecurityGroup securityGroup = sgBuilder.build();
        final MatchBuilder matchBuilder = new MatchBuilder();
        matchBuilder.setSecurityGroup(securityGroup);
        return matchBuilder.build();
    }

    static ClassMap createClassMap(final String classMapName, final Match match) {
        final ClassMapBuilder cmBuilder = new ClassMapBuilder();
        cmBuilder.setName(classMapName)
                .setKey(new ClassMapKey(classMapName))
                .setPrematch(ClassMap.Prematch.MatchAll)
                .setMatch(match);
        return cmBuilder.build();
    }

    static TenantId getTenantId(final PeerEndpoint peer) {
        for (RuleGroupWithRendererEndpointParticipation ruleGroup :
                peer.getRuleGroupWithRendererEndpointParticipation()) {
            if (ruleGroup.getTenantId() != null) {
                return ruleGroup.getTenantId();
            }
        }
        return null;
    }

    static String generateClassMapName(final int sourceTag, final int destinationTag) {
        return "srcTag" + sourceTag + "_dstTag" + destinationTag;
    }

    static Class createPolicyEntry(final String policyClassName, final RenderedServicePath renderedPath,
                                   final PolicyManagerImpl.ActionCase actionCase) {
        // Forward Case
        final ForwardCaseBuilder forwardCaseBuilder = new ForwardCaseBuilder();
        if (actionCase.equals(CHAIN) && renderedPath != null) {
            // Chain Action
            final ForwardBuilder forwardBuilder = new ForwardBuilder();
            final List<ServicePath> servicePaths = new ArrayList<>();
            final ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
            servicePathBuilder.setKey(new ServicePathKey(renderedPath.getPathId()))
                    .setServicePathId(renderedPath.getPathId())
                    .setServiceIndex(renderedPath.getStartingIndex());
            servicePaths.add(servicePathBuilder.build());
            forwardBuilder.setServicePath(servicePaths);
            forwardCaseBuilder.setForward(forwardBuilder.build());
        }
        // Create Action List
        final List<ActionList> actionList = new ArrayList<>();
        final ActionListBuilder actionListBuilder = new ActionListBuilder();
        actionListBuilder.setKey(new ActionListKey(PolicyActionType.Forward))
                .setActionType(PolicyActionType.Forward)
                .setActionParam(forwardCaseBuilder.build());
        actionList.add(actionListBuilder.build());
        // Build class entry
        final ClassBuilder policyClassBuilder = new ClassBuilder();
        policyClassBuilder.setName(new ClassNameType(policyClassName))
                .setKey(new ClassKey(new ClassNameType(policyClassName)))
                .setActionList(actionList);
        return policyClassBuilder.build();
    }

    public static PolicyMap createPolicyMap(final String policyMapName, final List<Class> policyMapEntries) {
        // Create default class entry
        final ClassBuilder defaultBuilder = new ClassBuilder();
        defaultBuilder.setName(new ClassNameType(DEFAULT))
                .setKey(new ClassKey(new ClassNameType(DEFAULT)));
        // TODO add pass-through value
        policyMapEntries.add(defaultBuilder.build());
        // Construct policy map
        final PolicyMapBuilder policyMapBuilder = new PolicyMapBuilder();
        policyMapBuilder.setName(policyMapName)
                .setKey(new PolicyMapKey(policyMapName))
                .setType(PolicyMap.Type.ServiceChain)
                .setXmlClass(policyMapEntries);
        return policyMapBuilder.build();
    }

    public static ServicePolicy createServicePolicy(final String chainName, final ServiceChain.Direction direction) {
        // Service Chain
        final ServiceChainBuilder serviceChainBuilder = new ServiceChainBuilder();
        serviceChainBuilder.setName(chainName) // Same as the policy map name
                .setDirection(direction);
        // Service policy
        final TypeBuilder typeBuilder = new TypeBuilder();
        typeBuilder.setServiceChain(serviceChainBuilder.build());
        // Service Policy
        ServicePolicyBuilder servicePolicyBuilder = new ServicePolicyBuilder();
        servicePolicyBuilder.setType(typeBuilder.build());

        return servicePolicyBuilder.build();
    }

    public static InstanceIdentifier getAbsoluteLocationMountpoint(final RendererEndpoint endpoint,
                                                                   final List<AddressEndpointWithLocation> endpointsWithLocation) {
        if (endpointsWithLocation.isEmpty()) {
            return null;
        }
        AddressEndpointWithLocation endpointWithLocation = RendererPolicyUtil.lookupEndpoint(endpoint,
                endpointsWithLocation);
        final AbsoluteLocation absoluteLocation = endpointWithLocation.getAbsoluteLocation();
        final LocationType locationType = absoluteLocation.getLocationType();
        ExternalLocationCase location = (ExternalLocationCase) locationType;
        if (location == null) {
            LOG.warn("Endpoint {} does not contain info about external location",
                    endpointWithLocation.getKey().toString());
            return null;
        }
        return location.getExternalNodeMountPoint();
    }

    public static String getInterfaceNameForPolicyMap(final RendererEndpoint endpoint,
                                                      final List<AddressEndpointWithLocation> endpointsWithLocation) {
        if (endpoint == null || endpointsWithLocation == null) {
            return null;
        }
        final AddressEndpointWithLocation endpointWithLocation = RendererPolicyUtil.lookupEndpoint(endpoint,
                endpointsWithLocation);
        final AbsoluteLocation absoluteLocation = endpointWithLocation.getAbsoluteLocation();
        final LocationType locationType = absoluteLocation.getLocationType();
        final ExternalLocationCase location = (ExternalLocationCase) locationType;
        if (location == null) {
            LOG.warn("Endpoint {} does not contain info about external location",
                    endpointWithLocation.getKey().toString());
            return null;
        }
        return location.getExternalNodeConnector();
    }


    private static Map<PolicyManagerImpl.ActionCase, Action> getActionInDirection(final Configuration data, final PeerEndpoint peer) {
        final Set<ResolvedRule> rulesInDirection = new HashSet<>();
        // Find all rules in desired direction
        for (RuleGroupWithRendererEndpointParticipation ruleGroupKey :
                peer.getRuleGroupWithRendererEndpointParticipation()) {
            final RuleGroup ruleGroup = findRuleGroup(data, ruleGroupKey);
            if (ruleGroup == null || ruleGroup.getResolvedRule() == null) {
                continue;
            }

            for (ResolvedRule resolvedRule : ruleGroup.getResolvedRule()) {
                if (resolvedRule == null) {
                    continue;
                }
                if (resolvedRule.getClassifier() == null || resolvedRule.getAction() == null) {
                    continue;
                }
                rulesInDirection.add(resolvedRule);
            }
        }
        if (rulesInDirection.isEmpty()) {
            return null;
        }
        // TODO use only first rule with ActionDefinitionID for now
        final Map<PolicyManagerImpl.ActionCase, Action> result = new HashMap<>();
        for (ResolvedRule resolvedRule : rulesInDirection) {
            // TODO only first action used for now
            final Action action = resolvedRule.getAction().get(0);
            if (action.getActionDefinitionId() != null) {
                final ActionDefinitionId actionDefinitionId = action.getActionDefinitionId();
                // Currently only chain action is supported
                if (actionDefinitionId.equals(ChainActionDefinition.ID)) {
                    result.put(PolicyManagerImpl.ActionCase.CHAIN, action);
                    return result;
                }
            }
        }
        return null;
    }

    private static RuleGroup findRuleGroup(final Configuration data,
                                           final RuleGroupWithRendererEndpointParticipation ruleGroupWithParticipation) {
        final TenantId tenantId = ruleGroupWithParticipation.getTenantId();
        final ContractId contractId = ruleGroupWithParticipation.getContractId();
        final SubjectName subjectName = ruleGroupWithParticipation.getSubjectName();
        for (RuleGroup ruleGroup : data.getRuleGroups().getRuleGroup()) {
            if (!ruleGroup.getTenantId().equals(tenantId)) {
                continue;
            }
            if (!ruleGroup.getContractId().equals(contractId)) {
                continue;
            }
            if (ruleGroup.getSubjectName().equals(subjectName)) {
                return ruleGroup;
            }
        }
        return null;
    }
}
