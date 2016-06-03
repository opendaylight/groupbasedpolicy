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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.writer.PolicyWriter;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChainBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.config.service.chain.grouping.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.LocalBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointWithPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceChainingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceChainingUtil.class);

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

    static void resolveChainAction(final PeerEndpointWithPolicy peerEndpoint, final Sgt sourceSgt,
                                   final Sgt destinationSgt, final Map<PolicyManagerImpl.ActionCase, Action> actionMap,
                                   final String classMapName, PolicyWriter policyWriter) {
        final List<Class> entries = new ArrayList<>();
        final Action action = actionMap.get(PolicyManagerImpl.ActionCase.CHAIN);
        final ServiceFunctionPath servicePath = ServiceChainingUtil.getServicePath(action.getParameterValue());
        if (servicePath == null) {
            return;
        }
        final TenantId tenantId = PolicyManagerUtil.getTenantId(peerEndpoint);
        if (tenantId == null) {
            return;
        }
        final RenderedServicePath renderedPath = ServiceChainingUtil.createRenderedPath(servicePath, tenantId);
        // Create appropriate service path && remote forwarder
        setSfcPart(renderedPath, policyWriter);

        entries.add(PolicyManagerUtil.createPolicyEntry(classMapName, renderedPath, PolicyManagerImpl.ActionCase.CHAIN));
        if (servicePath.isSymmetric()) {
            // symmetric path is in opposite direction. Roles of renderer and peer endpoint will invert
            RenderedServicePath symmetricPath = ServiceChainingUtil
                    .createSymmetricRenderedPath(servicePath, renderedPath, tenantId);
            final String oppositeClassMapName = PolicyManagerUtil.generateClassMapName(destinationSgt.getValue(), sourceSgt.getValue());
            entries.add(PolicyManagerUtil.createPolicyEntry(oppositeClassMapName, symmetricPath, PolicyManagerImpl.ActionCase.CHAIN));
        }
        policyWriter.cache(entries);
    }

    static RenderedServicePath createRenderedPath(final ServiceFunctionPath sfp, final TenantId tenantId) {
        RenderedServicePath renderedServicePath;
        // Try to read existing RSP
        final RspName rspName = new RspName(sfp.getName().getValue() + tenantId.getValue() + "-gbp-rsp");
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
        return renderedServicePath;
    }

    static RenderedServicePath createSymmetricRenderedPath(final ServiceFunctionPath sfp, final RenderedServicePath rsp,
                                                           final TenantId tenantId) {
        RenderedServicePath reversedRenderedPath;
        // Try to read existing RSP
        final RspName rspName = new RspName(sfp.getName().getValue() + tenantId.getValue() + "-gbp-rsp-Reverse");
        reversedRenderedPath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (reversedRenderedPath != null) {
            return reversedRenderedPath;
        }
        LOG.info("Reversed rendered service path with name {} not found, creating a new one ..", rspName.getValue());
        reversedRenderedPath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(rsp);
        LOG.info("Rendered service path {} created", rspName.getValue());
        return reversedRenderedPath;
    }

    /**
     * Method checks up, whether a {@link Local} Service Function Forwarder is present on device or not.
     *
     * @param mountpoint used to access specific device
     * @return true if Local Forwarder is present, false otherwise
     */
    private static boolean checkLocalForwarderPresence(DataBroker mountpoint) {
        InstanceIdentifier<Local> localSffIid = InstanceIdentifier.builder(Native.class)
                .child(ServiceChain.class)
                .child(org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionForwarder.class)
                .child(Local.class).build();
        ReadWriteTransaction rwt = mountpoint.newReadWriteTransaction();
        CheckedFuture<Optional<Local>, ReadFailedException> submitFuture = rwt.read(LogicalDatastoreType.CONFIGURATION,
                localSffIid);
        try {
            Optional<Local> optionalLocalSff = submitFuture.checkedGet();
            return optionalLocalSff.isPresent();
        } catch (ReadFailedException e) {
            LOG.warn("Read transaction failed to {} ", e);
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
        return false;
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
        ReadWriteTransaction rwt = mountpoint.newReadWriteTransaction();
        CheckedFuture<Optional<ServiceChain>, ReadFailedException> submitFuture = rwt.read(LogicalDatastoreType.CONFIGURATION,
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

    private static ServiceFunctionPath findServiceFunctionPath(final SfcName chainName) {
        final ServiceFunctionPaths allPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        for (ServiceFunctionPath serviceFunctionPath : allPaths.getServiceFunctionPath()) {
            if (serviceFunctionPath.getServiceChainName().equals(chainName)) {
                return serviceFunctionPath;
            }
        }
        return null;
    }

    private static void setSfcPart(final RenderedServicePath renderedServicePath, PolicyWriter policyWriter) {
        if (renderedServicePath != null && renderedServicePath.getRenderedServicePathHop() != null &&
                !renderedServicePath.getRenderedServicePathHop().isEmpty()) {
            final RenderedServicePathHop firstHop = renderedServicePath.getRenderedServicePathHop().get(0);
            if (firstHop == null) {
                LOG.error("Rendered service path {} does not contain any hop", renderedServicePath.getName().getValue());
                return;
            }
            final SffName sffName = firstHop.getServiceFunctionForwarder();
            final ServiceFunctionForwarder serviceFunctionForwarder = SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(sffName);
            if (serviceFunctionForwarder == null) {
                LOG.error("Sff with name {} does not exist", sffName.getValue());
                return;
            }
            // Forwarders
            //
            // If classifier node is also forwarder, first entry in service path has to point to first service function
            // (Local case)
            //
            // If first hop Sff is on different node, first service path entry has to point to that specific service
            // forwarder (Remote case)

            // Local case (only when does not exist)

            if (!checkLocalForwarderPresence(policyWriter.getCurrentMountpoint())) {
                final LocalBuilder localSffBuilder = new LocalBuilder();
                localSffBuilder.setIp(new IpBuilder().setAddress(new Ipv4Address(policyWriter.getManagementIpAddress()))
                        .build());
                policyWriter.cache(localSffBuilder.build());
            } else {
                LOG.info("Local forwarder for node {} is already created", policyWriter.getCurrentNodeId());
            }
            // Set up choice. If remote, this choice is overwritten
            ServiceTypeChoice serviceTypeChoice = functionTypeChoice(firstHop.getServiceFunctionName().getValue());
            // Remote case
            if (serviceFunctionForwarder.getIpMgmtAddress() == null
                    || serviceFunctionForwarder.getIpMgmtAddress().getIpv4Address() == null) {
                LOG.error("Cannot create remote forwarder, SFF {} does not contain management ip address",
                        sffName.getValue());
                return;
            }
            final String sffMgmtIpAddress = serviceFunctionForwarder.getIpMgmtAddress().getIpv4Address().getValue();
            // If local SFF has the same ip as first hop sff, it's the same SFF; no need to create a remote one
            if (!sffMgmtIpAddress.equals(policyWriter.getManagementIpAddress())) {
                final ServiceFfNameBuilder remoteSffBuilder = new ServiceFfNameBuilder();
                remoteSffBuilder.setName(sffName.getValue())
                        .setKey(new ServiceFfNameKey(sffName.getValue()))
                        .setIp(new IpBuilder().setAddress(new Ipv4Address(sffMgmtIpAddress)).build());
                policyWriter.cache(remoteSffBuilder.build());
                serviceTypeChoice = forwarderTypeChoice(sffName.getValue());
            }

            // Service chain
            final List<Services> services = new ArrayList<>();
            final ServicesBuilder servicesBuilder = new ServicesBuilder();
            servicesBuilder.setServiceIndexId(renderedServicePath.getStartingIndex())
                    .setServiceTypeChoice(serviceTypeChoice);
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
    }

    private static ServiceTypeChoice forwarderTypeChoice(final String forwarderName) {
        final ServiceFunctionForwarderBuilder sffBuilder = new ServiceFunctionForwarderBuilder();
        sffBuilder.setServiceFunctionForwarder(forwarderName);
        return sffBuilder.build();
    }

    private static ServiceTypeChoice functionTypeChoice(final String functionName) {
        final ServiceFunctionBuilder sfBuilder = new ServiceFunctionBuilder();
        sfBuilder.setServiceFunction(functionName);
        return sfBuilder.build();
    }

}
