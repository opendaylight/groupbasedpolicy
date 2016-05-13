/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.ActionChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.AllowActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.SfcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.action.action.choice.allow.action._case.AllowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstanceBuilder;

public final class MappingUtils {

    public static final String NEUTRON_ROUTER = "neutron_router-";
    public static final String NEUTRON_EXTERNAL = "neutron_external_network-";
    public static final String NEUTRON_GROUP = "neutron_group-";
    public static final IpPrefix DEFAULT_ROUTE = new IpPrefix(new Ipv4Prefix("0.0.0.0/0"));
    public static final ActionInstance ACTION_ALLOW = new ActionInstanceBuilder().setName(
            new ActionName("Allow"))
        .setActionDefinitionId(AllowActionDefinition.DEFINITION.getId())
        .build();
    public static final ActionChoice ALLOW_ACTION_CHOICE = new AllowActionCaseBuilder().setAllow(
            new AllowBuilder().build()).build();
    public static final ActionRef ACTION_REF_ALLOW =
            new ActionRefBuilder().setName(ACTION_ALLOW.getName()).setOrder(0).build();
    public static final Uuid EIG_UUID = new Uuid("eeeaa3a2-e9ba-44e0-a462-bea923d30e38");
    public static final EndpointGroupId EPG_EXTERNAL_ID = new EndpointGroupId(EIG_UUID.getValue());

    public static final String NAME_VALUE_DELIMETER = "-";
    public static final String NAME_DELIMETER = "_";
    public static final String NAME_DOUBLE_DELIMETER = "__";

    private MappingUtils() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    public static ActionRef createSfcActionRef(String sfcChainName) {
        return new ActionRefBuilder().setName(new ActionName(sfcChainName)).setOrder(0).build();
    }

    public static ActionChoice createSfcActionChoice(String chainName) {
        return new SfcActionCaseBuilder().setSfcChainName(chainName).build();
    }

}
