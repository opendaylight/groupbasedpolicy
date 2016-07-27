/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.NeutronEntityFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.test.PolicyAssert;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.AllowActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.allow.action._case.AllowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.parameter.value.RangeValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;

public class SingleClassifierRuleTest {

    private ActionChoice allow = new AllowActionCaseBuilder().setAllow(new AllowBuilder().build()).build();
    private ActionChoice sfc = new SfcActionCaseBuilder().setSfcChainName("orange_chain").build();
    private SingleClassifierRule singleClsfRule;

    @Test
    public void testConstructorAndGetters() {
        SecurityRule secRule = NeutronEntityFactory.securityRuleWithoutGroupIds(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", EthertypeV4.class,
                DirectionIngress.class, ProtocolTcp.class, 8010, 8020);
        final int ruleOrder = 1;
        singleClsfRule = new SingleClassifierRule(secRule, ruleOrder, MappingUtils.ALLOW_ACTION_CHOICE);

        ClassifierInstance clsfInstance = singleClsfRule.getClassifierInstance();
        assertNotNull(clsfInstance);
        assertEquals(clsfInstance, SecRuleEntityDecoder.getClassifierInstance(secRule));

        ClassifierRef clsfRef = singleClsfRule.getClassifierRef();
        assertNotNull(clsfRef);
        assertEquals(clsfRef, SecRuleEntityDecoder.getClassifierRef(secRule));

        Rule rule = singleClsfRule.getRule();
        assertNotNull(rule);
        PolicyAssert.assertRule(rule, secRule, ruleOrder + 180);
    }

    @Test
    public final void updateOrderBasedOnEthTypeClassifTest() throws Exception {
        ClassifierInstance ci = createClassifierInstance(EtherTypeClassifierDefinition.ID);
        SingleClassifierRule.updateOrderBasedOn(ci, allow, 0);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 350);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, sfc, 0), 345);
    }

    @Test
    public final void updateOrderBasedOnProtoClassifTest() throws Exception {
        ClassifierInstance ci = createClassifierInstance(IpProtoClassifierDefinition.ID);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 300);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, sfc, 0), 295);
    }

    @Test
    public final void updateOrderBasedOnL4ClassifTest() throws Exception {
        ParameterValue sPort = createIntParameterValue(L4ClassifierDefinition.SRC_PORT_PARAM, Long.valueOf(4999));
        ParameterValue dPort = createIntParameterValue(L4ClassifierDefinition.DST_PORT_PARAM, Long.valueOf(8080));
        ParameterValue sMiniRange = createRangeParameterValue(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM, 1000, 1000);
        ParameterValue dMiniRange = createRangeParameterValue(L4ClassifierDefinition.DST_PORT_RANGE_PARAM, 2000, 2000);
        ParameterValue sPortRange = createRangeParameterValue(L4ClassifierDefinition.SRC_PORT_RANGE_PARAM, 3335, 3336);
        ParameterValue dPortRange = createRangeParameterValue(L4ClassifierDefinition.DST_PORT_RANGE_PARAM, 9998, 9999);
        ClassifierInstance ci;
        ci = createClassifierInstance(L4ClassifierDefinition.ID, dPortRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 180);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, sPortRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 180);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, sPort);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 170);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, sMiniRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 170);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, dPort);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 170);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, dMiniRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 170);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, sPortRange, dPortRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 160);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, sPortRange, dPort);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 150);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, sPortRange, dMiniRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 150);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, dPortRange, sPort);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 150);
        ci = createClassifierInstance(L4ClassifierDefinition.ID, dPortRange, sMiniRange);
        assertEquals(SingleClassifierRule.updateOrderBasedOn(ci, allow, 0), 150);
    }

    private ParameterValue createIntParameterValue(String paramName, Long value) {
        return new ParameterValueBuilder().setName(new ParameterName(paramName)).setIntValue(value).build();
    }

    private ParameterValue createRangeParameterValue(String paramName, int min, int max) {
        return new ParameterValueBuilder()
            .setName(new ParameterName(paramName))
            .setRangeValue(new RangeValueBuilder()
                .setMin(Long.valueOf(min))
                .setMax(Long.valueOf(max))
                .build())
            .build();
    }

    private ClassifierInstance createClassifierInstance(ClassifierDefinitionId id, ParameterValue... pv) {
        return new ClassifierInstanceBuilder()
            .setClassifierDefinitionId(id)
            .setParameterValue(Arrays.asList(pv))
            .build();
    }
}
