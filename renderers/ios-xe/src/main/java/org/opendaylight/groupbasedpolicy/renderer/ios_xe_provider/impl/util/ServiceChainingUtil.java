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
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.createPolicyMapEntry;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.createSecurityGroupMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.generateClassMapName;
import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil.getTenantId;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.In;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.Out;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.CONSUMER;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.PROVIDER;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NetconfTransactionCreator;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriterUtil;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.RuleName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.unconfigured.rule.groups.unconfigured.rule.group.UnconfiguredResolvedRuleBuilder;
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
     * According to provided action, this method gets service function path and collects all info about participation
     * and orientation of path. According to path symmetricity, participation and direction, one of these cases happens:
     * 1. Path is asymmetric, and it starts in  this classifier (specified by context) - direct chain is created
     * 2. Path is asymmetric, and it starts in classifier on opposite side of the chain - skipped
     * 3. Path is symmetric, and it starts in this classifier - direct chain is created
     * 2. Path is symmetric, and it starts in classifier on opposite side of the chain - reversed path is created
     * <p>
     * Behaviour is correct also in case when "this" and "opposite" classifier is the same
     *
     * @param peerEndpoint   - peer endpoint, used to generate status and access to tenant ID
     * @param sourceSgt      - security group tag of source endpoint
     * @param destinationSgt - security group tag of destination endpoint
     * @param actionMap      - contains all info to evaluate correct chain orientation according to endpoint participation
     * @param context        - contains policy-map location and status info
     * @param dataBroker     - to access odl datastore
     */
    static void newChainAction(final PeerEndpoint peerEndpoint, final Sgt sourceSgt,
                               final Sgt destinationSgt, final Map<PolicyManagerImpl.ActionCase, ActionInDirection> actionMap,
                               final PolicyConfigurationContext context, final DataBroker dataBroker) {
        final ActionInDirection actionInDirection = actionMap.get(ActionCase.CHAIN);
        if (actionInDirection == null) {
            return;
        }
        context.setCurrentUnconfiguredRule(new UnconfiguredResolvedRuleBuilder()
                .setRuleName(new RuleName(actionInDirection.getRuleName())).build());
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
        boolean sfcPartSuccessful = true;
        // Creates direct path in corresponding direction
        if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                (participation.equals(CONSUMER) && direction.equals(In))) {
            final RenderedServicePath renderedServicePath = ServiceChainingUtil.resolveRenderedServicePath(servicePath,
                    tenantId, dataBroker, sourceSgt, destinationSgt, context);
            sfcPartSuccessful = resolveRemoteSfcComponents(renderedServicePath, context);
            // Creates reversed path if symmetric
        } else if (servicePath.isSymmetric()) {
            final RenderedServicePath renderedServicePath =
                    ServiceChainingUtil.resolveReversedRenderedServicePath(servicePath, tenantId, dataBroker, sourceSgt,
                            destinationSgt, context);
            sfcPartSuccessful = resolveRemoteSfcComponents(renderedServicePath, context);
        }
        if (!sfcPartSuccessful) {
            final String info = String.format("failed during sfc-part execution (sourceSgt=%s, destinationSgt=%s)",
                    sourceSgt, destinationSgt);
            context.appendUnconfiguredRendererEP(StatusUtil.assembleNotConfigurableRendererEPForPeer(context,
                    peerEndpoint, info));
        }
    }

    /**
     * According to service function path and direction, creates appropriate rendered service path name {@link RspName}
     * and starts appropriate method which removes policy for resolved endpoint pair
     *
     * @param peerEndpoint   - contains info about tenant ID
     * @param sourceSgt      - security group tag of source endpoint
     * @param destinationSgt - security group tag of destination endpoint
     * @param actionMap      - contains all info to evaluate correct chain orientation according to endpoint participation
     * @param context        - contains policy-map location and status info
     */
    static void resolveRemovedChainAction(final PeerEndpoint peerEndpoint, final Sgt sourceSgt, final Sgt destinationSgt,
                                          final Map<ActionCase, ActionInDirection> actionMap, final PolicyConfigurationContext context) {
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
        if ((participation.equals(PROVIDER) && direction.equals(Out)) ||
                (participation.equals(CONSUMER) && direction.equals(In))) {
            final RspName rspName = generateRspName(servicePath, tenantId);
            resolveRemovedRenderedServicePath(rspName, sourceSgt, destinationSgt, context);

        } else if (servicePath.isSymmetric()) {
            final RspName rspName = generateReversedRspName(servicePath, tenantId);
            resolveRemovedRenderedServicePath(rspName, sourceSgt, destinationSgt, context);
        }
    }

    /**
     * Service-path (netconf) is created on every netconf device, which contains service function belonging to specific
     * chain. Classifier has to be able to reach first service function forwarder in order to send packet to chain. If
     * first service function forwarder is present on the same node as classifier, service-path entry should be already
     * present (created by IOS-XE renderer in SFC) also with appropriate remote SFF if necessary. If first SFF is on
     * different node (remote classifier), classifier has to create it's own service-path entry with remote SFF.
     *
     * @param renderedServicePath - path classifier has to reach
     * @param context             - contains policy-map location and status info
     * @return true if everything went good, false otherwise
     */
    public static boolean resolveRemoteSfcComponents(final RenderedServicePath renderedServicePath,
                                                      final PolicyConfigurationContext context) {
        final PolicyManagerImpl.PolicyMapLocation location = context.getPolicyMapLocation();
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
                        if (!addressValue.equals(location.getManagementIpAddress())) {
                            // Remote forwarder
                            final ServiceFfNameBuilder remoteSffBuilder = new ServiceFfNameBuilder();
                            remoteSffBuilder.setName(sffName.getValue())
                                    .setKey(new ServiceFfNameKey(sffName.getValue()))
                                    .setIp(new IpBuilder().setAddress(new Ipv4Address(remoteForwarderStringIp)).build());
                            boolean rsResult = PolicyWriterUtil.writeRemote(remoteSffBuilder.build(), location);
                            context.setFutureResult(Futures.immediateCheckedFuture(rsResult));
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
                            boolean scResult = PolicyWriterUtil.writeServicePath(serviceChain, location);
                            context.setFutureResult(Futures.immediateCheckedFuture(scResult));
                        }
                        return true;
                    }).orElseGet(createNegativePathWithLogSupplier(sffName.getValue(),
                            (value) -> LOG.error("Cannot create remote forwarder, SFF {} does not contain management ip address",
                                    value))
                    );
        }
        return false;
    }

    /**
     * Investigates provided parameter values and derives service chain name. This name is used to find service function
     * path
     *
     * @param params - list of parameters
     * @return - service function path if found, null if provided parameters does not correspond with any chain or there
     * is no service function path defined by that chain
     */
    @Nullable
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
     * DataBroker)}. If this operation is successful, class-map {@link ClassMap} and entry in policy-map {@link Class}
     * is written
     *
     * @param sfp            - path used to create RSP
     * @param tenantId       - used to generate RSP name according to GBP standards
     * @param dataBroker     - data provider to access odl controller
     * @param sourceSgt      - source security group tag
     * @param destinationSgt - destination security group tag
     * @param context        - contains policy-map location and status info
     * @return read/created RSP
     */
    static RenderedServicePath resolveRenderedServicePath(final ServiceFunctionPath sfp, final TenantId tenantId,
                                                                  final DataBroker dataBroker, final Sgt sourceSgt, final Sgt destinationSgt,
                                                                  final PolicyConfigurationContext context) {
        // Get rendered service path
        final RspName rspName = generateRspName(sfp, tenantId);
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.info("Rendered service path with name {} not found, creating a new one ..", rspName.getValue());
            final CreateRenderedPathInput input = new CreateRenderedPathInputBuilder()
                    .setParentServiceFunctionPath(sfp.getName().getValue())
                    .setName(rspName.getValue())
                    .setSymmetric(sfp.isSymmetric())
                    .build();
            renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfp, input);
            LOG.info("Rendered service path {} created", rspName.getValue());
            checkRspManagerStatus(rspName, dataBroker);
        }
        // Create class-map and policy-map entry
        final String classMapName = generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
        final Match match = createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
        final ClassMap classMap = createClassMap(classMapName, match);
        final Class policyMapEntry = createPolicyMapEntry(classMapName, renderedServicePath, ActionCase.CHAIN);
        boolean cmResult = PolicyWriterUtil.writeClassMap(classMap, context.getPolicyMapLocation());
        context.setFutureResult(Futures.immediateCheckedFuture(cmResult));
        boolean pmeResult = PolicyWriterUtil.writePolicyMapEntry(policyMapEntry, context.getPolicyMapLocation());
        context.setFutureResult(Futures.immediateCheckedFuture(pmeResult));
        return renderedServicePath;
    }

    /**
     * Creates reversed {@link RenderedServicePath} if not exist. To be successful, direct path has to exist.
     * If created, ios-xe renderer in SFC is invoked, so this method has to wait till SFC part is done to prevent
     * transaction collisions. If this operation is successful, class-map {@link ClassMap} and entry in policy-map
     * {@link Class} is written
     *
     * @param sfp            - path used to create RSP
     * @param tenantId       - used to generate RSP name according to GBP standards
     * @param dataBroker     - data provider to access odl controller
     * @param sourceSgt      - source security group tag
     * @param destinationSgt - destination security group tag
     * @param context        - contains policy-map location and status info
     * @return read/created RSP
     */
    public static RenderedServicePath resolveReversedRenderedServicePath(final ServiceFunctionPath sfp, final TenantId tenantId,
                                                                          final DataBroker dataBroker, final Sgt sourceSgt,
                                                                          final Sgt destinationSgt, final PolicyConfigurationContext context) {
        // Get rendered service path
        final RspName rspName = generateRspName(sfp, tenantId);
        RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.info("Rendered service path with name {} not found, creating a new one ..", rspName.getValue());
            final CreateRenderedPathInput input = new CreateRenderedPathInputBuilder()
                    .setParentServiceFunctionPath(sfp.getName().getValue())
                    .setName(rspName.getValue())
                    .setSymmetric(sfp.isSymmetric())
                    .build();
            renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfp, input);
            LOG.info("Rendered service path {} created", rspName.getValue());
            checkRspManagerStatus(rspName, dataBroker);
        }
        // Get reversed rendered service path
        final RspName reversedRspName = generateReversedRspName(sfp, tenantId);
        RenderedServicePath reversedRenderedPath = SfcProviderRenderedPathAPI.readRenderedServicePath(reversedRspName);
        if (reversedRenderedPath == null) {
            LOG.info("Reversed rendered service path with name {} not found, creating a new one ..", reversedRspName.getValue());
            reversedRenderedPath = SfcProviderRenderedPathAPI.createReverseRenderedServicePathEntry(renderedServicePath);
            LOG.info("Rendered service path {} created", reversedRspName.getValue());
            checkRspManagerStatus(reversedRspName, dataBroker);
        }
        // Create class-map and policy-map entry
        final String classMapName = generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
        final Match match = createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
        final ClassMap classMap = createClassMap(classMapName, match);
        final Class policyMapEntry = createPolicyMapEntry(classMapName, renderedServicePath, ActionCase.CHAIN);
        boolean cmResult = PolicyWriterUtil.writeClassMap(classMap, context.getPolicyMapLocation());
        context.setFutureResult(Futures.immediateCheckedFuture(cmResult));
        boolean pmeResult = PolicyWriterUtil.writePolicyMapEntry(policyMapEntry, context.getPolicyMapLocation());
        context.setFutureResult(Futures.immediateCheckedFuture(pmeResult));
        resolveRemoteSfcComponents(renderedServicePath, context);
        return reversedRenderedPath;
    }

    /**
     * Removes all policy setup created according to rendered service path.
     *
     * @param rspName        - rendered service path name
     * @param sourceSgt      - source security group tag
     * @param destinationSgt - destination security group tag
     * @param context        - context with policy-map location
     */
    private static void resolveRemovedRenderedServicePath(final RspName rspName, final Sgt sourceSgt, final Sgt destinationSgt,
                                                          final PolicyConfigurationContext context) {
        final String classMapName = PolicyManagerUtil.generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
        final ClassMap classMap = PolicyManagerUtil.createClassMap(classMapName, null);
        final Class policyMapEntry = PolicyManagerUtil.createPolicyMapEntry(classMapName, null, PolicyManagerImpl.ActionCase.CHAIN);
        PolicyWriterUtil.removePolicyMapEntry(policyMapEntry, context.getPolicyMapLocation());
        PolicyWriterUtil.removeClassMap(classMap, context.getPolicyMapLocation());
        final RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        final ServiceFunctionForwarder firstHopSff = getFirstHopSff(renderedServicePath);
        if (firstHopSff != null && firstHopSff.getIpMgmtAddress() != null &&
                firstHopSff.getIpMgmtAddress().getIpv4Address() != null) {
            final String sffMgmtIpValue = firstHopSff.getIpMgmtAddress().getIpv4Address().getValue();
            if (!sffMgmtIpValue.equals(context.getPolicyMapLocation().getManagementIpAddress())) {
                // Remove service chain and remote forwarder
                final ServiceChain serviceChain = createServiceChain(renderedServicePath);
                final ServiceFfName remoteForwarder = createRemoteForwarder(firstHopSff);
                PolicyWriterUtil.removeServicePath(serviceChain, context.getPolicyMapLocation());
                PolicyWriterUtil.removeRemote(remoteForwarder, context.getPolicyMapLocation());
            }
        }
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
     *
     * @param value - set actual timeout value
     */
    @VisibleForTesting
    public static void setTimeout(long value) {
        timeout = value;
    }
}