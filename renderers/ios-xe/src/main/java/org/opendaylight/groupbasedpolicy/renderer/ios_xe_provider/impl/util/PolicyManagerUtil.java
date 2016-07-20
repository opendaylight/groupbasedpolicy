/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase.CHAIN;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriterUtil;
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
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChain.Direction;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyManagerUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerUtil.class);

    /**
     * Main method for policy creation which looks for all actions specified in rules between two endpoints. Whichever
     * action has been found, it is resolved (only chain action is supported for now).
     *
     * @param sourceSgt      - security group tag of source endpoint
     * @param destinationSgt - security group tag of destination endpoint
     * @param context        - stores info about location of classifier/policy-map and status
     * @param data           - new data, used to found appropriate rule group
     * @param peerEndpoint   - contains info about rule groups between endpoint pairs
     * @param dataBroker     - data provider for odl controller
     */
    public static void syncEndpointPairCreatePolicy(final Sgt sourceSgt, final Sgt destinationSgt,
                                                    final PolicyConfigurationContext context, final Configuration data,
                                                    final PeerEndpoint peerEndpoint, final DataBroker dataBroker) {
        // Create appropriate policy map
        if (!PolicyManagerUtil.constructEmptyPolicyMapWithInterface(context)) {
            final String policyMapName = context.getPolicyMapLocation().getPolicyMapName();
            final String interfaceName = context.getPolicyMapLocation().getInterfaceName();
            final String info = String.format("Unable to create policy-map %s on interface %s", policyMapName, interfaceName);
            context.appendUnconfiguredRendererEP(StatusUtil.assembleFullyNotConfigurableRendererEP(context, info));
            LOG.warn(info);
            return;
        }

        // Find actions from acquired data
        final Map<ActionCase, ActionInDirection> actionMap = PolicyManagerUtil.getActionInDirection(data, peerEndpoint);
        if (actionMap.isEmpty()) {
            LOG.debug("No usable action found for EP-sgt[{}] | peerEP-sgt[{}]", sourceSgt, destinationSgt);
            return;
        }

        // Chain action
        if (actionMap.containsKey(ActionCase.CHAIN)) {
            ServiceChainingUtil.newChainAction(peerEndpoint, sourceSgt, destinationSgt, actionMap, context,
                    dataBroker);
        }
    }

    /**
     * Method for policy removal which looks for all actions specified in rules between two endpoints. Whichever
     * action has been found, it is resolved (only chain action is supported).
     *
     * @param sourceSgt      - security group tag of source endpoint
     * @param destinationSgt - security group tag of destination endpoint
     * @param context        - stores info about location of classifier/policy-map and status
     * @param data           - data used to identify all elements marked to remove
     * @param peerEndpoint   - contains info about rule groups between endpoint pairs
     */
    public static void syncEndpointPairRemovePolicy(final Sgt sourceSgt, final Sgt destinationSgt,
                                                    final PolicyConfigurationContext context, final Configuration data,
                                                    final PeerEndpoint peerEndpoint) {
        // Find actions from acquired data
        final Map<ActionCase, ActionInDirection> actionMap = PolicyManagerUtil.getActionInDirection(data, peerEndpoint);
        if (actionMap.isEmpty()) {
            LOG.debug("no usable action found for EP-sgt[{}] | peerEP-sgt[{}]",
                    sourceSgt, destinationSgt);
            return;
        }

        // Chain action
        if (actionMap.containsKey(ActionCase.CHAIN)) {
            ServiceChainingUtil.resolveRemovedChainAction(peerEndpoint, sourceSgt, destinationSgt, actionMap,
                    context);
        }

        // Remove policy-map if empty
        if (!deleteEmptyPolicyMapWithInterface(context.getPolicyMapLocation())) {
            final PolicyManagerImpl.PolicyMapLocation location = context.getPolicyMapLocation();
            final String info = String.format("Unable to remove policy-map %s and interface %s", location.getPolicyMapName(),
                    location.getInterfaceName());
            LOG.warn(info);
        }
    }

    /**
     * According to info from {@link RuleGroupWithRendererEndpointParticipation} (composite key) finds appropriate subject
     *
     * @param data                       - contains all rule groups
     * @param ruleGroupWithParticipation - contains info about how to find right rule group
     * @return rule group if found, null otherwise
     */
    @Nullable
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

    @Nullable
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

    /**
     * Creates empty policy-map if does not exist and bounds it to interface if it is not. If policy-map exists, method
     * checks whether it is connected to correct interface and creates it if necessary. If policy-map does not exist,
     * it is created with particular interface
     *
     * @param context - all data required to create/localize policy-map
     * @return true if policy-map and interface is present/written on the device, false otherwise
     */
    private static boolean constructEmptyPolicyMapWithInterface(final PolicyConfigurationContext context) {
        final PolicyManagerImpl.PolicyMapLocation policyMapLocation = context.getPolicyMapLocation();
        final String policyMapName = policyMapLocation.getPolicyMapName();
        final DataBroker mountpoint = policyMapLocation.getMountpoint();
        final String interfaceName = policyMapLocation.getInterfaceName();
        final NodeId nodeId = policyMapLocation.getNodeId();
        final InstanceIdentifier<PolicyMap> policyMapIid = PolicyWriterUtil.policyMapInstanceIdentifier(policyMapName);
        final Optional<PolicyMap> optionalPolicyMap =
                Optional.ofNullable(PolicyWriterUtil.netconfRead(mountpoint, policyMapIid));
        if (optionalPolicyMap.isPresent()) {
            LOG.trace("Policy map with name {} on interface {} already exists", policyMapName, interfaceName);
            final InstanceIdentifier<ServicePolicy> servicePolicyIid = PolicyWriterUtil.interfaceInstanceIdentifier(interfaceName);
            final Optional<ServicePolicy> optionalServicePolicy =
                    Optional.ofNullable(PolicyWriterUtil.netconfRead(mountpoint, servicePolicyIid));
            if (optionalServicePolicy.isPresent()) {
                LOG.trace("Policy map {} is bound to correct interface {} ", policyMapName, interfaceName);
                return true;
            } else {
                boolean iResult = PolicyWriterUtil.writeInterface(context.getPolicyMapLocation());
                context.setFutureResult(Futures.immediateCheckedFuture(iResult));
                return iResult;
            }
        } else {
            final PolicyMap emptyMap = createEmptyPolicyMap(policyMapName);
            boolean pmResult = PolicyWriterUtil.writePolicyMap(emptyMap, context.getPolicyMapLocation());
            context.setFutureResult(Futures.immediateCheckedFuture(pmResult));
            if (pmResult) {
                LOG.info("Created policy-map {} on node {}", policyMapName, nodeId.getValue());
                LOG.trace("Adding policy-map {} to interface {}", policyMapName, interfaceName);
                boolean iResult = PolicyWriterUtil.writeInterface(context.getPolicyMapLocation());
                context.setFutureResult(Futures.immediateCheckedFuture(iResult));
                return iResult;
            }
            return false;
        }
    }

    /**
     * Removes empty policy-map and its interface
     *
     * @param policyMapLocation - location of policy-map
     * @return true if policy-map is present and not empty or if it is successfully removed also with interface, false
     * otherwise
     */
    private static boolean deleteEmptyPolicyMapWithInterface(PolicyManagerImpl.PolicyMapLocation policyMapLocation) {
        final String policyMapName = policyMapLocation.getPolicyMapName();
        final DataBroker mountpoint = policyMapLocation.getMountpoint();
        final InstanceIdentifier<PolicyMap> policyMapIid = PolicyWriterUtil.policyMapInstanceIdentifier(policyMapName);
        // Read policy map
        final Optional<PolicyMap> optionalPolicyMap = Optional.ofNullable(PolicyWriterUtil.netconfRead(mountpoint, policyMapIid));
        if (optionalPolicyMap.isPresent()) {
            final PolicyMap policyMap = optionalPolicyMap.get();
            if (policyMap.getXmlClass() == null || policyMap.getXmlClass().isEmpty()) {
                // No entries, remove
                if (PolicyWriterUtil.removePolicyMap(policyMapLocation)) {
                    // Remove interface binding if exists
                    LOG.info("Policy-map {} removed", policyMapName);
                    return PolicyWriterUtil.removeInterface(policyMapLocation);
                }
                return false;
            }
            LOG.debug("Policy-map {} still contains entries, cannot be removed", policyMapLocation.getPolicyMapName());
            return true;
        }
        return true;

    }

    public static ServicePolicy createServicePolicy(final String chainName, final Direction direction) {
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

    private static PolicyMap createEmptyPolicyMap(String policyMapName) {
        // TODO maybe could be better to create also class-default entry with pass-through value than not to create any default entry at all
        // Construct policy map
        final PolicyMapBuilder policyMapBuilder = new PolicyMapBuilder();
        policyMapBuilder.setName(policyMapName)
                .setKey(new PolicyMapKey(policyMapName))
                .setType(PolicyMap.Type.ServiceChain);
        return policyMapBuilder.build();
    }

    static Class createPolicyMapEntry(final String policyClassName, final RenderedServicePath renderedPath,
                                      final ActionCase actionCase) {
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

    @Nonnull
    static ClassMap createClassMap(final String classMapName, final Match match) {
        final ClassMapBuilder cmBuilder = new ClassMapBuilder();
        cmBuilder.setName(classMapName)
                .setKey(new ClassMapKey(classMapName))
                .setPrematch(ClassMap.Prematch.MatchAll)
                .setMatch(match);
        return cmBuilder.build();
    }

    /**
     * Constructs {@link ActionInDirection} object with {@link ActionCase} as a key. ActionInDirection object contains
     * info about action, participation and rule direction.
     *
     * @param data - used for finding rule's rule group
     * @param peer - contains {@link RuleGroupWithRendererEndpointParticipation}
     * @return map with actionCase/ActionInDirection entries, empty map if no rule is found
     */
    @Nonnull
    private static Map<ActionCase, ActionInDirection> getActionInDirection(final Configuration data,
                                                                           final PeerEndpoint peer) {
        final Set<ResolvedRule> rulesInDirection = new HashSet<>();
        EndpointPolicyParticipation participation = null;
        HasDirection.Direction direction = null;
        // Find all rules in desired direction
        for (RuleGroupWithRendererEndpointParticipation ruleGroupKey :
                peer.getRuleGroupWithRendererEndpointParticipation()) {
            participation = ruleGroupKey.getRendererEndpointParticipation();
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
                // TODO only first Classifier used
                final Classifier classifier = resolvedRule.getClassifier().get(0);
                direction = classifier.getDirection();
                rulesInDirection.add(resolvedRule);
            }
        }
        if (rulesInDirection.isEmpty()) {
            return Collections.emptyMap();
        }
        // TODO use only first rule with ActionDefinitionID for now
        final Map<ActionCase, ActionInDirection> result = new HashMap<>();
        for (ResolvedRule resolvedRule : rulesInDirection) {
            // TODO only first action used for now
            final Action action = resolvedRule.getAction().get(0);
            final RuleName name = resolvedRule.getName();
            if (action.getActionDefinitionId() != null) {
                final ActionDefinitionId actionDefinitionId = action.getActionDefinitionId();
                // Currently only chain action is supported
                if (actionDefinitionId.equals(ChainActionDefinition.ID)) {
                    ActionInDirection actionInDirection = new ActionInDirection(name, action, participation, direction);
                    result.put(ActionCase.CHAIN, actionInDirection);
                    return result;
                }
            }
        }
        return Collections.emptyMap();
    }

    public static InstanceIdentifier getMountpointIidFromAbsoluteLocation(final RendererEndpoint endpoint,
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

    public static String getInterfaceNameFromAbsoluteLocation(final RendererEndpoint endpoint,
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

    /**
     * Action in Direction - wrapper class
     */
    static class ActionInDirection {

        private final RuleName ruleName;
        private final Action action;
        private final EndpointPolicyParticipation participation;
        private final HasDirection.Direction direction;

        ActionInDirection(final RuleName ruleName,
                          final Action action,
                          final EndpointPolicyParticipation participation,
                          final HasDirection.Direction direction) {
            this.ruleName = Preconditions.checkNotNull(ruleName);
            this.action = Preconditions.checkNotNull(action);
            this.participation = Preconditions.checkNotNull(participation);
            this.direction = Preconditions.checkNotNull(direction);
        }

        RuleName getRuleName() {
            return ruleName;
        }

        Action getAction() {
            return action;
        }

        EndpointPolicyParticipation getParticipation() {
            return participation;
        }

        HasDirection.Direction getDirection() {
            return direction;
        }
    }
}
