/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.ClassNameType;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.PolicyActionType;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.SecurityGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.Destination;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.Source;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._class.map.match.grouping.security.group.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.Type;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.TypeBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMapBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.PolicyMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.Match;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native._class.map.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.ClassKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.ActionList;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.ActionListBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.ActionListKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.ForwardCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.ForwardBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.forward.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.forward.ServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map._class.action.list.action.param.forward._case.forward.ServicePathKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.manager.PolicyManagerImpl.ActionCase.CHAIN;

class PolicyMapper {

    private final DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(PolicyMapper.class);

    PolicyMapper(final DataBroker dataBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    Match createSecurityGroupMatch(int sourceTag, int destinationTag) {
        MatchBuilder matchBuilder = new MatchBuilder();
        SecurityGroupBuilder sgBuilder = new SecurityGroupBuilder();
        Source source = new SourceBuilder().setTag(sourceTag).build();
        Destination destination = new DestinationBuilder().setTag(destinationTag).build();
        sgBuilder.setSource(source)
                .setDestination(destination);
        return matchBuilder.setSecurityGroup(sgBuilder.build()).build();
    }

    ClassMap createClassMap(final String classMapName, Match match) {
        ClassMapBuilder cmBuilder = new ClassMapBuilder();
        cmBuilder.setName(classMapName)
                .setKey(new ClassMapKey(classMapName))
                .setMatch(match)
                .setPrematch(ClassMap.Prematch.MatchAll);
        return cmBuilder.build();
    }

    Class createPolicyEntry(String policyClassName, RenderedServicePath renderedPath,
                            PolicyManagerImpl.ActionCase actionCase) {
        // Forward Case
        ForwardCaseBuilder forwardCaseBuilder = new ForwardCaseBuilder();
        if (actionCase.equals(CHAIN) && renderedPath != null) {
            // Chain Action
            ForwardBuilder forwardBuilder = new ForwardBuilder();
            List<ServicePath> servicePaths = new ArrayList<>();
            ServicePathBuilder servicePathBuilder = new ServicePathBuilder();
            servicePathBuilder.setKey(new ServicePathKey(renderedPath.getPathId()))
                    .setServicePathId(renderedPath.getPathId())
                    .setServiceIndex(renderedPath.getStartingIndex());
            servicePaths.add(servicePathBuilder.build());
            forwardBuilder.setServicePath(servicePaths);
            forwardCaseBuilder.setForward(forwardBuilder.build());
        }
        // Create Action List
        List<ActionList> actionList = new ArrayList<>();
        ActionListBuilder actionListBuilder = new ActionListBuilder();
        actionListBuilder.setKey(new ActionListKey(PolicyActionType.Forward))
                .setActionType(PolicyActionType.Forward)
                .setActionParam(forwardCaseBuilder.build());
        actionList.add(actionListBuilder.build());
        // Build class entry
        ClassBuilder policyClassBuilder = new ClassBuilder();
        policyClassBuilder.setName(new ClassNameType(policyClassName))
                .setKey(new ClassKey(new ClassNameType(policyClassName)))
                .setActionList(actionList);
        return policyClassBuilder.build();
    }

    public Type getServicePolicyType(String name) {
        TypeBuilder typeBuilder = new TypeBuilder();
        org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChainBuilder serviceChainBuilder =
                new org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChainBuilder();
        serviceChainBuilder.setName(name)
                .setDirection(org.opendaylight.yang.gen.v1.urn.ios.rev160308._interface.common.grouping.service.policy.type.ServiceChain.Direction.Input);
        typeBuilder.setServiceChain(serviceChainBuilder.build());
        return typeBuilder.build();
    }

    PolicyMap policyMap(String policyMapName) {
        PolicyMapBuilder pmBuilder = new PolicyMapBuilder();
        pmBuilder.setName(policyMapName)
                .setKey(new PolicyMapKey(policyMapName))
                .setType(null);
        return pmBuilder.build();
    }
}
