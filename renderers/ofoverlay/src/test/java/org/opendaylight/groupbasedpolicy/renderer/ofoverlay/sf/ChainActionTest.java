/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.PolicyEnforcer.PolicyPair;
import org.opendaylight.groupbasedpolicy.resolver.EgKey;
import org.opendaylight.groupbasedpolicy.sf.actions.ChainActionDefinition;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class ChainActionTest {

    private ChainAction chainAction;

    private ServiceFunctionPath sfcPath;
    private OfContext ctx;
    private DataBroker dataBroker;
    private ReadOnlyTransaction rTx;
    private CheckedFuture<Optional<RenderedServicePath>, ReadFailedException> checkedFuture;
    private Optional<RenderedServicePath> optRsp;
    private RenderedServicePath rsp;
    private NetworkElements netElements;
    private NodeId nodeId;
    private EndpointFwdCtxOrdinals endpointFwdCtxOrdinals;
    private Endpoint endpoint;
    private PolicyPair policyPair;
    private EndpointManager endpointManager;
    private EgKey egKey;
    private TenantId tenant = new TenantId("e09a2308-6ffa-40af-92a2-69f54b2cf3e4");

    @SuppressWarnings("unchecked")
    @Before
    public void initialise() throws Exception {
        chainAction = spy(new ChainAction());

        sfcPath = mock(ServiceFunctionPath.class);
        when(sfcPath.getName()).thenReturn(new SfpName("sfcPathName"));
        when(sfcPath.isSymmetric()).thenReturn(true);

        ctx = mock(OfContext.class);
        dataBroker = mock(DataBroker.class);
        when(ctx.getDataBroker()).thenReturn(dataBroker);
        rTx = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        checkedFuture = mock(CheckedFuture.class);
        when(rTx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        optRsp = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optRsp);
        rsp = mock(RenderedServicePath.class);
        when(optRsp.isPresent()).thenReturn(true).thenReturn(false);
        when(optRsp.get()).thenReturn(rsp);

        netElements = mock(NetworkElements.class);
        nodeId = mock(NodeId.class);
        when(netElements.getDstNodeId()).thenReturn(nodeId);
        endpointFwdCtxOrdinals = mock(EndpointFwdCtxOrdinals.class);
        when(netElements.getSrcEpOrds()).thenReturn(endpointFwdCtxOrdinals);
        endpoint = mock(Endpoint.class);
        when(netElements.getSrcEp()).thenReturn(endpoint);
        when(netElements.getSrcEp().getTenant()).thenReturn(tenant);
        policyPair = mock(PolicyPair.class);
        when(policyPair.getConsumerEpgId()).thenReturn(Integer.valueOf(5));

        endpointManager = mock(EndpointManager.class);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        egKey = mock(EgKey.class);
        Set<EgKey> keysForEndpoint = new HashSet<EgKey>();
        keysForEndpoint.add(egKey);
        when(endpointManager.getEgKeysForEndpoint(any(Endpoint.class))).thenReturn(keysForEndpoint);
    }

    @Test
    public void staticTest() {
        Assert.assertNotNull(chainAction.getId());
        Assert.assertNotNull(chainAction.getActionDef());
    }

    @Test
    public void updateActionTestDirectionOutOpendaylightSfcNull() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Arrays.asList(actionBuilder);
        Map<String, Object> params = new HashMap<String, Object>();
        String chainName = "chainName";
        params.put(ChainActionDefinition.SFC_CHAIN_NAME, chainName);
        Integer order = Integer.valueOf(0);
        OfWriter ofWriter = mock(OfWriter.class);

        doReturn(sfcPath).when(chainAction).getSfcPath(new SfcName(chainName));

        List<ActionBuilder> result =
                chainAction.updateAction(actions, params, order, netElements, policyPair, ofWriter, ctx, Direction.Out);
        Assert.assertNull(result);
    }

    @Test
    public void updateActionTestParamsNull() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Arrays.asList(actionBuilder);
        Integer order = Integer.valueOf(0);
        OfWriter ofWriter = mock(OfWriter.class);

        List<ActionBuilder> result =
                chainAction.updateAction(actions, null, order, netElements, policyPair, ofWriter, ctx, Direction.In);
        Assert.assertNull(result);
    }

    @Test
    public void updateActionTestChainNameNull() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Arrays.asList(actionBuilder);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(ChainActionDefinition.SFC_CHAIN_NAME, null);
        Integer order = Integer.valueOf(0);
        NetworkElements netElements = mock(NetworkElements.class);
        PolicyPair policyPair = mock(PolicyPair.class);
        OfWriter ofWriter = mock(OfWriter.class);

        chainAction.updateAction(actions, params, order, netElements, policyPair, ofWriter, ctx, Direction.In);
    }

    @Test
    public void updateActionTest() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Arrays.asList(actionBuilder);
        Map<String, Object> params = new HashMap<String, Object>();
        String chainName = "chainName";
        params.put(ChainActionDefinition.SFC_CHAIN_NAME, chainName);
        Integer order = Integer.valueOf(0);
        OfWriter ofWriter = mock(OfWriter.class);

        doReturn(sfcPath).when(chainAction).getSfcPath(new SfcName(chainName));
        when(sfcPath.getName()).thenReturn(null);

        List<ActionBuilder> result =
                chainAction.updateAction(actions, params, order, netElements, policyPair, ofWriter, ctx, Direction.Out);
        Assert.assertNull(result);
    }

    @Test
    public void isValidTestParameterValueNull() {
        ActionInstance actionInstance = mock(ActionInstance.class);
        Assert.assertFalse(chainAction.isValid(actionInstance));
    }
}
