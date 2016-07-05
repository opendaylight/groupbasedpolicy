package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction.Out;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation.PROVIDER;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.PolicyManagerUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.RendererPolicyUtil;
import org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util.ServiceChainingUtil;
import org.opendaylight.groupbasedpolicy.util.IetfModelCodec;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.EndpointPolicyParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.has.rule.group.with.renderer.endpoint.participation.RuleGroupWithRendererEndpointParticipationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.Configuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RendererForwarding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.RuleGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.rule.groups.RuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.actions.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.ClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.resolved.rules.ResolvedRuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.AddressEndpointWithLocationAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RendererPolicyUtil.class, PolicyManagerUtil.class, SfcProviderServiceForwarderAPI.class})
public class PolicyManagerImplTest {

    private final String address = "address";
    private final String connector = "connector";
    private final SfpName servicePath = new SfpName("service-path");
    private final RspName renderedPath = new RspName("rendered-path");
    private final SffName forwarderName = new SffName("service-forwarder");
    private final ContextId contextId_1 = new ContextId("context-id-1");
    private final ContextId contextId_2 = new ContextId("context-id-2");
    private final ContextId contextId_3 = new ContextId("context-id-3");
    private final ContextId contextId_4 = new ContextId("context-id-4");
    private final ContractId contractId = new ContractId("contract-id");
    private final String ipAddress = "192.168.50.1";
    private final NodeId nodeId = new NodeId("node-id");
    private final SubjectName subjectName = new SubjectName("subject-name");
    private final TenantId tenantId = new TenantId("tenant-id");
    private final TopologyId topologyId = new TopologyId("topology-id");
    private final ActionDefinitionId chainActionDefinitionId = new ActionDefinitionId("Action-Chain");
    private final ActionDefinitionId otherActionDefinitionId = new ActionDefinitionId("Action-Other");
    private final SfName functionName = new SfName("service-function");
    private PolicyManagerImpl policyManager;
    private DataBroker mountpoint;
    private NodeManager nodeManager;
    private WriteTransaction writeTransaction;

    @Before
    public void init() {
        mountpoint = mock(DataBroker.class);
        writeTransaction = mock(WriteTransaction.class);
        nodeManager = mock(NodeManager.class);
        policyManager = new PolicyManagerImpl(mountpoint, nodeManager);
        when(mountpoint.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(Futures.immediateCheckedFuture((Void) null));
    }

    @Test
    public void testSyncPolicy_emptyConfiguration() throws Exception {
        Configuration policyConfiguration = createTestConfiguration(null, null, null, null);
        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_noEndpointsInConfiguration() throws Exception {
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_1, contextId_2, null);
        Configuration policyConfiguration = createTestConfiguration(null, Collections.singletonList(rendererEndpoint),
                null, null);
        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_noMountPoint() throws Exception {
        AddressEndpointWithLocation endpointWithLocation = createAddressEndpointWithLocation(contextId_1, null, false);
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_2, contextId_3, null);
        Configuration policyConfiguration = createTestConfiguration(Collections.singletonList(endpointWithLocation),
                Collections.singletonList(rendererEndpoint), null, null);

        AbsoluteLocation location = createAbsoluteLocationExternal(connector, createMountpointIid());
        AddressEndpointWithLocation lookupEndpoint = createAddressEndpointWithLocation(contextId_4, location, false);
        stub(method(RendererPolicyUtil.class, "lookupEndpoint")).toReturn(lookupEndpoint);
        when(nodeManager.getNodeMountPoint(eq(createMountpointIid()))).thenReturn(null);

        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_nullSgtTags() throws Exception {
        AddressEndpointWithLocation endpointWithLocation = createAddressEndpointWithLocation(contextId_1, null, false);
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_2, contextId_3, null);
        Configuration policyConfiguration = createTestConfiguration(Collections.singletonList(endpointWithLocation),
                Collections.singletonList(rendererEndpoint), null, null);

        AbsoluteLocation location = createAbsoluteLocationExternal(connector, createMountpointIid());
        AddressEndpointWithLocation lookupEndpoint = createAddressEndpointWithLocation(contextId_4, location, false);
        stub(method(RendererPolicyUtil.class, "lookupEndpoint")).toReturn(lookupEndpoint);
        when(nodeManager.getNodeMountPoint(eq(createMountpointIid()))).thenReturn(mountpoint);
        when(nodeManager.getNodeIdByMountpointIid(eq(createMountpointIid()))).thenReturn(nodeId);
        when(nodeManager.getNodeManagementIpByMountPointIid(eq(createMountpointIid()))).thenReturn(ipAddress);

        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_emptyRuleGroup() throws Exception {
        AddressEndpointWithLocation endpointWithLocation = createAddressEndpointWithLocation(contextId_1, null, false);
        RuleGroupWithRendererEndpointParticipation ruleGroupWithParticipation =
                createRuleGroupWithRendererEpParticipation(PROVIDER);
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_2, contextId_3,
                ruleGroupWithParticipation);
        RuleGroup ruleGroup = createRuleGroup(null);
        Configuration policyConfiguration = createTestConfiguration(Collections.singletonList(endpointWithLocation),
                Collections.singletonList(rendererEndpoint), null, ruleGroup);

        AbsoluteLocation location = createAbsoluteLocationExternal(connector, createMountpointIid());
        AddressEndpointWithLocation lookupEndpoint = createAddressEndpointWithLocation(contextId_4, location, true);
        stub(method(RendererPolicyUtil.class, "lookupEndpoint")).toReturn(lookupEndpoint);
        when(nodeManager.getNodeMountPoint(eq(createMountpointIid()))).thenReturn(mountpoint);
        when(nodeManager.getNodeIdByMountpointIid(eq(createMountpointIid()))).thenReturn(nodeId);
        when(nodeManager.getNodeManagementIpByMountPointIid(eq(createMountpointIid()))).thenReturn(ipAddress);

        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_noActionDefinition() throws Exception {
        AddressEndpointWithLocation endpointWithLocation = createAddressEndpointWithLocation(contextId_1, null, false);
        RuleGroupWithRendererEndpointParticipation ruleGroupWithParticipation =
                createRuleGroupWithRendererEpParticipation(PROVIDER);
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_2, contextId_3,
                ruleGroupWithParticipation);

        RuleGroup ruleGroup = createRuleGroup(createRule(Out, null));
        Configuration policyConfiguration = createTestConfiguration(Collections.singletonList(endpointWithLocation),
                Collections.singletonList(rendererEndpoint), null, ruleGroup);

        AbsoluteLocation location = createAbsoluteLocationExternal(connector, createMountpointIid());
        AddressEndpointWithLocation lookupEndpoint = createAddressEndpointWithLocation(contextId_4, location, true);
        stub(method(RendererPolicyUtil.class, "lookupEndpoint")).toReturn(lookupEndpoint);
        when(nodeManager.getNodeMountPoint(eq(createMountpointIid()))).thenReturn(mountpoint);
        when(nodeManager.getNodeIdByMountpointIid(eq(createMountpointIid()))).thenReturn(nodeId);
        when(nodeManager.getNodeManagementIpByMountPointIid(eq(createMountpointIid()))).thenReturn(ipAddress);

        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_otherAction() throws Exception {
        AddressEndpointWithLocation endpointWithLocation = createAddressEndpointWithLocation(contextId_1, null, false);
        RuleGroupWithRendererEndpointParticipation ruleGroupWithParticipation =
                createRuleGroupWithRendererEpParticipation(PROVIDER);
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_2, contextId_3,
                ruleGroupWithParticipation);

        RuleGroup ruleGroup = createRuleGroup(createRule(Out, otherActionDefinitionId));
        Configuration policyConfiguration = createTestConfiguration(Collections.singletonList(endpointWithLocation),
                Collections.singletonList(rendererEndpoint), null, ruleGroup);

        AbsoluteLocation location = createAbsoluteLocationExternal(connector, createMountpointIid());
        AddressEndpointWithLocation lookupEndpoint = createAddressEndpointWithLocation(contextId_4, location, true);
        stub(method(RendererPolicyUtil.class, "lookupEndpoint")).toReturn(lookupEndpoint);
        when(nodeManager.getNodeMountPoint(eq(createMountpointIid()))).thenReturn(mountpoint);
        when(nodeManager.getNodeIdByMountpointIid(eq(createMountpointIid()))).thenReturn(nodeId);
        when(nodeManager.getNodeManagementIpByMountPointIid(eq(createMountpointIid()))).thenReturn(ipAddress);

        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    @Test
    public void testSyncPolicy_asymmetricChain() throws Exception {
        AddressEndpointWithLocation endpointWithLocation = createAddressEndpointWithLocation(contextId_1, null, false);
        RuleGroupWithRendererEndpointParticipation ruleGroupWithParticipation =
                createRuleGroupWithRendererEpParticipation(PROVIDER);
        RendererEndpoint rendererEndpoint = createRendererEndpoint(contextId_2, contextId_3,
                ruleGroupWithParticipation);

        RuleGroup ruleGroup = createRuleGroup(createRule(Out, chainActionDefinitionId));
        Configuration policyConfiguration = createTestConfiguration(Collections.singletonList(endpointWithLocation),
                Collections.singletonList(rendererEndpoint), null, ruleGroup);

        AbsoluteLocation location = createAbsoluteLocationExternal(connector, createMountpointIid());
        AddressEndpointWithLocation lookupEndpoint = createAddressEndpointWithLocation(contextId_4, location, true);
        stub(method(RendererPolicyUtil.class, "lookupEndpoint")).toReturn(lookupEndpoint);
        when(nodeManager.getNodeMountPoint(eq(createMountpointIid()))).thenReturn(mountpoint);
        when(nodeManager.getNodeIdByMountpointIid(eq(createMountpointIid()))).thenReturn(nodeId);
        when(nodeManager.getNodeManagementIpByMountPointIid(eq(createMountpointIid()))).thenReturn(ipAddress);
        ServiceFunctionPath sfp = createServiceFunctionPath();
        stub(method(ServiceChainingUtil.class, "getServicePath")).toReturn(sfp);
        RenderedServicePath rsp = createRenderedServicePath();
        stub(method(ServiceChainingUtil.class, "createRenderedPath")).toReturn(rsp);
        ServiceFunctionForwarder serviceFunctionForwarder = createServiceForwarder();
        stub(method(SfcProviderServiceForwarderAPI.class, "readServiceFunctionForwarder"))
                .toReturn(serviceFunctionForwarder);

        ListenableFuture result = policyManager.syncPolicy(policyConfiguration, null, 0);
        assertTrue((boolean) result.get());
    }

    private ServiceFunctionForwarder createServiceForwarder() {
        ServiceFunctionForwarderBuilder serviceFunctionForwarderBuilder = new ServiceFunctionForwarderBuilder();
        serviceFunctionForwarderBuilder.setName(new SffName(forwarderName))
                .setKey(new ServiceFunctionForwarderKey(new SffName(forwarderName)))
                .setIpMgmtAddress(IetfModelCodec.ipAddress2013(new IpAddress(new Ipv4Address(ipAddress))));
        return serviceFunctionForwarderBuilder.build();
    }

    // Utility methods

    private InstanceIdentifier createMountpointIid() {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(topologyId))
                .child(Node.class, new NodeKey(nodeId)).build();
    }

    private ResolvedRule createRule(HasDirection.Direction direction, ActionDefinitionId actionDefinitionId) {
        ResolvedRuleBuilder resolvedRuleBuilder = new ResolvedRuleBuilder();
        ClassifierBuilder classifierBuilder = new ClassifierBuilder();
        classifierBuilder.setDirection(direction);
        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setActionDefinitionId(actionDefinitionId);
        resolvedRuleBuilder.setClassifier(Collections.singletonList(classifierBuilder.build()))
                .setAction(Collections.singletonList(actionBuilder.build()));
        return resolvedRuleBuilder.build();
    }

    private RuleGroupWithRendererEndpointParticipation createRuleGroupWithRendererEpParticipation(final EndpointPolicyParticipation participation) {
        RuleGroupWithRendererEndpointParticipationBuilder ruleGroupBuilder = new RuleGroupWithRendererEndpointParticipationBuilder();
        ruleGroupBuilder.setKey(new RuleGroupWithRendererEndpointParticipationKey(contractId, participation, subjectName,
                tenantId));
        return ruleGroupBuilder.build();
    }

    private AbsoluteLocation createAbsoluteLocationExternal(String connector, InstanceIdentifier mountpoint) {
        ExternalLocationCaseBuilder externalLocationCaseBuilder = new ExternalLocationCaseBuilder();
        externalLocationCaseBuilder.setExternalNodeConnector(connector)
                .setExternalNodeMountPoint(mountpoint);
        AbsoluteLocationBuilder absoluteLocationBuilder = new AbsoluteLocationBuilder();
        absoluteLocationBuilder.setLocationType(externalLocationCaseBuilder.build());
        return absoluteLocationBuilder.build();
    }

    private ServiceFunctionPath createServiceFunctionPath() {
        ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
        serviceFunctionPathBuilder.setKey(new ServiceFunctionPathKey(servicePath))
                .setSymmetric(false);
        return serviceFunctionPathBuilder.build();
    }

    private RenderedServicePath createRenderedServicePath() {
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();
        renderedServicePathHopBuilder.setServiceFunctionForwarder(forwarderName);
        renderedServicePathHopBuilder.setServiceFunctionName(functionName);
        renderedServicePathHopBuilder.build();

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();
        renderedServicePathBuilder.setKey(new RenderedServicePathKey(renderedPath))
                .setRenderedServicePathHop(Collections.singletonList(renderedServicePathHopBuilder.build()));
        return renderedServicePathBuilder.build();
    }


    private AddressEndpointWithLocation createAddressEndpointWithLocation(final ContextId contextId,
                                                                          final AbsoluteLocation location,
                                                                          boolean augmentation) {
        AddressEndpointWithLocationAugBuilder augmentationBuilder = new AddressEndpointWithLocationAugBuilder();
        augmentationBuilder.setSgt(new Sgt(1));
        AddressEndpointWithLocationBuilder addressEndpointBuilder = new AddressEndpointWithLocationBuilder();
        addressEndpointBuilder.setKey(new AddressEndpointWithLocationKey(address, IpPrefixType.class,
                contextId, L2BridgeDomain.class))
                .setAbsoluteLocation(location);
        if (augmentation) {
            addressEndpointBuilder.addAugmentation(AddressEndpointWithLocationAug.class, augmentationBuilder.build());
        }
        return addressEndpointBuilder.build();
    }

    private RendererEndpoint createRendererEndpoint(ContextId contextId_1, ContextId contextId_2,
                                                    RuleGroupWithRendererEndpointParticipation ruleGroup) {
        PeerEndpointBuilder PeerEndpointBuilder = new PeerEndpointBuilder();
        PeerEndpointBuilder.setKey(new PeerEndpointKey(address, IpPrefixType.class, contextId_1,
                L2BridgeDomain.class))
                .setRuleGroupWithRendererEndpointParticipation(Collections.singletonList(ruleGroup));
        RendererEndpointBuilder rendererEndpointBuilder = new RendererEndpointBuilder();
        rendererEndpointBuilder.setKey(new RendererEndpointKey(address, IpPrefixType.class, contextId_2,
                L2BridgeDomain.class))
                .setPeerEndpoint(Collections.singletonList(PeerEndpointBuilder.build()));
        return rendererEndpointBuilder.build();
    }

    private RuleGroup createRuleGroup(ResolvedRule rule) {
        RuleGroupBuilder ruleGroupBuilder = new RuleGroupBuilder();
        ruleGroupBuilder.setKey(new RuleGroupKey(contractId, subjectName, tenantId))
                .setResolvedRule(Collections.singletonList(rule));
        return ruleGroupBuilder.build();
    }

    private Configuration createTestConfiguration(final List<AddressEndpointWithLocation> endpointsWithLocation,
                                                  final List<RendererEndpoint> rendererEndpoints,
                                                  final RendererForwarding rendererForwarding,
                                                  final RuleGroup ruleGroup) {
        // Set endpoints
        EndpointsBuilder endpointsBuilder = new EndpointsBuilder();
        endpointsBuilder.setAddressEndpointWithLocation(endpointsWithLocation);
        // Set renderer endpoints
        RendererEndpointsBuilder rendererEndpointsBuilder = new RendererEndpointsBuilder();
        rendererEndpointsBuilder.setRendererEndpoint(rendererEndpoints);
        // Set rule group
        RuleGroupsBuilder ruleGroupsBuilder = new RuleGroupsBuilder();
        ruleGroupsBuilder.setRuleGroup(Collections.singletonList(ruleGroup));
        // Build configuration
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setEndpoints(endpointsBuilder.build())
                .setRendererEndpoints(rendererEndpointsBuilder.build())
                .setRendererForwarding(rendererForwarding)
                .setRuleGroups(ruleGroupsBuilder.build());
        return configurationBuilder.build();
    }


}