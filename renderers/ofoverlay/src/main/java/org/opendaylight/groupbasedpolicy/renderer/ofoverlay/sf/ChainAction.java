package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadNshc1RegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadNshc2RegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIPv4Action;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIdAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxSetNsiAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxSetNspAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.outputAction;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.IsRequired;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.Parameter.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definition.ParameterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;

/**
 * Chain action for the OpenFlow Overlay renderer
 * TODO: separate the generic definition from the concrete
 * implementation for the OpenFlow Ovelray renderer
 */
public class ChainAction extends Action {

    private static final Logger LOG = LoggerFactory.getLogger(ChainAction.class);

    public static final ActionDefinitionId ID = new ActionDefinitionId("3d886be7-059f-4c4f-bbef-0356bea40933");

    public static final Integer CHAIN_CONDITION_GROUP = 0xfffffe;

    protected static final String TYPE = "type";

    // the chain action
    public static final String SFC_CHAIN_ACTION = "chain";
    // the parameter used for storing the chain name
    public static final String SFC_CHAIN_NAME = "sfc-chain-name";

    protected static final ActionDefinition DEF = new ActionDefinitionBuilder().setId(ID)
        .setName(new ActionName(SFC_CHAIN_ACTION))
        .setDescription(new Description("Send the traffic through a Service Function Chain"))
        .setParameter(
                (ImmutableList.of(new ParameterBuilder().setName(new ParameterName(SFC_CHAIN_NAME))
                    .setDescription(new Description("The named chain to match against"))
                    .setIsRequired(IsRequired.Required)
                    .setType(Type.String)
                    .build())))
        .build();

    @Override
    public ActionDefinitionId getId() {
        return ID;
    }

    @Override
    public ActionDefinition getActionDef() {
        return DEF;
    }

    @Override
    public List<ActionBuilder> updateAction(List<ActionBuilder> actions,
                                            Map<String, Object> params,
                                            Integer order,
                                            NetworkElements netElements) {
        /*
         * Get the named chain
         */
        ServiceFunctionPath sfcPath = null;
        String chainName = null;
        if (params != null) {
            LOG.debug("Searching for named chain");
            for (String name : params.keySet()) {
                if (name instanceof String) {
                    if (name.equals(SFC_CHAIN_NAME)) {
                        chainName = (String) params.get(name);
                        if (chainName == null) {
                            LOG.error("ChainAction: Chain name was null");
                            return null;
                        }
                        sfcPath = getSfcPath(chainName);
                    }
                }
            }
        } else {
            LOG.error("ChainAction: Parameters null for chain action");
            return null;
        }

        if (sfcPath == null) {
            LOG.error("ChainAction: SFC Path null for chain {}", chainName);
            return null;
        }
        String rspName = sfcPath.getName() + "-gbp-rsp";
        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(rspName);
        if (rspFirstHop == null) {
            LOG.info("ChainAction: Could not find RSP {} for Chain {}", rspName, chainName);

            CreateRenderedPathInput rspInput = new CreateRenderedPathInputBuilder().setParentServiceFunctionPath(
                    sfcPath.getName())
                .setName(rspName)
                .setSymmetric(Boolean.FALSE)
                .build();
            RenderedServicePath renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(
                    sfcPath, rspInput);
            if (renderedServicePath == null) {
                LOG.error("Could not find or create RSP for chain {}", chainName);
                return null;
            }
            rspFirstHop=SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(renderedServicePath.getName());
        }

        IpAddress sfcTunIpDst = rspFirstHop.getIp();
        sfcTunIpDst.getIpv4Address();
        if (sfcTunIpDst == null || sfcTunIpDst.getIpv4Address() == null || sfcTunIpDst.getIpv6Address() != null) {
            LOG.error("Invalid IP Tunnel destination for SFC RSP First Hop {}", rspName);
            return null;
        }
        PortNumber sfcTunUdpPort = rspFirstHop.getPort();
        if (sfcTunUdpPort == null) {
            LOG.error("Invalid UDP Port Number for SFC RSP {}", rspName);
            return null;
        }
        Long sfcNsp = rspFirstHop.getPathId();
        if (sfcNsp == null) {
            LOG.error("Invalid NSP for SFC RSP {}", rspName);
            return null;
        }
        Short sfcNsi = rspFirstHop.getStartingIndex();
        if (sfcNsi == null) {
            LOG.error("Invalid NSI for SFC RSP {}", rspName);
            return null;
        }

        NodeConnectorId tunOpenFlowPort = SwitchManager.getTunnelPort(netElements.getNodeId(), TunnelTypeVxlanGpe.class);

        /*
         * Setting NSH Network Context Headers for post-SFC encapsulation
         * VXLAN header encap:
         * - TunnelDestination IP: NSH C1
         * - Tunnel ID (VNID) NSH C2
         */
        long postSfcTunnelDst = 999L;
        IpAddress tunnelDest;

        if (netElements.getDst().getAugmentation(OfOverlayContext.class).getNodeId().equals(netElements.getNodeId())) {
            // Return destination is here
            tunnelDest=SwitchManager.getTunnelIP(netElements.getNodeId(), TunnelTypeVxlanGpe.class);
        } else {
            tunnelDest=SwitchManager.getTunnelIP(netElements.getDst().getAugmentation(OfOverlayContext.class).getNodeId(), TunnelTypeVxlanGpe.class);
        }
        postSfcTunnelDst = (InetAddresses.coerceToInteger(InetAddresses.forString(tunnelDest.getIpv4Address().getValue()))) & 0xFFFFFFFFL;

        // TunnelDestination after Chain
        actions = addActionBuilder(actions, nxLoadNshc1RegAction(postSfcTunnelDst), order++);
        // VNID after Chain
        actions = addActionBuilder(actions, nxLoadNshc2RegAction((long) netElements.getSrcOrds().getTunnelId()), order++);

        /*
         * Set the tunnel destination IP
         */
        if (sfcTunIpDst.getIpv4Address() != null) {
            String nextHop = sfcTunIpDst.getIpv4Address().getValue();
            actions = addActionBuilder(actions, nxLoadTunIPv4Action(nextHop, false), order);
        } else if (sfcTunIpDst.getIpv6Address() != null) {
            LOG.error("IPv6 tunnel destination {} not supported", sfcTunIpDst.getIpv6Address().getValue());
            return actions;
        } else {
            // this shouldn't happen
            LOG.error("Tunnel IP is invalid");
            return actions;
        }

        /*
         * Put TunID - with NSH we don't really care about this.
         */
        actions = addActionBuilder(actions,
                nxLoadTunIdAction(BigInteger.valueOf(netElements.getSrcOrds().getTunnelId()), false), order);

        /*
         * Set the NSH header fields, based on RSP
         */
         actions = addActionBuilder(actions,nxSetNsiAction(sfcNsi),order);
         actions = addActionBuilder(actions,nxSetNspAction(sfcNsp),order);
         /*
         * Set up the actions to send to the destination port
         */
         actions = addActionBuilder(actions,outputAction(tunOpenFlowPort), order);

        return actions;
    }

    private List<ActionBuilder> addActionBuilder(List<ActionBuilder> actions,
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action, Integer order) {
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(action);
        ab.setOrder(order);
        actions.add(ab);
        return actions;
    }

    @Override
    public boolean isValid(ActionInstance actionInstance) {
        return validChain(actionInstance.getParameterValue());
    }

    private boolean validChain(List<ParameterValue> paramValue) {
        ParameterValue pv = getChainNameParameter(paramValue);
        if (pv == null) {
            return false;
        }
        LOG.trace("Invoking RPC for chain {}", pv.getStringValue());
        ServiceFunctionChain chain = SfcProviderServiceChainAPI.readServiceFunctionChain(pv.getStringValue());
        return chain != null;
    }

    public ServiceFunctionPath getSfcPath(String chainName) {

        ServiceFunctionPaths paths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        for (ServiceFunctionPath path : paths.getServiceFunctionPath()) {
            if (path.getServiceChainName().equals(chainName)) {
                return path;
            }
        }
        return null;
    }

    private ParameterValue getChainNameParameter(List<ParameterValue> paramValueList) {
        if (paramValueList == null)
            return null;
        for (ParameterValue pv : paramValueList) {
            if (pv.getName().getValue().equals(SFC_CHAIN_NAME)) {
                return pv;
            }
        }
        return null;
    }

}
