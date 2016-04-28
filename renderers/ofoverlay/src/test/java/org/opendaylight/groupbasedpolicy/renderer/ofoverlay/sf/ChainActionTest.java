/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.dto.EgKey;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer.NetworkElements;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ChainAction.class)
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
    private EndpointManager endpointManager;
    private EgKey egKey;
    private TenantId tenantId = new TenantId("e09a2308-6ffa-40af-92a2-69f54b2cf3e4");

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        chainAction = new ChainAction();

        sfcPath = new ServiceFunctionPathBuilder().setName(new SfpName("sfcPathName")).setSymmetric(true).build();
        rsp = new RenderedServicePathBuilder().setName(new RspName("rspName")).build();
        endpoint = new EndpointBuilder().setL2Context(new L2BridgeDomainId("L2context"))
            .setMacAddress(new MacAddress("01:23:45:67:89:ab"))
            .setTenant(tenantId)
            .build();

        ctx = mock(OfContext.class);
        dataBroker = mock(DataBroker.class);
        when(ctx.getDataBroker()).thenReturn(dataBroker);
        rTx = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(rTx);
        checkedFuture = mock(CheckedFuture.class);
        when(rTx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        optRsp = mock(Optional.class);
        when(checkedFuture.checkedGet()).thenReturn(optRsp);
        when(optRsp.isPresent()).thenReturn(true).thenReturn(false);
        when(optRsp.get()).thenReturn(rsp);

        netElements = mock(NetworkElements.class);
        nodeId = mock(NodeId.class);
        when(netElements.getDstNodeId()).thenReturn(nodeId);
        endpointFwdCtxOrdinals = mock(EndpointFwdCtxOrdinals.class);
        when(netElements.getSrcEpOrdinals()).thenReturn(endpointFwdCtxOrdinals);
        when(netElements.getDstEpOrdinals()).thenReturn(endpointFwdCtxOrdinals);
        when(netElements.getSrcEp()).thenReturn(endpoint);

        endpointManager = mock(EndpointManager.class);
        when(ctx.getEndpointManager()).thenReturn(endpointManager);
        egKey = mock(EgKey.class);
        Set<EgKey> keysForEndpoint = new HashSet<>();
        keysForEndpoint.add(egKey);
        when(endpointManager.getEgKeysForEndpoint(any(Endpoint.class))).thenReturn(keysForEndpoint);
    }

    @Test
    public void testGetters() {
        assertEquals(ChainActionDefinition.ID, chainAction.getId());
        assertEquals(ChainActionDefinition.DEFINITION, chainAction.getActionDef());
    }

    @Test
    public void testUpdateAction_DirectionOut_OpendaylightSfcNull() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Collections.singletonList(actionBuilder);
        Map<String, Object> params = new HashMap<>();
        String chainName = "chainName";
        params.put(ChainActionDefinition.SFC_CHAIN_NAME, chainName);
        Integer order = 0;
        OfWriter ofWriter = mock(OfWriter.class);

        PowerMockito.mockStatic(ChainAction.class);
        SfcName sfcName = new SfcName(chainName);
        when(ChainAction.getSfcPath(eq(sfcName))).thenReturn(sfcPath);
        chainAction.setResolvedSymmetricChains(Collections.singletonList(chainName));

        List<ActionBuilder> result =
                chainAction.updateAction(actions, params, order, netElements, ofWriter, ctx, Direction.Out);
        Assert.assertNull(result);
    }

    @Test
    public void testUpdateAction_ParamsNull() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Collections.singletonList(actionBuilder);
        Integer order = 0;
        OfWriter ofWriter = mock(OfWriter.class);

        List<ActionBuilder> result =
                chainAction.updateAction(actions, null, order, netElements, ofWriter, ctx, Direction.In);
        Assert.assertNull(result);
    }

    @Test
    public void testUpdateAction_ChainNameNull() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Collections.singletonList(actionBuilder);
        Map<String, Object> params = new HashMap<>();
        params.put(ChainActionDefinition.SFC_CHAIN_NAME, null);
        Integer order = 0;
        NetworkElements netElements = mock(NetworkElements.class);
        OfWriter ofWriter = mock(OfWriter.class);

        List<ActionBuilder> result =
                chainAction.updateAction(actions, params, order, netElements, ofWriter, ctx, Direction.In);

        Assert.assertNull(result);
    }

    @Test
    public void testUpdateAction() {
        ActionBuilder actionBuilder = mock(ActionBuilder.class);
        List<ActionBuilder> actions = Collections.singletonList(actionBuilder);
        Map<String, Object> params = new HashMap<>();
        String chainName = "chainName";
        params.put(ChainActionDefinition.SFC_CHAIN_NAME, chainName);
        Integer order = 0;
        OfWriter ofWriter = mock(OfWriter.class);

        ServiceFunctionPath sfcPathNameNull = new ServiceFunctionPathBuilder().setName(null).setSymmetric(true).build();

        PowerMockito.mockStatic(ChainAction.class);
        when(ChainAction.getSfcPath(new SfcName(chainName))).thenReturn(sfcPathNameNull);

        List<ActionBuilder> result =
                chainAction.updateAction(actions, params, order, netElements, ofWriter, ctx, Direction.Out);
        Assert.assertNull(result);
    }

    @Test
    public void testIsValid_ParameterValueNull() {
        ActionInstance actionInstance = mock(ActionInstance.class);
        Assert.assertFalse(chainAction.validate(actionInstance).isValid());
    }
}
