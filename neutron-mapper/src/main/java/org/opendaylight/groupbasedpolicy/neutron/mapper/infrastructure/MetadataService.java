/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.infrastructure;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.MappingUtils;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Description;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.Name;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.endpoint.identification.constraints.endpoint.identification.constraints.L3EndpointIdentificationConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ConsumerMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.clause.ProviderMatchers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;

import com.google.common.collect.ImmutableList;

public class MetadataService extends ServiceUtil {

    private static final ClassifierName METADATA_SERVER_TO_CLIENT_NAME =
        new ClassifierName("METADATA_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName METADATA_CLIENT_TO_SERVER_NAME =
        new ClassifierName("METADATA_FROM_CLIENT_TO_SERVER");
    private static final SubjectName METADATA_SUBJECT_NAME = new SubjectName("ALLOW_METADATA");
    private static final Description METADATA_CONTRACT_DESC =
        new Description("Allow METADATA management communication between server and client.");

    /**
     * Id of {@link #METADATA_CONTRACT}
     */
    public static final ContractId METADATA_CONTRACT_ID = new ContractId("be0675b7-b0d6-46cc-acf1-247ed31cf572");
    /**
     * Contains rules with action {@link MappingUtils#ACTION_REF_ALLOW} matching ICMP and SSH
     * communication
     * between Client and Server.
     */
    public static final Contract METADATA_CONTRACT;
    /**
     * {@link ConsumerNamedSelector} pointing to {@link #METADATA_CONTRACT}
     */
    public static final ConsumerNamedSelector METADATA_CONTRACT_CONSUMER_SELECTOR;

    // ########### NETWORK-SERVICE ENDPOINT-GROUP
    private static final Name METADATA_SERVICE_EPG_NAME = new Name("NETWORK_SERVICE");
    private static final Description METADATA_SERVICE_EPG_DESC = new Description("Represents DHCP and DNS servers.");
    /**
     * ID of {@link #EPG}
     */
    public static final EndpointGroupId EPG_ID = new EndpointGroupId("ffff1111-dfe5-11e4-8a00-1681e6b88ec1");
    /**
     * Network-service endpoint-group providing {@link #METADATA_CONTRACT}
     */
    public static final EndpointGroup EPG;

    static {
        METADATA_CONTRACT = createContractMetadata();
        METADATA_CONTRACT_CONSUMER_SELECTOR = createConsumerSelector(METADATA_CONTRACT);
        EPG = createNetworkServiceEpg();
    }

    private static EndpointGroup createNetworkServiceEpg() {
        ProviderNamedSelector metadataProviderSelector = createProviderSelector(METADATA_CONTRACT);
        return createEpgBuilder(EPG_ID, METADATA_SERVICE_EPG_NAME, METADATA_SERVICE_EPG_DESC)
            .setProviderNamedSelector(ImmutableList.of(metadataProviderSelector))
            .build();
    }

    private static Contract createContractMetadata() {
        Rule serverClientMetadataIpv4Rule = createRuleAllow(METADATA_SERVER_TO_CLIENT_NAME, Direction.Out);
        Rule clientServerMetadataIpv4Rule = createRuleAllow(METADATA_CLIENT_TO_SERVER_NAME, Direction.In);
        Subject subject = new SubjectBuilder().setName(METADATA_SUBJECT_NAME)
            .setOrder(0)
            .setRule(ImmutableList.of(serverClientMetadataIpv4Rule, clientServerMetadataIpv4Rule))
            .build();
        return createContract(METADATA_CONTRACT_ID, ImmutableList.of(subject), METADATA_CONTRACT_DESC);
    }

    /**
     * puts clause with {@link L3EndpointIdentificationConstraints} in {@link ConsumerMatchers}
     * and {@link ProviderMatchers}. This clause points to subject in {@link #METADATA_CONTRACT}.
     *
     * @param tenantId location of {@link #METADATA_CONTRACT}
     * @param ipPrefix used in {@link L3EndpointIdentificationConstraints}
     * @param wTx transaction where entities are written
     */
    public static void writeMetadataClauseWithConsProvEic(TenantId tenantId, @Nullable IpPrefix ipPrefix,
        WriteTransaction wTx) {
        Clause clause = createClauseWithConsProvEic(ipPrefix, METADATA_SUBJECT_NAME);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.clauseIid(tenantId, METADATA_CONTRACT_ID, clause.getName()),
            clause, true);
    }

    /**
     * Puts network service entities (classifier-instances, {@link #METADATA_CONTRACT},
     * and {@link #EPG}) to {@link LogicalDatastoreType#CONFIGURATION}
     *
     * @param tenantId location of network-service entities
     * @param wTx transaction where network-service entities are written
     */
    public static void writeNetworkServiceEntitiesToTenant(TenantId tenantId, WriteTransaction wTx, long metadataPort) {
        Set<ClassifierInstance> classifierInstances = getAllClassifierInstances(metadataPort);
        for (ClassifierInstance ci : classifierInstances) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierInstanceIid(tenantId, ci.getName()), ci,
                    true);
        }
        for (ActionInstance ai : Collections.singleton(MappingUtils.ACTION_ALLOW)) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.actionInstanceIid(tenantId, ai.getName()), ai, true);
        }
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, METADATA_CONTRACT_ID), METADATA_CONTRACT,
                true);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, EPG_ID), EPG, true);
    }

    /**
     * @return All classifier-instances used in {@link #METADATA_CONTRACT}
     */
    public static Set<ClassifierInstance> getAllClassifierInstances(long metadataPort) {
        HashSet<ClassifierInstance> cis = new HashSet<>();
        // METADATA
        cis.add(createMetadataIpv4ClientServer(metadataPort));
        cis.add(createMetadataIpv4ServerClient(metadataPort));
        return cis;
    }

    private static ClassifierInstance createMetadataIpv4ClientServer(long dstPort) {
        return createClassifInstance(METADATA_CLIENT_TO_SERVER_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, null, dstPort));
    }

    private static ClassifierInstance createMetadataIpv4ServerClient(long srcPort) {
        return createClassifInstance(METADATA_SERVER_TO_CLIENT_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, srcPort, null));
    }
}
