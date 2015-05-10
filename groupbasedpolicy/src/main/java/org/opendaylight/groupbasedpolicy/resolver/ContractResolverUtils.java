package org.opendaylight.groupbasedpolicy.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.ConsumerSelectionRelator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.ProviderSelectionRelator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.target.selector.QualityMatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Target;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerTargetSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderTargetSelector;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


public class ContractResolverUtils {

    private ContractResolverUtils() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    /**
     * Choose the contracts that are in scope for each pair of endpoint groups,
     * then perform subject selection for the pair
     */
    protected static Table<EgKey, EgKey, List<ContractMatch>> selectContracts(Set<IndexedTenant> tenants) {
        Table<TenantId, ContractId, List<ConsumerContractMatch>> consumerMatches = HashBasedTable.create();
        Table<EgKey, EgKey, List<ContractMatch>> contractMatches = HashBasedTable.create();

        for (IndexedTenant tenant : tenants) {
            selectContracts(consumerMatches, contractMatches, tenant.getTenant());
        }
        return contractMatches;
    }

    protected static void selectContracts(Table<TenantId, ContractId, List<ConsumerContractMatch>> consumerMatches,
            Table<EgKey, EgKey, List<ContractMatch>> contractMatches, Tenant tenant) {
        // For each endpoint group, match consumer selectors
        // against contracts to get a set of matching consumer selectors
        if (tenant.getEndpointGroup() == null)
            return;
        for (EndpointGroup group : tenant.getEndpointGroup()) {
            List<ConsumerContractMatch> r = matchConsumerContracts(tenant, group);
            for (ConsumerContractMatch ccm : r) {
                List<ConsumerContractMatch> cms = consumerMatches.get(tenant.getId(), ccm.contract.getId());
                if (cms == null) {
                    cms = new ArrayList<>();
                    consumerMatches.put(tenant.getId(), ccm.contract.getId(), cms);
                }
                cms.add(ccm);
            }
        }

        // Match provider selectors, and check each match for a corresponding
        // consumer selector match.
        for (EndpointGroup group : tenant.getEndpointGroup()) {
            List<ContractMatch> matches = matchProviderContracts(tenant, group, consumerMatches);
            for (ContractMatch cm : matches) {
                EgKey consumerKey = new EgKey(cm.consumerTenant.getId(), cm.consumer.getId());
                EgKey providerKey = new EgKey(cm.providerTenant.getId(), cm.provider.getId());
                List<ContractMatch> egPairMatches = contractMatches.get(consumerKey, providerKey);
                if (egPairMatches == null) {
                    egPairMatches = new ArrayList<>();
                    contractMatches.put(consumerKey, providerKey, egPairMatches);
                }

                egPairMatches.add(cm);
            }
        }
    }

    private static List<ConsumerContractMatch> matchConsumerContracts(Tenant tenant,
            EndpointGroup consumer) {
        List<ConsumerContractMatch> matches = new ArrayList<>();
        if (consumer.getConsumerNamedSelector() != null) {
            for (ConsumerNamedSelector cns : consumer.getConsumerNamedSelector()) {
                if (cns.getContract() == null)
                    continue;
                for (ContractId contractId : cns.getContract()) {
                    Contract contract =
                            TenantUtils.findContract(tenant, contractId);
                    if (contract == null)
                        continue;
                    matches.add(new ConsumerContractMatch(tenant, contract,
                            tenant, consumer,
                            cns));
                }
            }
        }
        if (consumer.getConsumerTargetSelector() != null) {
            for (ConsumerTargetSelector cts : consumer.getConsumerTargetSelector()) {
                if (tenant.getContract() == null)
                    continue;
                for (Contract contract : tenant.getContract()) {
                    if (contract.getTarget() == null)
                        continue;
                    for (Target t : contract.getTarget()) {
                        boolean match = true;
                        if (cts.getQualityMatcher() != null) {
                            for (QualityMatcher m : cts.getQualityMatcher()) {
                                if (!MatcherUtils.applyQualityMatcher(m, t)) {
                                    match = false;
                                    break;
                                }
                            }
                        }
                        if (match) {
                            matches.add(new ConsumerContractMatch(tenant,
                                    contract,
                                    tenant,
                                    consumer,
                                    cts));
                        }
                    }
                }
            }
        }
        // TODO match selectors also against contract references
        // for (ConsumerTargetSelector cts :
        // consumer.getConsumerTargetSelector()) {
        // if (tenant.getContractRef() == null) continue;
        // for (ContractRef c : tenant.getContractRef()) {
        //
        // }
        // }
        return matches;
    }

    private static List<ContractMatch> matchProviderContracts(Tenant tenant, EndpointGroup provider,
            Table<TenantId, ContractId, List<ConsumerContractMatch>> consumerMatches) {
        List<ContractMatch> matches = new ArrayList<>();
        if (provider.getProviderNamedSelector() != null) {
            for (ProviderNamedSelector pns : provider.getProviderNamedSelector()) {
                if (pns.getContract() == null)
                    continue;
                for (ContractId contractId : pns.getContract()) {
                    Contract c = TenantUtils.findContract(tenant, contractId);
                    if (c == null)
                        continue;
                    List<ConsumerContractMatch> cMatches = consumerMatches.get(tenant.getId(), c.getId());
                    amendContractMatches(matches, cMatches, tenant, provider, pns);
                }
            }
        }
        if (provider.getProviderTargetSelector() != null) {
            for (ProviderTargetSelector pts : provider.getProviderTargetSelector()) {
                if (tenant.getContract() == null)
                    continue;
                for (Contract c : tenant.getContract()) {
                    if (c.getTarget() == null)
                        continue;
                    for (Target t : c.getTarget()) {
                        boolean match = true;
                        if (pts.getQualityMatcher() != null) {
                            for (QualityMatcher m : pts.getQualityMatcher()) {
                                if (!MatcherUtils.applyQualityMatcher(m, t)) {
                                    match = false;
                                    break;
                                }
                            }
                        }
                        if (match) {
                            List<ConsumerContractMatch> cMatches = consumerMatches.get(tenant.getId(), c.getId());
                            amendContractMatches(matches, cMatches, tenant, provider, pts);

                        }
                    }
                }
            }
        }
        return matches;
    }

    private static void amendContractMatches(List<ContractMatch> matches,
            List<ConsumerContractMatch> cMatches,
            Tenant tenant, EndpointGroup provider,
            ProviderSelectionRelator relator) {
        if (cMatches == null)
            return;
        for (ConsumerContractMatch cMatch : cMatches) {
            matches.add(new ContractMatch(cMatch, tenant, provider, relator));
        }
    }

    /**
     * Represents a selected contract made by endpoint groups matching it using
     * selection relators. This is the result of the contract selection phase.
     *
     * @author readams
     */
    @Immutable
    protected static class ContractMatch extends ConsumerContractMatch {

        /**
         * The tenant ID of the provider endpoint group
         */
        final Tenant providerTenant;

        /**
         * The provider endpoint group
         */
        final EndpointGroup provider;

        /**
         * The provider selection relator that was used to match the contract
         */
        final ProviderSelectionRelator providerRelator;

        public ContractMatch(ConsumerContractMatch consumerMatch, Tenant providerTenant, EndpointGroup provider,
                ProviderSelectionRelator providerRelator) {
            super(consumerMatch.contractTenant, consumerMatch.contract, consumerMatch.consumerTenant,
                    consumerMatch.consumer, consumerMatch.consumerRelator);
            this.providerTenant = providerTenant;
            this.provider = provider;
            this.providerRelator = providerRelator;
        }
    }

    @Immutable
    protected static class ConsumerContractMatch {

        /**
         * The tenant of the matching contract
         */
        final Tenant contractTenant;

        /**
         * The matching contract
         */
        final Contract contract;

        /**
         * The tenant for the endpoint group
         */
        final Tenant consumerTenant;

        /**
         * The consumer endpoint group
         */
        final EndpointGroup consumer;

        /**
         * The consumer selection relator that was used to match the contract
         */
        final ConsumerSelectionRelator consumerRelator;

        public ConsumerContractMatch(Tenant contractTenant, Contract contract, Tenant consumerTenant,
                EndpointGroup consumer, ConsumerSelectionRelator consumerRelator) {
            super();
            this.contractTenant = contractTenant;
            this.contract = contract;
            this.consumerTenant = consumerTenant;
            this.consumer = consumer;
            this.consumerRelator = consumerRelator;
        }
    }

}
