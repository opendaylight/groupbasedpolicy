/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.ChainActionFlows.createChainTunnelFlows;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxSetNsiAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxSetNspAction;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sf.actions.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.PolicyPair;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcIidFactory;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader.SfcNshHeaderBuilder;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceChainAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.path.first.hop.info.RenderedServicePathFirstHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.action.definition.SupportedParameterValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.supported.action.definition.SupportedParameterValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Chain action for the OpenFlow Overlay renderer
 * TODO: separate the generic definition from the concrete
 * implementation for the OpenFlow Overlay renderer
 */
public class ChainAction extends Action {

    private static final Logger LOG = LoggerFactory.getLogger(ChainAction.class);

    @Override
    public ActionDefinitionId getId() {
        return ChainActionDefinition.ID;
    }

    @Override
    public ActionDefinition getActionDef() {
        return ChainActionDefinition.DEFINITION;
    }

    @Override
    public List<ActionBuilder> updateAction(List<ActionBuilder> actions, Map<String, Object> params, Integer order,
            NetworkElements netElements, PolicyPair policyPair, OfWriter ofWriter, OfContext ctx, Direction direction) {
        /*
         * Get the named chain
         */
        String chainName = null;
        if (params != null) {
            LOG.debug("updateAction: Searching for named chain");
            for (String name : params.keySet()) {
                if (name instanceof String) {
                    if (name.equals(ChainActionDefinition.SFC_CHAIN_NAME)) {
                        chainName = (String) params.get(name);
                        if (chainName == null) {
                            LOG.error("updateAction: Chain name was null");
                            return null;
                        }
                    }
                }
            }
        } else {
            LOG.error("updateAction: Parameters null for chain action");
            return null;
        }

        if (chainName == null) {
            LOG.error("updateAction: Chain name was null");
            return null;
        }

        /*
         * If path is symmetrical then there are two RSPs.
         * if srcEp is in consumer EPG use "rspName"
         * else srcEp is in provider EPG, "rspName-Reverse".
         */
        ServiceFunctionPath sfcPath = getSfcPath(new SfcName(chainName));
        if (sfcPath == null || sfcPath.getName() == null) {
            LOG.error("updateAction: SFC Path was invalid. Either null or name was null.", sfcPath);
            return null;
        }
        // Find existing RSP based on following naming convention, else create it.
        RspName rspName = new RspName(sfcPath.getName() + "-gbp-rsp");
        ReadOnlyTransaction rTx = ctx.getDataBroker().newReadOnlyTransaction();
        RenderedServicePath renderedServicePath;
        RenderedServicePath rsp = getRspByName(rspName, rTx);
        if (rsp == null) {
            renderedServicePath = createRsp(sfcPath, rspName);
            if (renderedServicePath != null) {
                LOG.info("updateAction: Could not find RSP {} for Chain {}, created.", rspName, chainName);
            } else {
                LOG.error("updateAction: Could not create RSP {} for Chain {}", rspName, chainName);
                return null;
            }
        } else {
            renderedServicePath = rsp;
        }

        try {
            if (sfcPath.isSymmetric() && direction.equals(Direction.Out)){
                rspName = new RspName(rspName.getValue() + "-Reverse");
                rsp = getRspByName(rspName, rTx);
                if (rsp == null) {
                    LOG.info("updateAction: Could not find Reverse RSP {} for Chain {}", rspName, chainName);
                    renderedServicePath = createSymmetricRsp(renderedServicePath);
                    if (renderedServicePath == null) {
                        LOG.error("updateAction: Could not create RSP {} for Chain {}", rspName, chainName);
                        return null;
                    }
                } else {
                    renderedServicePath = rsp;
                }
            }
        } catch (Exception e) {
            LOG.error("updateAction: Attemping to determine if srcEp {} was consumer.", netElements.getSrcEp().getKey(), e);
            return null;
        }

        RenderedServicePathFirstHop rspFirstHop = SfcProviderRenderedPathAPI.readRenderedServicePathFirstHop(rspName);
        if (!isValidRspFirstHop(rspFirstHop)) {
            // Errors logged in method.
            return null;
        }

        NodeId tunnelDestNodeId=netElements.getDstNodeId();

        Long returnVnid = (long) netElements.getSrcEpOrds().getTunnelId();

        IpAddress tunnelDest = ctx.getSwitchManager().getTunnelIP(tunnelDestNodeId, TunnelTypeVxlanGpe.class);
        if (tunnelDest == null || tunnelDest.getIpv4Address() == null) {
            LOG.error("updateAction: Invalid tunnelDest for NodeId: {}", tunnelDestNodeId);
            return null;
        }

        RenderedServicePathHop firstRspHop = renderedServicePath.getRenderedServicePathHop().get(0);
        RenderedServicePathHop lastRspHop = Iterables.getLast(renderedServicePath.getRenderedServicePathHop());
        SfcNshHeader sfcNshHeader = new SfcNshHeaderBuilder().setNshTunIpDst(rspFirstHop.getIp().getIpv4Address())
            .setNshTunUdpPort(rspFirstHop.getPort())
            .setNshNsiToChain(firstRspHop.getServiceIndex())
            .setNshNspToChain(renderedServicePath.getPathId())
            .setNshNsiFromChain((short) (lastRspHop.getServiceIndex().intValue() - 1))
            .setNshNspFromChain(renderedServicePath.getPathId())
            .setNshMetaC1(SfcNshHeader.convertIpAddressToLong(tunnelDest.getIpv4Address()))
            .setNshMetaC2(returnVnid)
            .build();

        // Cannot set all actions here. Some actions are destination specific, and we don't know
        // a destination is to be
        // chained until we reach this point. Need to write match/action in External Table for
        // chained packets.
        actions = addActionBuilder(actions, nxSetNsiAction(sfcNshHeader.getNshNsiToChain()), order);
        actions = addActionBuilder(actions, nxSetNspAction(sfcNshHeader.getNshNspToChain()), order);
        createChainTunnelFlows(sfcNshHeader, netElements, ofWriter, ctx);
        return actions;
    }

    private RenderedServicePath createRsp(ServiceFunctionPath sfcPath, RspName rspName) {
        CreateRenderedPathInput rspInput = new CreateRenderedPathInputBuilder().setParentServiceFunctionPath(
                sfcPath.getName().getValue())
            .setName(rspName.getValue())
            .setSymmetric(sfcPath.isSymmetric())
            .build();
         return SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfcPath, rspInput);
    }

    private RenderedServicePath createSymmetricRsp(RenderedServicePath rsp) {
        if (rsp == null) {
            return null;
        }
        return SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(rsp);
    }

    private boolean isValidRspFirstHop(RenderedServicePathFirstHop rspFirstHop) {
        boolean valid = true;
        if (rspFirstHop == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop is null.");
            return false;
        }
        if (rspFirstHop.getIp() == null || rspFirstHop.getIp().getIpv4Address() == null
                || rspFirstHop.getIp().getIpv6Address() != null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has invalid IP address.");
            valid = false;
        }
        if (rspFirstHop.getPort() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no IP port .");
            valid = false;
        }
        if (rspFirstHop.getPathId() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no Path Id (NSP).");
            valid = false;
        }
        if (rspFirstHop.getStartingIndex() == null) {
            LOG.error("isValidRspFirstHop: rspFirstHop has no Starting Index (NSI)");
            valid = false;
        }
        return valid;
    }

    private RenderedServicePath getRspByName(RspName rspName, ReadOnlyTransaction rTx) {
        Optional<RenderedServicePath> optRsp = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                SfcIidFactory.rspIid(rspName), rTx);
        if (optRsp.isPresent()) {
            return optRsp.get();
        }
        return null;
    }

    @Override
    public boolean isValid(ActionInstance actionInstance) {
        return isValidGbpChain(actionInstance.getParameterValue());
    }

    private boolean isValidGbpChain(List<ParameterValue> paramValue) {
        ParameterValue pv = getChainNameParameter(paramValue);
        if (pv == null) {
            return false;
        }
        SfcName sfcName = new SfcName(pv.getStringValue());
        LOG.trace("isValidGbpChain: Invoking RPC for chain {}", pv.getStringValue());
        ServiceFunctionChain chain = SfcProviderServiceChainAPI.readServiceFunctionChain(sfcName);
        return (chain != null);
    }

    public ServiceFunctionPath getSfcPath(SfcName chainName) {
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
            if (pv.getName().getValue().equals(ChainActionDefinition.SFC_CHAIN_NAME)) {
                return pv;
            }
        }
        return null;
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
    public List<SupportedParameterValues> getSupportedParameterValues() {
        // supported parameter does not contain parameter type - it means all strings are supported
        return ImmutableList.<SupportedParameterValues>of(new SupportedParameterValuesBuilder()
            .setParameterName(new ParameterName(ChainActionDefinition.SFC_CHAIN_NAME)).build());
    }

}
