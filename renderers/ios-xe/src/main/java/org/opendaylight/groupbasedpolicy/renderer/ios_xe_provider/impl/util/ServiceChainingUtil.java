/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyConfigurationContext;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.NetconfTransactionCreator;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.groupbasedpolicy.util.IetfModelCodec;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.Match;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.ConfigServiceChainPathModeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.ServiceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.Services;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.ServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.ServiceTypeChoice;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.path.config.service.chain.path.mode.service.index.services.service.type.choice.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServiceChainingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceChainingUtil.class);
    private static final String RSP_SUFFIX = "-gbp-rsp";
    private static final String RSP_REVERSED_SUFFIX = "-gbp-rsp-Reverse";

    static ServiceFunctionPath getServicePath(final List<ParameterValue> params) {
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
        final ServiceFunctionPath serviceFunctionPath = findServiceFunctionPath(new SfcName(chainName));
        if (serviceFunctionPath == null) {
            LOG.error("Service function path not found for name {}", chainName);
            return null;
        }
        return serviceFunctionPath;
    }

    static void resolveNewChainAction(final PeerEndpoint peerEndpoint, final Sgt sourceSgt,
                                      final Sgt destinationSgt, final Map<PolicyManagerImpl.ActionCase, Action> actionMap,
                                      final PolicyConfigurationContext context, final DataBroker dataBroker) {
        final List<Class> policyMapEntries = new ArrayList<>();
        final Action action = actionMap.get(PolicyManagerImpl.ActionCase.CHAIN);
        final ServiceFunctionPath servicePath = ServiceChainingUtil.getServicePath(action.getParameterValue());
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
        final RenderedServicePath directPath = ServiceChainingUtil.createRenderedPath(servicePath, tenantId, dataBroker);
        // Rsp found, create class-map and policy-map entry
        final String classMapName = PolicyManagerUtil.generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
        final Match match = PolicyManagerUtil.createSecurityGroupMatch(sourceSgt.getValue(), destinationSgt.getValue());
        final ClassMap classMap = PolicyManagerUtil.createClassMap(classMapName, match);
        policyMapEntries.add(PolicyManagerUtil.createPolicyEntry(classMapName, directPath, PolicyManagerImpl.ActionCase.CHAIN));
        RenderedServicePath reversedPath = null;
        if (servicePath.isSymmetric()) {
            // symmetric path is in opposite direction. Roles of renderer and peer endpoint will invert
            reversedPath = ServiceChainingUtil.createSymmetricRenderedPath(servicePath, directPath, tenantId, dataBroker);
            // Reversed Rsp found, create class-map and policy-map entry in opposite direction
            final String oppositeClassMapName = PolicyManagerUtil.generateClassMapName(destinationSgt.getValue(), sourceSgt.getValue());
            final Match oppositeMatch = PolicyManagerUtil.createSecurityGroupMatch(destinationSgt.getValue(), sourceSgt.getValue());
            final ClassMap oppositeClassMap = PolicyManagerUtil.createClassMap(oppositeClassMapName, oppositeMatch);
            policyMapEntries.add(PolicyManagerUtil.createPolicyEntry(oppositeClassMapName, reversedPath, PolicyManagerImpl.ActionCase.CHAIN));
            context.getPolicyWriter().cache(oppositeClassMap);
        }
        // Create appropriate service path && remote forwarder
        final boolean sfcPartSuccessful = setSfcPart(servicePath, directPath, reversedPath, context.getPolicyWriter());
        if (!sfcPartSuccessful) {
            //TODO: extract resolved-rule name
            final String info = String.format("failed during sfc-part execution (sourceSgt=%s, destinationSgt=%s)",
                    sourceSgt, destinationSgt);
            //context.appendUnconfiguredRendererEP(StatusUtil.assembleNotConfigurableRendererEPForPeerAndAction(context, peerEndpoint, info));
            return;
        }
        context.getPolicyWriter().cache(classMap);
        context.getPolicyWriter().cache(policyMapEntries);
    }

    static void removeChainAction(final PeerEndpoint peerEndpoint, final Sgt sourceSgt, final Sgt destinationSgt,
                                  final Map<PolicyManagerImpl.ActionCase, Action> actionMap, PolicyWriter policyWriter) {
        final Action action = actionMap.get(PolicyManagerImpl.ActionCase.CHAIN);
        final ServiceFunctionPath servicePath = ServiceChainingUtil.getServicePath(action.getParameterValue());
        if (servicePath == null || servicePath.getName() == null) {
            return;
        }
        final TenantId tenantId = PolicyManagerUtil.getTenantId(peerEndpoint);
        if (tenantId == null) {
            return;
        }
        // Cache class-maps, appropriate policy-map entries and service-chains
        final List<Class> policyMapEntries = new ArrayList<>();
        final String classMapName = PolicyManagerUtil.generateClassMapName(sourceSgt.getValue(), destinationSgt.getValue());
        final ClassMap classMap = PolicyManagerUtil.createClassMap(classMapName, null);
        final RspName rspName = generateRspName(servicePath, tenantId);
        final ServiceChain serviceChain = findServiceChainToRsp(rspName);
        policyMapEntries.add(PolicyManagerUtil.createPolicyEntry(classMapName, null, PolicyManagerImpl.ActionCase.CHAIN));
        policyWriter.cache(classMap);
        policyWriter.cache(serviceChain);
        if (servicePath.isSymmetric()) {
            final String oppositeClassMapName = PolicyManagerUtil.generateClassMapName(destinationSgt.getValue(), sourceSgt.getValue());
            final ClassMap oppositeClassMap = PolicyManagerUtil.createClassMap(oppositeClassMapName, null);
            final RspName reversedRspName = generateReversedRspName(servicePath, tenantId);
            final ServiceChain reversedServiceChain = findServiceChainToRsp(reversedRspName);
            policyMapEntries.add(PolicyManagerUtil.createPolicyEntry(oppositeClassMapName, null, PolicyManagerImpl.ActionCase.CHAIN));
            policyWriter.cache(oppositeClassMap);
            policyWriter.cache(reversedServiceChain);
        }
        policyWriter.cache(policyMapEntries);
        // TODO remove other sfc stuff - forwarders, etc.
    }

    private static ServiceChain findServiceChainToRsp(final RspName rspName) {
        // Do not actually remove rsp from DS, could be used by someone else
        final RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath == null) {
            LOG.debug("Rendered service path not found, if there is service-path created according to that rsp, " +
                    "it cannot be removed. Rendered path name: {} ", rspName.getValue());
            return null;
        }
        // Construct service chain with key
        final Long pathId = renderedServicePath.getPathId();
        final ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
        final ServiceChainBuilder serviceChainBuilder = new ServiceChainBuilder();
        servicePathBuilder.setServicePathId(pathId)
                .setKey(new ServicePathKey(pathId));
        serviceChainBuilder.setServicePath(Collections.singletonList(servicePathBuilder.build()));
        return serviceChainBuilder.build();
    }

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
        checkSfcRspStatus(rspName, dataBroker);
        return renderedServicePath;
    }

    static RenderedServicePath createSymmetricRenderedPath(final ServiceFunctionPath sfp, final RenderedServicePath rsp,
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
        checkSfcRspStatus(rspName, dataBroker);
        return reversedRenderedPath;
    }

    /**
     * Method checks up, if some {@link ServicePath} is present on device.
     *
     * @param mountpoint used to access specific device
     * @return true if service chain does not exist, is null or does not contain any service path. False otherwise
     */
    public static boolean checkServicePathPresence(DataBroker mountpoint) {
        InstanceIdentifier<ServiceChain> serviceChainIid = InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class).build();
        java.util.Optional<ReadOnlyTransaction> optionalTransaction =
                NetconfTransactionCreator.netconfReadOnlyTransaction(mountpoint);
        if (!optionalTransaction.isPresent()) {
            LOG.warn("Failed to create transaction, mountpoint: {}", mountpoint);
            return false;
        }
        ReadOnlyTransaction transaction = optionalTransaction.get();
        CheckedFuture<Optional<ServiceChain>, ReadFailedException> submitFuture = transaction.read(LogicalDatastoreType.CONFIGURATION,
                serviceChainIid);
        try {
            Optional<ServiceChain> optionalServiceChain = submitFuture.checkedGet();
            if (optionalServiceChain.isPresent()) {
                ServiceChain chain = optionalServiceChain.get();
                return chain == null || chain.getServicePath() == null || chain.getServicePath().isEmpty();
            } else {
                return true;
            }
        } catch (ReadFailedException e) {
            LOG.warn("Read transaction failed to {} ", e);
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
        return false;
    }

    static ServiceFunctionPath findServiceFunctionPath(final SfcName chainName) {
        final ServiceFunctionPaths allPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        for (ServiceFunctionPath serviceFunctionPath : allPaths.getServiceFunctionPath()) {
            if (serviceFunctionPath.getServiceChainName().equals(chainName)) {
                return serviceFunctionPath;
            }
        }
        return null;
    }

    private static RspName generateRspName(final ServiceFunctionPath serviceFunctionPath, final TenantId tenantId) {
        return new RspName(serviceFunctionPath.getName().getValue() + tenantId.getValue() + RSP_SUFFIX);
    }

    private static RspName generateReversedRspName(final ServiceFunctionPath serviceFunctionPath, final TenantId tenantId) {
        return new RspName(serviceFunctionPath.getName().getValue() + tenantId.getValue() + RSP_REVERSED_SUFFIX);
    }

    private static <T> Supplier<Boolean> createNegativePathWithLogSupplier(final T value, final Consumer<T> logCommand) {
        return () -> {
            // fireLog
            logCommand.accept(value);
            return false;
        };
    }

    static boolean setSfcPart(final ServiceFunctionPath serviceFunctionPath, final RenderedServicePath renderedServicePath,
                              final RenderedServicePath reversedRenderedServicePath, PolicyWriter policyWriter) {
        boolean outcome = true;
        // Direct path
        final java.util.Optional<RenderedServicePath> renderedServicePathSafe = java.util.Optional.ofNullable(renderedServicePath);
        if (renderedServicePathSafe.isPresent()) {
            if (renderedServicePath.getRenderedServicePathHop() != null
                    && !renderedServicePath.getRenderedServicePathHop().isEmpty()) {
                if (!resolveRenderedServicePath(renderedServicePath, policyWriter)) {
                    outcome = false;
                }
            } else {
                LOG.warn("Rendered service path {} does not contain any hop",
                        renderedServicePathSafe.map(RenderedServicePath::getName).map(RspName::getValue).orElse("n/a"));
                outcome = false;
            }
        } else {
            LOG.warn("Rendered service path is null");
            outcome = false;
        }
        if (serviceFunctionPath.isSymmetric()) {
            // Reversed path
            final java.util.Optional<RenderedServicePath> reversedRenderedServicePathSafe = java.util.Optional.ofNullable(reversedRenderedServicePath);
            if (reversedRenderedServicePathSafe.isPresent()) {
                if (reversedRenderedServicePath.getRenderedServicePathHop() != null
                        && !reversedRenderedServicePath.getRenderedServicePathHop().isEmpty()) {
                    if (!resolveRenderedServicePath(reversedRenderedServicePath, policyWriter)) {
                        outcome = false;
                    }
                } else {
                    LOG.warn("Rendered service path {} does not contain any hop",
                            reversedRenderedServicePathSafe.map(RenderedServicePath::getName).map(RspName::getValue).orElse("n/a"));
                    outcome = false;
                }
            } else {
                LOG.warn("Reversed rendered service path is null");
                outcome = false;
            }
        }
        return outcome;
    }

    private static boolean resolveRenderedServicePath(final RenderedServicePath renderedServicePath, PolicyWriter policyWriter) {
        final RenderedServicePathHop firstHop = renderedServicePath.getRenderedServicePathHop().get(0);
        if (firstHop == null) {
            return false;
        }
        final SffName sffName = firstHop.getServiceFunctionForwarder();

        // Forwarders
        //
        // If classifier node is also forwarder, first entry in service path has to point to first service function
        // (Local case)
        //
        // If first hop Sff is on different node, first service path entry has to point to that specific service
        // forwarder (Remote case)

        final java.util.Optional<ServiceFunctionForwarder> serviceFunctionForwarder = java.util.Optional.ofNullable(
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName));
        if (!serviceFunctionForwarder.isPresent()) {
            LOG.warn("Service function forwarder {} does not exist", sffName.getValue());
            return false;
        }
        final ServiceFunctionForwarder forwarder = serviceFunctionForwarder.get();
        if (forwarder.getSffDataPlaneLocator() == null || forwarder.getSffDataPlaneLocator().isEmpty()) {
            LOG.warn("Service function forwarder {} does not contain data plane locator", sffName.getValue());
            return false;
        }
        // TODO only first dpl resolved
        final SffDataPlaneLocator sffDataPlaneLocator = forwarder.getSffDataPlaneLocator().get(0);
        final DataPlaneLocator dataPlaneLocator = sffDataPlaneLocator.getDataPlaneLocator();
        final LocatorType locatorType = dataPlaneLocator.getLocatorType();
        if (locatorType != null && locatorType instanceof Ip) {
            final IpAddress remoteForwarderIpAddress = IetfModelCodec.ipAddress2010(((Ip) locatorType).getIp());
            if (remoteForwarderIpAddress == null || remoteForwarderIpAddress.getIpv4Address() == null) {
                LOG.warn("Service function forwarder {} data plane locator does not contain ip address", sffName.getValue());
                return false;
            }
            final String remoteForwarderStringIp = remoteForwarderIpAddress.getIpv4Address().getValue();
            return serviceFunctionForwarder.map(sff -> java.util.Optional.ofNullable(sff.getIpMgmtAddress())
                    .map(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress::getIpv4Address)
                    .map(Ipv4Address::getValue)
                    .map(addressValue -> {
                        // Set up choice. If remote, this choice is overwritten
                        final ServiceTypeChoice serviceTypeChoice;
                        if (!addressValue.equals(policyWriter.getManagementIpAddress())) {
                            final ServiceFfNameBuilder remoteSffBuilder = new ServiceFfNameBuilder();
                            remoteSffBuilder.setName(sffName.getValue())
                                    .setKey(new ServiceFfNameKey(sffName.getValue()))
                                    .setIp(new IpBuilder().setAddress(new Ipv4Address(remoteForwarderStringIp)).build());
                            policyWriter.cache(remoteSffBuilder.build());
                            serviceTypeChoice = forwarderTypeChoice(sffName.getValue());
                        } else {
                            serviceTypeChoice = functionTypeChoice(firstHop.getServiceFunctionName().getValue());
                        }

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

                        return true;
                    }).orElseGet(createNegativePathWithLogSupplier(sffName.getValue(),
                            (value) -> LOG.error("Cannot create remote forwarder, SFF {} does not contain management ip address",
                                    value))
                    )
            ).orElseGet(createNegativePathWithLogSupplier(sffName.getValue(),
                    (value) -> LOG.error("Sff with name {} does not exist", value))
            );
        }
        return false;
    }

    static ServiceTypeChoice forwarderTypeChoice(final String forwarderName) {
        final ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setServiceFunctionForwarder(forwarderName);
        return sffBuilder.build();
    }

    static ServiceTypeChoice functionTypeChoice(final String functionName) {
        final ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setServiceFunction(functionName);
        return sfBuilder.build();
    }

    private static void checkSfcRspStatus(final RspName rspName, final DataBroker dataBroker) {
        /** TODO A better way to do this is to register listener and wait for notification than using hardcoded timeout
         *  with Thread.sleep(). Example in class BridgeDomainManagerImpl
         */
        ConfiguredRenderedPath renderedPath = null;
        LOG.info("Waiting for SFC to configure path {} ...", rspName.getValue());

        byte attempt = 0;
        do {
            attempt++;
            // Wait
            try {
                Thread.sleep(5000L);
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
                LOG.info("RSP {} configured by SFC", rspName.getValue());
                try {
                    Thread.sleep(5000); // Just for sure, maybe will be safe to remove this
                } catch (InterruptedException e) {
                    LOG.error("Thread interrupted while waiting ... {} ", e);
                }
                return;
            }
        }
        while (attempt <= 6);
        LOG.warn("Maximum number of attempts reached");
    }
}
