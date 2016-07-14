/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.ActionInDirection;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.createClassMap;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.createPolicyEntry;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.createSecurityGroupMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.generateClassMapName;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.getTenantId;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.In;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.Out;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.CONSUMER;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.PROVIDER;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NetconfTransactionCreator;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.RendererPathStates;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.RendererPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.RendererPathStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.ConfiguredRenderedPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.rsp.manager.rev160421.renderer.path.states.renderer.path.state.configured.rendered.paths.ConfiguredRenderedPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RendererName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarder.base.sff.data.plane.locator.DataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.Match;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.Services;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.ServiceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceChainingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceChainingUtil.class);
    private static final String RSP_SUFFIX = "-gbp-rsp";
    private static final String RSP_REVERSED_SUFFIX = "-gbp-rsp-Reverse";
    private static long timeout = 5000L;

    /**
     * According to input, creates class-maps ({@link ClassMap}) and entries into policy-map ({@link Class}). These
     * components are created when particular RSP is build by SFC ios-xe renderer. If so, method continues by resolving
     * first RSP hop in {@link this#resolveRemoteSfcComponents(RenderedServicePath, PolicyWriter)}.
     *
     * @param peerEndpoint   - peer endpoint, used to generate status and access to tenant ID
     * @param sourceSgt      - security group tag of source endpoint
     * @param destinationSgt - security group tag of destination endpoint
     * @param actionMap      - contains all info to evaluate correct chain orientation according to endpoint participation
     * @param context        - contains policy writer
     * @param dataBroker     - to access odl datastore
     */
    static void resolveNewChainAction(final PeerEndpoint peerEndpoint, final Sgt sourceSgt,
                                      final Sgt destinationSgt, final Map<PolicyManagerImpl.ActionCase, ActionInDirection> actionMap,
                                      final PolicyConfigurationContext context, final DataBroker dataBroker) {
        final ActionInDirection actionInDirection = actionMap.get(ActionCase.CHAIN);
        if (actionInDirection == null) {
            return;
        }
        // Rule action + orientation
        final Action action = actionInDirection.getAction();
        final EndpointPolicyParticipation participation = actionInDirection.getParticipation();
        final Direction direction = actionInDirection.getDirection();
        // Get service function path
        final ServiceFunctionPath servicePath = ServiceChainingUtil.findServicePathFromParameterValues(action.getParameterValue());
        if (servicePath == null || servicePath.getName() == null) {
            final String info = String.format("service-path not found (sourceSgt=%s, destinationSgt=%s)",
                    sourceSgt, destinationSgt);
            context.appendUnconfiguredRendererEP(StatusUtil.assembleNotConfigurableRendererEPForPeer(context, peerEndpoint, info));
            return;
        }
        final TenantId tenantId = PolicyManagerUtil.getTenantId(peerEndpoint);
        if (tenantId == null) {
            final String info = String.format("tenant-id not found (sourceSgt=%s, destinationSgt=%s)",
                    sourceSgt, destinationSgt);
            context.appendUnconfiguredRendererEP(StatusUtil.assembleNotConfigurableRendererEPForPeer(context, peerEndpoint, info));
            return;
        }
        RenderedServicePath renderedServicePath;
        boolean sfcPartSuccessful = true;
        // Symmetric chain - create direct or reversed rendered service path in corresponding direction
        if (servicePath.isSymmetric()) {
            if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                    (participation.equals(CONSUMER) && direction.equals(In))) {
                renderedServicePath = ServiceChainingUtil.createRenderedPath(servicePath, tenantId, dataBroker);
                // Rsp found, create class-map and policy-map entry
                final String classMapName = generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
                final Match match = createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
                final ClassMap classMap = createClassMap(classMapName, match);
                final Class policyMapEntry = createPolicyEntry(classMapName, renderedServicePath, ActionCase.CHAIN);
                context.getPolicyWriter().cache(classMap);
                context.getPolicyWriter().cache(policyMapEntry);
                sfcPartSuccessful = resolveRemoteSfcComponents(renderedServicePath, context.getPolicyWriter());
            } else {
                // Direct path required if reversed has to be created
                final RenderedServicePath directPath = ServiceChainingUtil.createRenderedPath(servicePath, tenantId, dataBroker);
                // Create reversed path
                renderedServicePath =
                        ServiceChainingUtil.createReversedRenderedPath(servicePath, directPath, tenantId, dataBroker);
                // Rsp found, create class-map and policy-map entry
                final String classMapName = generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
                final Match match = createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
                final ClassMap classMap = createClassMap(classMapName, match);
                final Class policyMapEntry = createPolicyEntry(classMapName, renderedServicePath, ActionCase.CHAIN);
                context.getPolicyWriter().cache(classMap);
                context.getPolicyWriter().cache(policyMapEntry);
                sfcPartSuccessful = resolveRemoteSfcComponents(renderedServicePath, context.getPolicyWriter());
            }
        }
        // Asymmetric chain - create direct path if corresponding direction or skip
        else if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                (participation.equals(CONSUMER) && direction.equals(In))) {
            renderedServicePath = ServiceChainingUtil.createRenderedPath(servicePath, tenantId, dataBroker);
            // Rsp found, create class-map and policy-map entry
            final String classMapName = generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
            final Match match = createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
            final ClassMap classMap = createClassMap(classMapName, match);
            final Class policyMapEntry = createPolicyEntry(classMapName, renderedServicePath, ActionCase.CHAIN);
            context.getPolicyWriter().cache(classMap);
            context.getPolicyWriter().cache(policyMapEntry);
            sfcPartSuccessful = resolveRemoteSfcComponents(renderedServicePath, context.getPolicyWriter());
        }
        // Create appropriate service path && remote forwarder
        if (!sfcPartSuccessful) {
            //TODO: extract resolved-rule name
            final String info = String.format("failed during sfc-part execution (sourceSgt=%s, destinationSgt=%s)",
                    sourceSgt, destinationSgt);
            //context.appendUnconfiguredRendererEP(StatusUtil.assembleNotConfigurableRendererEPForPeerAndAction(context, peerEndpoint, info));
        }
    }

    /**
     * Removes class-map and policy-map entry in policy between endpoint pair. If necessary, method deletes remote SFC
     * components.
     *
     * @param peerEndpoint    - contains info about tenant ID
     * @param sourceSgt       - security group tag of source endpoint
     * @param destinationSgt- security group tag of destination endpoint
     * @param actionMap       - contains all info to evaluate correct chain orientation according to endpoint participation
     * @param policyWriter    - used to access ios-xe capable device
     */
    static void resolveRemovedChainAction(final PeerEndpoint peerEndpoint, final Sgt sourceSgt, final Sgt destinationSgt,
                                          final Map<ActionCase, ActionInDirection> actionMap, PolicyWriter policyWriter) {
        final ActionInDirection actionInDirection = actionMap.get(ActionCase.CHAIN);
        final Action action = actionInDirection.getAction();
        final EndpointPolicyParticipation participation = actionInDirection.getParticipation();
        final Direction direction = actionInDirection.getDirection();
        final ServiceFunctionPath servicePath = ServiceChainingUtil.findServicePathFromParameterValues(action.getParameterValue());
        if (servicePath == null || servicePath.getName() == null) {
            return;
        }
        final TenantId tenantId = getTenantId(peerEndpoint);
        if (tenantId == null) {
            return;
        }
        //Symmetric chain
        if (servicePath.isSymmetric()) {
            if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                    (participation.equals(CONSUMER) && direction.equals(In))) {
                // Cache class-maps, appropriate policy-map entries and service-chains
                final String classMapName = PolicyManagerUtil.generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
                final ClassMap classMap = PolicyManagerUtil.createClassMap(classMapName, null);
                final Class policyMapEntry = PolicyManagerUtil.createPolicyEntry(classMapName, null, PolicyManagerImpl.ActionCase.CHAIN);
                policyWriter.cache(classMap);
                policyWriter.cache(policyMapEntry);

                final RspName rspName = generateRspName(servicePath, tenantId);
                final RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
                final ServiceFunctionForwarder firstHopSff = getFirstHopSff(renderedServicePath);
                if (firstHopSff != null && firstHopSff.getIpMgmtAddress() != null &&
                        firstHopSff.getIpMgmtAddress().getIpv4Address() != null) {
                    final String sffMgmtIpValue = firstHopSff.getIpMgmtAddress().getIpv4Address().getValue();
                    if (!sffMgmtIpValue.equals(policyWriter.getManagementIpAddress())) {
                        // Remove service chain and remote forwarder
                        final ServiceChain serviceChain = createServiceChain(renderedServicePath);
                        final ServiceFfName remoteForwarder = createRemoteForwarder(firstHopSff);
                        policyWriter.cache(serviceChain);
                        policyWriter.cache(remoteForwarder);
                    }
                }
            } else {
                final String oppositeClassMapName = PolicyManagerUtil.generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
                final ClassMap oppositeClassMap = PolicyManagerUtil.createClassMap(oppositeClassMapName, null);
                final Class policyMapEntry = PolicyManagerUtil.createPolicyEntry(oppositeClassMapName, null, PolicyManagerImpl.ActionCase.CHAIN);
                policyWriter.cache(oppositeClassMap);
                policyWriter.cache(policyMapEntry);

                final RspName reversedRspName = generateReversedRspName(servicePath, tenantId);
                final RenderedServicePath reversedRenderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(reversedRspName);
                final ServiceFunctionForwarder reversedFirstHopSff = getFirstHopSff(reversedRenderedServicePath);
                if (reversedFirstHopSff != null && reversedFirstHopSff.getIpMgmtAddress() != null &&
                        reversedFirstHopSff.getIpMgmtAddress().getIpv4Address() != null) {
                    final String reversedSffMgmtIpValue = reversedFirstHopSff.getIpMgmtAddress().getIpv4Address().getValue();
                    if (!reversedSffMgmtIpValue.equals(policyWriter.getManagementIpAddress())) {
                        // Remove service chain and remote forwarder
                        final ServiceChain serviceChain = createServiceChain(reversedRenderedServicePath);
                        final ServiceFfName remoteForwarder = createRemoteForwarder(reversedFirstHopSff);
                        policyWriter.cache(serviceChain);
                        policyWriter.cache(remoteForwarder);
                    }
                }
            }
        } else if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                (participation.equals(CONSUMER) && direction.equals(In))) {
            // Asymmetric chain
            final String classMapName = PolicyManagerUtil.generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
            final ClassMap classMap = PolicyManagerUtil.createClassMap(classMapName, null);
            final Class policyMapEntry = PolicyManagerUtil.createPolicyEntry(classMapName, null, PolicyManagerImpl.ActionCase.CHAIN);
            policyWriter.cache(classMap);
            policyWriter.cache(policyMapEntry);

            final RspName rspName = generateRspName(servicePath, tenantId);
            final RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
            final ServiceFunctionForwarder firstHopSff = getFirstHopSff(renderedServicePath);
            if (firstHopSff != null && firstHopSff.getIpMgmtAddress() != null &&
                    firstHopSff.getIpMgmtAddress().getIpv4Address() != null) {
                final String sffMgmtIpValue = firstHopSff.getIpMgmtAddress().getIpv4Address().getValue();
                if (!sffMgmtIpValue.equals(policyWriter.getManagementIpAddress())) {
                    // Remove service chain and remote forwarder
                    final ServiceChain serviceChain = createServiceChain(renderedServicePath);
                    final ServiceFfName remoteForwarder = createRemoteForwarder(firstHopSff);
                    policyWriter.cache(serviceChain);
                    policyWriter.cache(remoteForwarder);
                }
            }
        }
    }

    /**
     * Service-path (netconf) is created on every netconf device, which contains service function belonging to specific
     * chain. Classifier has to be able to reach first service function forwarder in order to send packet to chain. If
     * first service function forwarder is present on the same node as classifier, service-path entry should be already
     * present (created by IOS-XE renderer in SFC) also with appropriate remote SFF if necessary. If first SFF is on
     * different node, classifier has to create it's own service-path entry with remote SFF.
     *
     * @param renderedServicePath classifier has to reach
     * @param policyWriter        policy entries writer
     * @return true if everything went good, false otherwise
     */
    static boolean resolveRemoteSfcComponents(final RenderedServicePath renderedServicePath, PolicyWriter policyWriter) {
        final ServiceFunctionForwarder forwarder = getFirstHopSff(renderedServicePath);
        if (forwarder == null) {
            return false;
        }
        final SffName sffName = forwarder.getName();
        if (forwarder.getSffDataPlaneLocator() == null || forwarder.getSffDataPlaneLocator().isEmpty()) {
            LOG.warn("Service function forwarder {} does not contain data plane locator", sffName.getValue());
            return false;
        }
        // TODO only first dpl resolved
        final SffDataPlaneLocator sffDataPlaneLocator = forwarder.getSffDataPlaneLocator().get(0);
        final DataPlaneLocator dataPlaneLocator = sffDataPlaneLocator.getDataPlaneLocator();
        final LocatorType locatorType = dataPlaneLocator.getLocatorType();
        if (locatorType != null && locatorType instanceof Ip) {
            final IpAddress remoteForwarderIpAddress = (((Ip) locatorType).getIp());
            if (remoteForwarderIpAddress == null || remoteForwarderIpAddress.getIpv4Address() == null) {
                LOG.warn("Service function forwarder {} data plane locator does not contain ip address", sffName.getValue());
                return false;
            }
            final String remoteForwarderStringIp = remoteForwarderIpAddress.getIpv4Address().getValue();
            final java.util.Optional<IpAddress> optionalIpMgmtAddress = java.util.Optional.ofNullable(forwarder.getIpMgmtAddress());

            return optionalIpMgmtAddress.map(IpAddress::getIpv4Address)
                    .map(Ipv4Address::getValue)
                    .map(addressValue -> {
                        final ServiceTypeChoice serviceTypeChoice;
                        if (!addressValue.equals(policyWriter.getManagementIpAddress())) {
                            // Remote forwarder
                            final ServiceFfNameBuilder remoteSffBuilder = new ServiceFfNameBuilder();
                            remoteSffBuilder.setName(sffName.getValue())
                                    .setKey(new ServiceFfNameKey(sffName.getValue()))
                                    .setIp(new IpBuilder().setAddress(new Ipv4Address(remoteForwarderStringIp)).build());
                            policyWriter.cache(remoteSffBuilder.build());
                            serviceTypeChoice = createForwarderTypeChoice(sffName.getValue());
                            // Service chain
                            final List<Services> services = new ArrayList<>();
                            final ServicesBuilder servicesBuilder = new ServicesBuilder();
                            servicesBuilder.setServiceIndexId(renderedServicePath.getStartingIndex())
                                    .setServiceTypeChoice(serviceTypeChoice);
                            services.add(servicesBuilder.build());
                            final List<ServicePath> servicePaths = new ArrayList<>();
                            final ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
                            servicePathBuilder.setKey(new ServicePathKey(renderedServicePath.getPathId()))
                                    .setServicePathId(renderedServicePath.getPathId())
                                    .setConfigServiceChainPathMode(new ConfigServiceChainPathModeBuilder()
                                            .setServiceIndex(new ServiceIndexBuilder()
                                                    .setServices(services).build()).build());
                            servicePaths.add(servicePathBuilder.build());
                            final ServiceChainBuilder chainBuilder = new ServiceChainBuilder();
                            chainBuilder.setServicePath(servicePaths);
                            final ServiceChain serviceChain = chainBuilder.build();
                            policyWriter.cache(serviceChain);
                        }
                        return true;
                    }).orElseGet(createNegativePathWithLogSupplier(sffName.getValue(),
                            (value) -> LOG.error("Cannot create remote forwarder, SFF {} does not contain management ip address",
                                    value))
                    );
        }
        return false;
    }

    static ServiceFunctionPath findServicePathFromParameterValues(final List<ParameterValue> params) {
        if (params == null || params.isEmpty()) {
            LOG.error("Cannot found service path, parameter value is null");
            return null;
        }
        final Map<String, Object> paramsMap = new HashMap<>();
        for (ParameterValue value : params) {
            if (value.getName() == null)
                continue;
            if (value.getIntValue() != null) {
                paramsMap.put(value.getName().getValue(), value.getIntValue());
            } else if (value.getStringValue() != null) {
                paramsMap.put(value.getName().getValue(), value.getStringValue());
            }
        }
        String chainName = null;
        for (String name : paramsMap.keySet()) {
            if (name.equals(ChainActionDefinition.SFC_CHAIN_NAME)) {
                chainName = (String) paramsMap.get(name);
            }
        }
        if (chainName == null) {
            LOG.error("Cannot found service path, chain name is null");
            return null;
        }
        final ServiceFunctionPath serviceFunctionPath = findServiceFunctionPathFromServiceChainName(new SfcName(chainName));
        if (serviceFunctionPath == null) {
            LOG.error("Service function path not found for name {}", chainName);
            return null;
        }
        return serviceFunctionPath;
    }

    static ServiceFunctionPath findServiceFunctionPathFromServiceChainName(@Nonnull final SfcName chainName) {
        final ServiceFunctionPaths allPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        if (allPaths == null || allPaths.getServiceFunctionPath() == null || allPaths.getServiceFunctionPath().isEmpty()) {
            return null;
        }
        for (ServiceFunctionPath serviceFunctionPath : allPaths.getServiceFunctionPath()) {
            if (chainName.equals(serviceFunctionPath.getServiceChainName())) {
                return serviceFunctionPath;
            }
        }
        return null;
    }

    /**
     * Creates {@link RenderedServicePath} if not exist. If created, ios-xe renderer in SFC is invoked, so this method
     * has to wait till SFC part is done to prevent transaction collisions in {@link this#checkRspManagerStatus(RspName,
     * DataBroker)}
     *
     * @param sfp        - path used to create RSP
     * @param tenantId   - used to generate RSP name according to GBP standards
     * @param dataBroker - data provider to access odl controller
     * @return read/created RSP
     */
    static RenderedServicePath createRenderedPath(final ServiceFunctionPath sfp, final TenantId tenantId,
                                                  final DataBroker dataBroker) {
        RenderedServicePath renderedServicePath;
        // Try to read existing RSP
        final RspName rspName = generateRspName(sfp, tenantId);
        renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath != null) {
            return renderedServicePath;
        }
        LOG.info("Rendered service path with name {} not found, creating a new one ..", rspName.getValue());
        final CreateRenderedPathInput input = new CreateRenderedPathInputBuilder()
                .setParentServiceFunctionPath(sfp.getName().getValue())
                .setName(rspName.getValue())
                .setSymmetric(sfp.isSymmetric())
                .build();
        renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfp, input);
        LOG.info("Rendered service path {} created", rspName.getValue());
        checkRspManagerStatus(rspName, dataBroker);
        return renderedServicePath;
    }

    /**
     * Creates reversed {@link RenderedServicePath} if not exist. To be successful, direct path has to exist.
     * If created, ios-xe renderer in SFC is invoked, so this method has to wait till SFC part is done to prevent
     * transaction collisions in {@link this#checkRspManagerStatus(RspName, DataBroker)}
     *
     * @param sfp        - path used to create RSP
     * @param rsp        - appropriate direct RSP, used when the reversed path is created
     * @param tenantId   - used to generate RSP name according to GBP standards
     * @param dataBroker - data provider to access odl controller
     * @return read/created RSP
     */
    static RenderedServicePath createReversedRenderedPath(final ServiceFunctionPath sfp, final RenderedServicePath rsp,
                                                          final TenantId tenantId, final DataBroker dataBroker) {
        RenderedServicePath reversedRenderedPath;
        // Try to read existing RSP
        final RspName rspName = generateReversedRspName(sfp, tenantId);
        reversedRenderedPath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (reversedRenderedPath != null) {
            return reversedRenderedPath;
        }
        LOG.info("Reversed rendered service path with name {} not found, creating a new one ..", rspName.getValue());
        reversedRenderedPath = SfcProviderRenderedPathAPI.createReverseRenderedServicePathEntry(rsp);
        LOG.info("Rendered service path {} created", rspName.getValue());
        checkRspManagerStatus(rspName, dataBroker);
        return reversedRenderedPath;
    }

    static ServiceFfName createRemoteForwarder(ServiceFunctionForwarder firstHopSff) {
        final ServiceFfNameBuilder serviceFfNameBuilder = new ServiceFfNameBuilder();
        serviceFfNameBuilder.setName(firstHopSff.getName().getValue());
        return serviceFfNameBuilder.build();
    }

    private static ServiceTypeChoice createForwarderTypeChoice(final String forwarderName) {
        final ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setServiceFunctionForwarder(forwarderName);
        return sffBuilder.build();
    }

    /**
     * Creates service-chain with name/key only, using rendered service path id. This object contains no data, it is used
     * to create instance identifier when appropriate service-chain is removed from particular device
     *
     * @param renderedServicePath - it's path id is used as a identifier
     * @return service-chain object with id
     */
    private static ServiceChain createServiceChain(final RenderedServicePath renderedServicePath) {
        final Long pathId = renderedServicePath.getPathId();
        final ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
        final ServiceChainBuilder serviceChainBuilder = new ServiceChainBuilder();
        servicePathBuilder.setServicePathId(pathId)
                .setKey(new ServicePathKey(pathId));
        serviceChainBuilder.setServicePath(Collections.singletonList(servicePathBuilder.build()));
        return serviceChainBuilder.build();
    }

    private static <T> Supplier<Boolean> createNegativePathWithLogSupplier(final T value, final Consumer<T> logCommand) {
        return () -> {
            // fireLog
            logCommand.accept(value);
            return false;
        };
    }

    private static ServiceFunctionForwarder getFirstHopSff(RenderedServicePath renderedServicePath) {
        if (renderedServicePath == null || renderedServicePath.getRenderedServicePathHop() == null ||
                renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            return null;
        }
        final RenderedServicePathHop firstHop = renderedServicePath.getRenderedServicePathHop().get(0);
        final SffName firstHopSff = firstHop.getServiceFunctionForwarder();
        if (firstHopSff == null) {
            return null;
        }
        return SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(firstHopSff);
    }

    private static RspName generateRspName(final ServiceFunctionPath serviceFunctionPath, final TenantId tenantId) {
        return new RspName(serviceFunctionPath.getName().getValue() + "-" + tenantId.getValue() + RSP_SUFFIX);
    }

    private static RspName generateReversedRspName(final ServiceFunctionPath serviceFunctionPath, final TenantId tenantId) {
        return new RspName(serviceFunctionPath.getName().getValue() + "-" + tenantId.getValue() + RSP_REVERSED_SUFFIX);
    }

    private static void checkRspManagerStatus(final RspName rspName, final DataBroker dataBroker) {
        // TODO A better way to do this is to register listener and wait for notification than using hardcoded timeout
        // with Thread.sleep(). Example in class BridgeDomainManagerImpl
        ConfiguredRenderedPath renderedPath = null;
        LOG.debug("Waiting for SFC to configure path {} ...", rspName.getValue());

        byte attempt = 0;
        do {
            attempt++;
            // Wait
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                LOG.error("Thread interrupted while waiting ... {} ", e);
            }
            // Read actual status
            final InstanceIdentifier<ConfiguredRenderedPath> statusIid = InstanceIdentifier.builder(RendererPathStates.class)
                    .child(RendererPathState.class, new RendererPathStateKey(new RendererName("ios-xe-renderer")))
                    .child(ConfiguredRenderedPaths.class)
                    .child(ConfiguredRenderedPath.class, new ConfiguredRenderedPathKey(rspName)).build();
            final java.util.Optional<ReadWriteTransaction> optionalTransaction =
                    NetconfTransactionCreator.netconfReadWriteTransaction(dataBroker);
            if (!optionalTransaction.isPresent()) {
                LOG.warn("Failed to create transaction, mountpoint: {}", dataBroker);
                return;
            }
            ReadWriteTransaction transaction = optionalTransaction.get();
            try {
                final CheckedFuture<Optional<ConfiguredRenderedPath>, ReadFailedException> submitFuture =
                        transaction.read(LogicalDatastoreType.OPERATIONAL, statusIid);
                final Optional<ConfiguredRenderedPath> optionalPath = submitFuture.checkedGet();
                if (optionalPath.isPresent()) {
                    renderedPath = optionalPath.get();
                }
            } catch (ReadFailedException e) {
                LOG.warn("Failed while read rendered path status ... {} ", e.getMessage());
            }
            if (renderedPath == null || renderedPath.getPathStatus() == null ||
                    renderedPath.getPathStatus().equals(ConfiguredRenderedPath.PathStatus.InProgress)) {
                LOG.info("Still waiting for SFC ... ");
            } else if (renderedPath.getPathStatus().equals(ConfiguredRenderedPath.PathStatus.Failure)) {
                LOG.warn("SFC failed to configure rsp");
            } else if (renderedPath.getPathStatus().equals(ConfiguredRenderedPath.PathStatus.Success)) {
                LOG.debug("RSP {} configured by SFC", rspName.getValue());
                try {
                    Thread.sleep(timeout); // Just for sure, maybe will be safe to remove this
                } catch (InterruptedException e) {
                    LOG.error("Thread interrupted while waiting ... {} ", e);
                }
                return;
            }
        }
        while (attempt <= 6);
        LOG.warn("Maximum number of attempts reached");
    }

    /**
     * Only for test purposes
     * @param value - set actual timeout value
     */
    @VisibleForTesting
    public static void setTimeout(long value) {
        timeout = value;
    }
}
