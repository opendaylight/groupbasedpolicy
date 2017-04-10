/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
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

public class NetworkService extends ServiceUtil {

    /**
     * Unit tests {@link NetworkServiceTest}
     */
    // ########### DHCP
    private static final long DHCP_IPV4_SERVER_PORT = 67;
    private static final long DHCP_IPV4_CLIENT_PORT = 68;
    private static final long DHCP_IPV6_SERVER_PORT = 547;
    private static final long DHCP_IPV6_CLIENT_PORT = 546;
    private static final ClassifierName DHCP_IPV4_CLIENT_SERVER_NAME =
            new ClassifierName("DHCP_IPv4_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName DHCP_IPV4_SERVER_CLIENT_NAME =
            new ClassifierName("DHCP_IPv4_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName DHCP_IPV6_CLIENT_SERVER_NAME =
            new ClassifierName("DHCP_IPv6_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName DHCP_IPV6_SERVER_CLIENT_NAME =
            new ClassifierName("DHCP_IPv6_FROM_SERVER_TO_CLIENT");
    private static final SubjectName DHCP_SUBJECT_NAME = new SubjectName("ALLOW_DHCP");
    private static final Description DHCP_CONTRACT_DESC =
            new Description("Allow DHCP communication between client and server.");

    /**
     * Id of {@link #DHCP_CONTRACT}
     */
    public static final ContractId DHCP_CONTRACT_ID = new ContractId("11118d2e-dddd-11e5-885d-feff819cdc9f");
    /**
     * Contains rules with action {@link MappingUtils#ACTION_REF_ALLOW} matching DHCP communication
     * between Client and Server.
     */
    public static final Contract DHCP_CONTRACT;
    /**
     * {@link ConsumerNamedSelector} pointing to {@link #DHCP_CONTRACT}
     */
    public static final ConsumerNamedSelector DHCP_CONTRACT_CONSUMER_SELECTOR;

    // ########### DNS
    private static final long DNS_SERVER_PORT = 53;
    private static final ClassifierName DNS_UDP_IPV4_CLIENT_SERVER_NAME =
            new ClassifierName("DNS_UDP_IPv4_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName DNS_UDP_IPV4_SERVER_CLIENT_NAME =
            new ClassifierName("DNS_UDP_IPv4_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName DNS_UDP_IPV6_CLIENT_SERVER_NAME =
            new ClassifierName("DNS_UDP_IPv6_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName DNS_UDP_IPV6_SERVER_CLIENT_NAME =
            new ClassifierName("DNS_UDP_IPv6_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName DNS_TCP_IPV4_CLIENT_SERVER_NAME =
            new ClassifierName("DNS_TCP_IPv4_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName DNS_TCP_IPV4_SERVER_CLIENT_NAME =
            new ClassifierName("DNS_TCP_IPv4_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName DNS_TCP_IPV6_CLIENT_SERVER_NAME =
            new ClassifierName("DNS_TCP_IPv6_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName DNS_TCP_IPV6_SERVER_CLIENT_NAME =
            new ClassifierName("DNS_TCP_IPv6_FROM_SERVER_TO_CLIENT");
    private static final SubjectName DNS_SUBJECT_NAME = new SubjectName("ALLOW_DNS");
    private static final Description DNS_CONTRACT_DESC =
            new Description("Allow DNS communication between client and server.");
    /**
     * ID of {@link #DNS_CONTRACT}
     */
    public static final ContractId DNS_CONTRACT_ID = new ContractId("22218d2e-dddd-11e5-885d-feff819cdc9f");
    /**
     * Contains rules with action {@link MappingUtils#ACTION_REF_ALLOW} matching DNS communication
     * between Client and Server.
     */
    public static final Contract DNS_CONTRACT;
    /**
     * {@link ConsumerNamedSelector} pointing to {@link #DNS_CONTRACT}
     */
    public static final ConsumerNamedSelector DNS_CONTRACT_CONSUMER_SELECTOR;

    // ########### SSH and ICMP management
    private static final long SSH_TCP_PORT = 22;
    private static final ClassifierName SSH_IPV4_SERVER_TO_CLIENT_NAME =
            new ClassifierName("SSH_IPV4_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName SSH_IPV6_SERVER_TO_CLIENT_NAME =
            new ClassifierName("SSH_IPV6_FROM_SERVER_TO_CLIENT");
    private static final ClassifierName SSH_IPV4_CLIENT_TO_SERVER_NAME =
            new ClassifierName("SSH_IPV4_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName SSH_IPV6_CLIENT_TO_SERVER_NAME =
            new ClassifierName("SSH_IPV6_FROM_CLIENT_TO_SERVER");
    private static final ClassifierName ICMP_IPV4_BETWEEN_SERVER_CLIENT_NAME =
            new ClassifierName("ICMP_IPV4_BETWEEN_SERVER_CLIENT");
    private static final ClassifierName ICMP_IPV6_BETWEEN_SERVER_CLIENT_NAME =
            new ClassifierName("ICMP_IPV6_BETWEEN_SERVER_CLIENT");
    private static final SubjectName MGMT_SUBJECT_NAME = new SubjectName("ALLOW_MGMT");
    private static final Description MGMT_CONTRACT_DESC =
            new Description("Allow ICMP and SSH management communication between server and client.");

    /**
     * Id of {@link #MGMT_CONTRACT}
     */
    public static final ContractId MGMT_CONTRACT_ID = new ContractId("33318d2e-dddd-11e5-885d-feff819cdc9f");
    /**
     * Contains rules with action {@link MappingUtils#ACTION_REF_ALLOW} matching ICMP and SSH
     * communication
     * between Client and Server.
     */
    public static final Contract MGMT_CONTRACT;
    /**
     * {@link ConsumerNamedSelector} pointing to {@link #MGMT_CONTRACT}
     */
    public static final ConsumerNamedSelector MGMT_CONTRACT_CONSUMER_SELECTOR;

    // ########### NETWORK-SERVICE ENDPOINT-GROUP
    private static final Name NETWORK_SERVICE_EPG_NAME = new Name("NETWORK_SERVICE");
    private static final Description NETWORK_SERVICE_EPG_DESC = new Description("Represents DHCP and DNS servers.");
    /**
     * ID of {@link #EPG}
     */
    public static final EndpointGroupId EPG_ID = new EndpointGroupId("ddd6cfe6-dfe5-11e4-8a00-1681e6b88ec1");
    /**
     * Network-service endpoint-group providing {@link #DHCP_CONTRACT} and {@link #DNS_CONTRACT}
     */
    public static final EndpointGroup EPG;

    static {
        DHCP_CONTRACT = createContractDhcp();
        DHCP_CONTRACT_CONSUMER_SELECTOR = createConsumerSelector(DHCP_CONTRACT);
        DNS_CONTRACT = createContractDns();
        DNS_CONTRACT_CONSUMER_SELECTOR = createConsumerSelector(DNS_CONTRACT);
        MGMT_CONTRACT = createContractMgmt();
        MGMT_CONTRACT_CONSUMER_SELECTOR = createConsumerSelector(MGMT_CONTRACT);
        EPG = createNetworkServiceEpg();
    }

    private static EndpointGroup createNetworkServiceEpg() {
        ProviderNamedSelector dhcpProviderSelector = createProviderSelector(DHCP_CONTRACT);
        ProviderNamedSelector dnsProviderSelector = createProviderSelector(DNS_CONTRACT);
        ProviderNamedSelector mgmtProviderSelector = createProviderSelector(MGMT_CONTRACT);
        return createEpgBuilder(EPG_ID, NETWORK_SERVICE_EPG_NAME, NETWORK_SERVICE_EPG_DESC)
            .setProviderNamedSelector(ImmutableList.of(dhcpProviderSelector, dnsProviderSelector, mgmtProviderSelector))
            .build();
    }

    private static Contract createContractDhcp() {
        Rule clientServerIpv4Rule = createRuleAllow(DHCP_IPV4_CLIENT_SERVER_NAME, Direction.In);
        Rule serverClientIpv4Rule = createRuleAllow(DHCP_IPV4_SERVER_CLIENT_NAME, Direction.Out);
        Rule clientServerIpv6Rule = createRuleAllow(DHCP_IPV6_CLIENT_SERVER_NAME, Direction.In);
        Rule serverClientIpv6Rule = createRuleAllow(DHCP_IPV6_SERVER_CLIENT_NAME, Direction.Out);
        Subject subject = new SubjectBuilder().setName(DHCP_SUBJECT_NAME)
            .setOrder(0)
            .setRule(ImmutableList.of(clientServerIpv4Rule, serverClientIpv4Rule, clientServerIpv6Rule,
                    serverClientIpv6Rule))
            .build();
        return createContract(DHCP_CONTRACT_ID, ImmutableList.of(subject), DHCP_CONTRACT_DESC);
    }

    private static Contract createContractDns() {
        Rule clientServerUdpIpv4Rule = createRuleAllow(DNS_UDP_IPV4_CLIENT_SERVER_NAME, Direction.In);
        Rule serverClientUdpIpv4Rule = createRuleAllow(DNS_UDP_IPV4_SERVER_CLIENT_NAME, Direction.Out);
        Rule clientServerUdpIpv6Rule = createRuleAllow(DNS_UDP_IPV6_CLIENT_SERVER_NAME, Direction.In);
        Rule serverClientUdpIpv6Rule = createRuleAllow(DNS_UDP_IPV6_SERVER_CLIENT_NAME, Direction.Out);
        Rule clientServerTcpIpv4Rule = createRuleAllow(DNS_TCP_IPV4_CLIENT_SERVER_NAME, Direction.In);
        Rule serverClientTcpIpv4Rule = createRuleAllow(DNS_TCP_IPV4_SERVER_CLIENT_NAME, Direction.Out);
        Rule clientServerTcpIpv6Rule = createRuleAllow(DNS_TCP_IPV6_CLIENT_SERVER_NAME, Direction.In);
        Rule serverClientTcpIpv6Rule = createRuleAllow(DNS_TCP_IPV6_SERVER_CLIENT_NAME, Direction.Out);
        Subject subject = new SubjectBuilder().setName(DNS_SUBJECT_NAME)
            .setOrder(0)
            .setRule(ImmutableList.of(clientServerUdpIpv4Rule, serverClientUdpIpv4Rule, clientServerUdpIpv6Rule,
                    serverClientUdpIpv6Rule, clientServerTcpIpv4Rule, serverClientTcpIpv4Rule, clientServerTcpIpv6Rule,
                    serverClientTcpIpv6Rule))
            .build();
        return createContract(DNS_CONTRACT_ID, ImmutableList.of(subject), DNS_CONTRACT_DESC);
    }

    private static Contract createContractMgmt() {
        Rule serverClientSshIpv4Rule = createRuleAllow(SSH_IPV4_SERVER_TO_CLIENT_NAME, Direction.Out);
        Rule serverClientSshIpv6Rule = createRuleAllow(SSH_IPV6_SERVER_TO_CLIENT_NAME, Direction.Out);
        Rule clientServerSshIpv4Rule = createRuleAllow(SSH_IPV4_CLIENT_TO_SERVER_NAME, Direction.In);
        Rule clientServerSshIpv6Rule = createRuleAllow(SSH_IPV6_CLIENT_TO_SERVER_NAME, Direction.In);
        Rule serverClientIcmpIpv4Rule = createRuleAllow(ICMP_IPV4_BETWEEN_SERVER_CLIENT_NAME, Direction.Out);
        Rule serverClientIcmpIpv6Rule = createRuleAllow(ICMP_IPV6_BETWEEN_SERVER_CLIENT_NAME, Direction.Out);
        Rule clientServerIcmpIpv4Rule = createRuleAllow(ICMP_IPV4_BETWEEN_SERVER_CLIENT_NAME, Direction.In);
        Rule clientServerIcmpIpv6Rule = createRuleAllow(ICMP_IPV6_BETWEEN_SERVER_CLIENT_NAME, Direction.In);

        Subject subject = new SubjectBuilder().setName(MGMT_SUBJECT_NAME)
            .setOrder(0)
            .setRule(ImmutableList.of(serverClientSshIpv4Rule, serverClientSshIpv6Rule, clientServerSshIpv4Rule,
                    clientServerSshIpv6Rule, clientServerIcmpIpv4Rule, clientServerIcmpIpv6Rule,
                    serverClientIcmpIpv4Rule, serverClientIcmpIpv6Rule))
            .build();
        return createContract(MGMT_CONTRACT_ID, ImmutableList.of(subject), MGMT_CONTRACT_DESC);
    }

    /**
     * puts clause with {@link L3EndpointIdentificationConstraints} in {@link ConsumerMatchers}
     * and {@link ProviderMatchers}. This clause points to subject in {@link #DHCP_CONTRACT}.
     *
     * @param tenantId location of {@link #DHCP_CONTRACT}
     * @param ipPrefix used in {@link L3EndpointIdentificationConstraints}
     * @param wTx transaction where entities are written
     */
    public static void writeDhcpClauseWithConsProvEic(TenantId tenantId, @Nullable IpPrefix ipPrefix,
            WriteTransaction wTx) {
        Clause clause = createClauseWithConsProvEic(ipPrefix, DHCP_SUBJECT_NAME);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.clauseIid(tenantId, DHCP_CONTRACT_ID, clause.getName()),
                clause, true);
    }

    /**
     * puts clause with {@link L3EndpointIdentificationConstraints} in {@link ConsumerMatchers}
     * and {@link ProviderMatchers}. This clause points to subject in {@link #DNS_CONTRACT}.
     *
     * @param tenantId location of {@link #DNS_CONTRACT}
     * @param ipPrefix used in {@link L3EndpointIdentificationConstraints}
     * @param wTx transaction where entities are written
     */
    public static void writeDnsClauseWithConsProvEic(TenantId tenantId, @Nullable IpPrefix ipPrefix,
            WriteTransaction wTx) {
        Clause clause = createClauseWithConsProvEic(ipPrefix, DNS_SUBJECT_NAME);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.clauseIid(tenantId, DNS_CONTRACT_ID, clause.getName()),
                clause, true);
    }

    /**
     * puts clause with {@link L3EndpointIdentificationConstraints} in {@link ConsumerMatchers}
     * and {@link ProviderMatchers}. This clause points to subject in {@link #MGMT_CONTRACT}.
     *
     * @param tenantId location of {@link #MGMT_CONTRACT}
     * @param ipPrefix used in {@link L3EndpointIdentificationConstraints}
     * @param wTx transaction where entities are written
     */
    public static void writeMgmtClauseWithConsProvEic(TenantId tenantId, @Nullable IpPrefix ipPrefix,
            WriteTransaction wTx) {
        Clause clause = createClauseWithConsProvEic(ipPrefix, MGMT_SUBJECT_NAME);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.clauseIid(tenantId, MGMT_CONTRACT_ID, clause.getName()),
                clause, true);
    }

    /**
     * Puts network service entities (classifier-instances, {@link #DHCP_CONTRACT},
     * {@link #DNS_CONTRACT}, {@link #MGMT_CONTRACT} and {@link #EPG}) to
     * {@link LogicalDatastoreType#CONFIGURATION}
     *
     * @param tenantId location of network-service entities
     * @param wTx transaction where network-service entities are written
     */
    public static void writeNetworkServiceEntitiesToTenant(TenantId tenantId, WriteTransaction wTx) {
        Set<ClassifierInstance> classifierInstances = getAllClassifierInstances();
        for (ClassifierInstance ci : classifierInstances) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.classifierInstanceIid(tenantId, ci.getName()), ci,
                    true);
        }
        for (ActionInstance ai : Collections.singleton(MappingUtils.ACTION_ALLOW)) {
            wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.actionInstanceIid(tenantId, ai.getName()), ai, true);
        }
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, DHCP_CONTRACT_ID), DHCP_CONTRACT,
                true);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, DNS_CONTRACT_ID), DNS_CONTRACT,
                true);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.contractIid(tenantId, MGMT_CONTRACT_ID), MGMT_CONTRACT,
                true);
        wTx.put(LogicalDatastoreType.CONFIGURATION, IidFactory.endpointGroupIid(tenantId, EPG_ID), EPG, true);
    }

    /**
     * @return All classifier-instances used in {@link #DHCP_CONTRACT}, {@link #DNS_CONTRACT} and
     *         {@link #MGMT_CONTRACT}
     */
    public static Set<ClassifierInstance> getAllClassifierInstances() {
        HashSet<ClassifierInstance> cis = new HashSet<>();
        cis.add(createDhcpIpv4ClientServer());
        cis.add(createDhcpIpv4ServerClient());
        cis.add(createDhcpIpv6ClientServer());
        cis.add(createDhcpIpv6ServerClient());
        cis.add(createDnsUdpIpv4ClientServer());
        cis.add(createDnsUdpIpv4ServerClient());
        cis.add(createDnsUdpIpv6ClientServer());
        cis.add(createDnsUdpIpv6ServerClient());
        cis.add(createDnsTcpIpv4ClientServer());
        cis.add(createDnsTcpIpv4ServerClient());
        cis.add(createDnsTcpIpv6ClientServer());
        cis.add(createDnsTcpIpv6ServerClient());
        // MGMT
        cis.add(createSshTcpIpv4ServerClient());
        cis.add(createSshTcpIpv6ServerClient());
        cis.add(createSshTcpIpv4ClientServer());
        cis.add(createSshTcpIpv6ClientServer());
        cis.add(createIcmpIpv4());
        cis.add(createIcmpIpv6());
        return cis;
    }

    // ###################### DHCP
    private static ClassifierInstance createDhcpIpv4ClientServer() {
        return createClassifInstance(DHCP_IPV4_CLIENT_SERVER_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE, IpProtoClassifierDefinition.UDP_VALUE,
                        DHCP_IPV4_CLIENT_PORT, DHCP_IPV4_SERVER_PORT));
    }

    private static ClassifierInstance createDhcpIpv4ServerClient() {
        return createClassifInstance(DHCP_IPV4_SERVER_CLIENT_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE, IpProtoClassifierDefinition.UDP_VALUE,
                        DHCP_IPV4_SERVER_PORT, DHCP_IPV4_CLIENT_PORT));
    }

    private static ClassifierInstance createDhcpIpv6ClientServer() {
        return createClassifInstance(DHCP_IPV6_CLIENT_SERVER_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE, IpProtoClassifierDefinition.UDP_VALUE,
                        DHCP_IPV6_CLIENT_PORT, DHCP_IPV6_SERVER_PORT));
    }

    private static ClassifierInstance createDhcpIpv6ServerClient() {
        return createClassifInstance(DHCP_IPV6_SERVER_CLIENT_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE, IpProtoClassifierDefinition.UDP_VALUE,
                        DHCP_IPV6_SERVER_PORT, DHCP_IPV6_CLIENT_PORT));
    }

    // ###################### DNS UDP
    private static ClassifierInstance createDnsUdpIpv4ClientServer() {
        return createClassifInstance(DNS_UDP_IPV4_CLIENT_SERVER_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE, IpProtoClassifierDefinition.UDP_VALUE, null,
                        DNS_SERVER_PORT));
    }

    private static ClassifierInstance createDnsUdpIpv4ServerClient() {
        return createClassifInstance(DNS_UDP_IPV4_SERVER_CLIENT_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE, IpProtoClassifierDefinition.UDP_VALUE,
                        DNS_SERVER_PORT, null));
    }

    private static ClassifierInstance createDnsUdpIpv6ClientServer() {
        return createClassifInstance(DNS_UDP_IPV6_CLIENT_SERVER_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE, IpProtoClassifierDefinition.UDP_VALUE, null,
                        DNS_SERVER_PORT));
    }

    private static ClassifierInstance createDnsUdpIpv6ServerClient() {
        return createClassifInstance(DNS_UDP_IPV6_SERVER_CLIENT_NAME, L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE, IpProtoClassifierDefinition.UDP_VALUE,
                        DNS_SERVER_PORT, null));
    }

    // ###################### DNS TCP
    private static ClassifierInstance createDnsTcpIpv4ClientServer() {
        return createClassifInstance(DNS_TCP_IPV4_CLIENT_SERVER_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, null, DNS_SERVER_PORT));
    }

    private static ClassifierInstance createDnsTcpIpv4ServerClient() {
        return createClassifInstance(DNS_TCP_IPV4_SERVER_CLIENT_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, DNS_SERVER_PORT, null));
    }

    private static ClassifierInstance createDnsTcpIpv6ClientServer() {
        return createClassifInstance(DNS_TCP_IPV6_CLIENT_SERVER_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, null, DNS_SERVER_PORT));
    }

    private static ClassifierInstance createDnsTcpIpv6ServerClient() {
        return createClassifInstance(DNS_TCP_IPV6_SERVER_CLIENT_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, DNS_SERVER_PORT, null));
    }

    // ###################### SSH TCP
    private static ClassifierInstance createSshTcpIpv4ClientServer() {
        return createClassifInstance(SSH_IPV4_CLIENT_TO_SERVER_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, SSH_TCP_PORT, null));
    }

    private static ClassifierInstance createSshTcpIpv4ServerClient() {
        return createClassifInstance(SSH_IPV4_SERVER_TO_CLIENT_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv4_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, null, SSH_TCP_PORT));
    }

    private static ClassifierInstance createSshTcpIpv6ClientServer() {
        return createClassifInstance(SSH_IPV6_CLIENT_TO_SERVER_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, SSH_TCP_PORT, null));
    }

    private static ClassifierInstance createSshTcpIpv6ServerClient() {
        return createClassifInstance(SSH_IPV6_SERVER_TO_CLIENT_NAME,
                L4ClassifierDefinition.DEFINITION.getId(),
                createParams(EtherTypeClassifierDefinition.IPv6_VALUE,
                        IpProtoClassifierDefinition.TCP_VALUE, null, SSH_TCP_PORT));
    }

    // ###################### ICMP
    private static ClassifierInstance createIcmpIpv4() {
        return createClassifInstance(ICMP_IPV4_BETWEEN_SERVER_CLIENT_NAME,
                IpProtoClassifierDefinition.DEFINITION.getId(), createParams(
                        EtherTypeClassifierDefinition.IPv4_VALUE, IpProtoClassifierDefinition.ICMP_VALUE, null, null));
    }

    private static ClassifierInstance createIcmpIpv6() {
        return createClassifInstance((ICMP_IPV6_BETWEEN_SERVER_CLIENT_NAME),
                IpProtoClassifierDefinition.DEFINITION.getId(), createParams(
                        EtherTypeClassifierDefinition.IPv6_VALUE, IpProtoClassifierDefinition.ICMP_VALUE, null, null));
    }
}
