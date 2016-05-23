/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.cache.DSTreeBasedCache;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.api.manager.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.cache.EpPolicyTemplateCacheKey;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyWriter;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.RendererPolicyUtil;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.Match;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.Services;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.ServiceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.config.service.chain.grouping.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.In;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.Out;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.CONSUMER;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.PROVIDER;

public class PolicyManagerImpl implements PolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyMapper.class);
    private final DataBroker dataBroker;
    private DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> epPolicyCache;
    private final PolicyMapper mapper;
    private final String policyMapName = "service-chains";
    private Map<DataBroker, PolicyWriter> perDeviceWriterCache = new HashMap<>();

    public enum ActionCase { ALLOW, CHAIN }


    public PolicyManagerImpl(final DataBroker dataBroker,
                             final DSTreeBasedCache<EndpointPolicyTemplateBySgt, EpPolicyTemplateCacheKey, Sgt> epPolicyCache) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.epPolicyCache = Preconditions.checkNotNull(epPolicyCache);
        mapper = new PolicyMapper(dataBroker);
    }

    @Override
    public ListenableFuture<Boolean> syncPolicy(final Configuration dataBefore, final Configuration dataAfter) {
        // CREATE
        for (RendererEndpoint rendererEndpoint : dataAfter.getRendererEndpoints().getRendererEndpoint()) {
            // Get mountpoint
            if (dataAfter.getEndpoints() == null) {
                continue;
            }
            DataBroker mountpoint = getAbsoluteLocationMountpoint(rendererEndpoint, dataAfter.getEndpoints()
                    .getAddressEndpointWithLocation());
            if (mountpoint == null) {
                continue;
            }
            // Initialize appropriate writer
            PolicyWriter policyWriter;
            if (perDeviceWriterCache.containsKey(mountpoint)) {
                policyWriter = perDeviceWriterCache.get(mountpoint);
            } else {
                policyWriter = new PolicyWriter(mountpoint);
                perDeviceWriterCache.put(mountpoint, policyWriter);
            }
            // Peer Endpoint
            for (PeerEndpointWithPolicy peerEndpoint : rendererEndpoint.getPeerEndpointWithPolicy()) {
                // Sgt Tags
                final Sgt sourceSgt = findSgtTag(rendererEndpoint, dataAfter.getEndpoints()
                        .getAddressEndpointWithLocation());
                final Sgt destinationSgt = findSgtTag(peerEndpoint, dataAfter.getEndpoints()
                        .getAddressEndpointWithLocation());
                if (sourceSgt == null || destinationSgt == null) {
                    continue;
                }
                syncPolicyEntities(sourceSgt, destinationSgt, policyWriter, dataAfter, peerEndpoint);
            }
        }
        // Flush
        perDeviceWriterCache.values().forEach(PolicyWriter::commitToDatastore);
        perDeviceWriterCache.clear();

        return Futures.immediateFuture(true);
    }

    private void syncPolicyEntities(final Sgt sourceSgt, final Sgt destinationSgt, PolicyWriter policyWriter,
                                   final Configuration dataAfter, final PeerEndpointWithPolicy peerEndpoint) {
        // Class map
        String classMapName = generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
        Match match = mapper.createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
        ClassMap classMap = mapper.createClassMap(classMapName, match);
        policyWriter.write(classMap);

        Map<ActionCase, Action> actionMap = getActionInDirection(dataAfter, peerEndpoint);
        if (actionMap == null || actionMap.isEmpty()) {
            return;
        }
        // Policy map entry
        List<Class> policyMapEntries = new ArrayList<>();
        if (actionMap.containsKey(ActionCase.ALLOW)) {
            policyMapEntries = resolveAllowAction();
        }
        if (actionMap.containsKey(ActionCase.CHAIN)) {
            policyMapEntries = resolveChainAction(peerEndpoint, sourceSgt, destinationSgt, actionMap, classMapName);
        }
        policyWriter.write(policyMapEntries);
    }

    private Sgt findSgtTag(final AddressEndpointKey endpointKey,
                            final List<AddressEndpointWithLocation> endpointsWithLocation) {
        if (endpointKey == null || endpointsWithLocation == null) {
            return null;
        }
        AddressEndpointWithLocation endpointWithLocation = RendererPolicyUtil.lookupEndpoint(endpointKey,
                endpointsWithLocation);
        return epPolicyCache.lookupValue(new EpPolicyTemplateCacheKey(endpointWithLocation));
    }

    private List<Class> resolveChainAction(final PeerEndpointWithPolicy peerEndpoint, final Sgt sourceSgt,
                                           final Sgt destinationSgt, final Map<ActionCase, Action> actionMap,
                                           final String classMapName) {
        List<Class> entries = new ArrayList<>();
        final Action action = actionMap.get(ActionCase.CHAIN);
        ServiceFunctionPath servicePath = PolicyManagerUtil.getServicePath(action.getParameterValue());
        if (servicePath == null) {
            return null;
        }
        TenantId tenantId = getTenantId(peerEndpoint);
        if (tenantId == null) {
            return  null;
        }
        RenderedServicePath renderedPath = PolicyManagerUtil.createRenderedPath(servicePath, tenantId);
        entries.add(mapper.createPolicyEntry(classMapName, renderedPath, ActionCase.CHAIN));
        if (servicePath.isSymmetric()) {
            // symmetric path is in opposite direction. Roles of renderer and peer endpoint will invert
            RenderedServicePath symmetricPath = PolicyManagerUtil
                    .createSymmetricRenderedPath(servicePath, renderedPath, tenantId);
            String oppositeClassMapName = generateClassMapName(destinationSgt.getValue(), sourceSgt.getValue());
            entries.add(mapper.createPolicyEntry(oppositeClassMapName, symmetricPath, ActionCase.CHAIN));
        }
        return entries;
    }

    private List<Class> resolveAllowAction() {
        List<Class> entries = new ArrayList<>();
        entries.add(mapper.createPolicyEntry(policyMapName, null, ActionCase.ALLOW));
        return entries;
    }

    private DataBroker getAbsoluteLocationMountpoint(final RendererEndpoint endpoint,
                                                     final List<AddressEndpointWithLocation> endpointsWithLocation) {
        if (endpoint == null || endpointsWithLocation == null) {
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
        InstanceIdentifier mountPointId = location.getExternalNodeMountPoint();
        return NodeManager.getDataBrokerFromCache(mountPointId);
    }

    private String generateClassMapName(Integer sourceTag, Integer destinationTag) {
        return "srcTag" + sourceTag + "_dstTag" + destinationTag;
    }

    private Map<ActionCase, Action> getActionInDirection(Configuration data, PeerEndpointWithPolicy peer) {
        List<ResolvedRule> rulesInDirection = new ArrayList<>();
        // Find all rules in desired direction
        for (RuleGroupWithRendererEndpointParticipation ruleGroupKey :
                peer.getRuleGroupWithRendererEndpointParticipation()) {
            EndpointPolicyParticipation participation = ruleGroupKey.getRendererEndpointParticipation();
            RuleGroup ruleGroup = findRuleGroup(data, ruleGroupKey);
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
                Classifier classifier = resolvedRule.getClassifier().get(0);
                HasDirection.Direction direction = classifier.getDirection();
                if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                        (participation.equals(CONSUMER) && direction.equals(In))) {
                    rulesInDirection.add(resolvedRule);
                }
            }
        }
        if (rulesInDirection.isEmpty()) {
            return null; // TODO define drop?
        }
        // TODO use only first rule with ActionDefinitionID for now
        Map<ActionCase, Action> result = new HashMap<>();
        for (ResolvedRule resolvedRule : rulesInDirection) {
            // TODO only first action used for now
            Action action = resolvedRule.getAction().get(0);
            if (action.getActionDefinitionId() != null) {
                ActionDefinitionId actionDefinitionId = action.getActionDefinitionId();
                if (actionDefinitionId.equals(AllowActionDefinition.ID)) {
                    result.put(ActionCase.ALLOW, action);
                    return result;
                } else if (actionDefinitionId.equals(ChainActionDefinition.ID)) {
                    result.put(ActionCase.CHAIN, action);
                    return result;
                }
            }
        }
        return null;
    }

    private RuleGroup findRuleGroup(final Configuration data,
                                    final RuleGroupWithRendererEndpointParticipation ruleGroupWithParticipation) {
        final TenantId tenantId = ruleGroupWithParticipation.getTenantId();
        final ContractId contractId = ruleGroupWithParticipation.getContractId();
        final SubjectName subjectName = ruleGroupWithParticipation.getSubjectName();
        for (RuleGroup ruleGroup : data.getRuleGroups().getRuleGroup()) {
            if (!ruleGroup.getTenantId().equals(tenantId))
                continue;
            if (!ruleGroup.getContractId().equals(contractId)) {
                continue;
            }
            if (ruleGroup.getSubjectName().equals(subjectName)) {
                return ruleGroup;
            }
        }
        return null;
    }

    private TenantId getTenantId(PeerEndpointWithPolicy peer) {
        for (RuleGroupWithRendererEndpointParticipation ruleGroup :
                peer.getRuleGroupWithRendererEndpointParticipation()) {
            if (ruleGroup.getTenantId() != null) {
                return ruleGroup.getTenantId();
            }
        }
        return null;
    }


    private void resolveFirstSffOnClassifier(final Ipv4Address nodeIpAddress,
                                             final Set<RenderedServicePath> firstHops) {
        // Local forwarder
        LocalBuilder localSffBuilder = new LocalBuilder();
        localSffBuilder.setIp(new IpBuilder().setAddress(nodeIpAddress).build());

        // TODO add sff to writer

        for (RenderedServicePath renderedPath : firstHops) {
            // Remote forwarder
            RenderedServicePathHop firstRenderedPathHop = renderedPath.getRenderedServicePathHop().get(0);
            SffName sffName = firstRenderedPathHop.getServiceFunctionForwarder();

            // Remap sff and its management ip address
            org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder serviceFunctionForwarder =
                    SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            String sffMgmtIpAddress = serviceFunctionForwarder.getIpMgmtAddress().getIpv4Address().getValue();

            ServiceFfNameBuilder remoteSffBuilder = new ServiceFfNameBuilder();
            remoteSffBuilder.setName(sffName.getValue())
                    .setKey(new ServiceFfNameKey(sffName.getValue()))
                    .setIp(new IpBuilder().setAddress(new Ipv4Address(sffMgmtIpAddress)).build());
            // TODO add sff to writer

            // Service chain
            List<Services> services = new ArrayList<>();
            ServiceTypeChoice serviceTypeChoice = sffTypeChoice(sffName.getValue());
            ServicesBuilder servicesBuilder = new ServicesBuilder();
            servicesBuilder.setServiceIndexId(renderedPath.getStartingIndex())
                    .setServiceTypeChoice(serviceTypeChoice);
            List<ServicePath> servicePaths = new ArrayList<>();
            ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
            servicePathBuilder.setKey(new ServicePathKey(renderedPath.getPathId()))
                    .setServicePathId(renderedPath.getPathId())
                    .setConfigServiceChainPathMode(new ConfigServiceChainPathModeBuilder()
                            .setServiceIndex(new ServiceIndexBuilder()
                                    .setServices(services).build()).build());
            servicePaths.add(servicePathBuilder.build());
            ServiceChainBuilder chainBuilder = new ServiceChainBuilder();
            chainBuilder.setServicePath(servicePaths);
            ServiceChain serviceChain = chainBuilder.build();
            // TODO add service-chain to writer
        }
    }

    private ServiceTypeChoice sffTypeChoice(String forwarderName) {
        ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setServiceFunctionForwarder(forwarderName);
        return sffBuilder.build();
    }

    @Override
    public void close() {
        //NOOP
    }
}
