/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.SecurityRuleUtils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.ChangeActionOfSecurityGroupRulesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.NeutronGbpMapperService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.change.action.of.security.group.rules.input.SecurityGroupRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

public class NeutronGbpMapperServiceImpl implements NeutronGbpMapperService {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronGbpMapperServiceImpl.class);
    private NeutronSecurityRuleAware secRuleAware;
    private DataBroker dataProvider;

    public NeutronGbpMapperServiceImpl(DataBroker dataProvider, NeutronSecurityRuleAware secRuleAware) {
        this.dataProvider = checkNotNull(dataProvider);
        this.secRuleAware = checkNotNull(secRuleAware);
    }

    @Override
    public Future<RpcResult<Void>> changeActionOfSecurityGroupRules(ChangeActionOfSecurityGroupRulesInput input) {
        List<SecurityGroupRule> securityGroupRules = input.getSecurityGroupRule();
        if (securityGroupRules == null || input.getAction() == null) {
            LOG.debug("Missing params in request:\n{}", input);
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.PROTOCOL, "Missing params. Changing to action "
                        + input.getAction().getActionChoice() + " was not successful.")
                .build());
        }

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        Optional<Neutron> potentialNeutron;
        try {
            potentialNeutron = rwTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Neutron.class).build()).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception during neutron reading.", e);
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                    .withError(ErrorType.PROTOCOL, "Cannot read from CONF DS.")
                    .build());
        }
        if (!potentialNeutron.isPresent()) {
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                    .withError(ErrorType.PROTOCOL, "No neutron data in CONF DS.")
                    .build());
        }
        Neutron neutron = potentialNeutron.get();
        for (SecurityGroupRule secGrpRule : securityGroupRules) {
            Uuid uuid = secGrpRule.getUuid();
            Optional<SecurityRule> potentialSecRule = SecurityRuleUtils.findSecurityRule(uuid, neutron.getSecurityRules());
            if (!potentialSecRule.isPresent()) {
                LOG.warn("Security rule {} does not exist.", uuid);
                continue;
            }
            LOG.trace("Changing action to {} in security group rule {}", input.getAction().getActionChoice(), uuid);
            boolean isSuccessful =
                    secRuleAware.changeActionOfNeutronSecurityRule(potentialSecRule.get(), input.getAction().getActionChoice(), neutron, rwTx);
            if (!isSuccessful) {
                rwTx.cancel();
                LOG.warn("Changing action to {} in security group rule {} was not successful.",
                        input.getAction().getActionChoice(), uuid);
                return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                    .withError(ErrorType.APPLICATION,
                            "Changing to action " + input.getAction().getActionChoice() + " was not successful.")
                    .build());
            }
        }
        boolean isSubmittedToDs = DataStoreHelper.submitToDs(rwTx);
        if (!isSubmittedToDs) {
            LOG.warn("Changing action to {} in security group rules {} was not successful.",
                    input.getAction().getActionChoice(), input.getSecurityGroupRule());
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                .withError(ErrorType.APPLICATION, "Storing to datastore was not successful. Changing to action "
                        + input.getAction().getActionChoice() + " was not successful.")
                .build());
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

}
