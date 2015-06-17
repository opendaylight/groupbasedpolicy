package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.rule;

import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class SecRuleDao {

    private final SetMultimap<EndpointGroupId, NeutronSecurityRule> secRulesByOwnerSecGrpId = HashMultimap.create();
    private final SetMultimap<OwnerAndRemoteOfSecRule, NeutronSecurityRule> secRulesByRemoteSecGrpId =
            HashMultimap.create();

    public void addSecRule(NeutronSecurityRule secRule) {
        Preconditions.checkNotNull(secRule);
        EndpointGroupId ownerSecGrp = SecRuleEntityDecoder.getProviderEpgId(secRule);
        EndpointGroupId remoteSecGrp = SecRuleEntityDecoder.getConsumerEpgId(secRule);
        secRulesByOwnerSecGrpId.put(ownerSecGrp, secRule);
        secRulesByRemoteSecGrpId.put(new OwnerAndRemoteOfSecRule(ownerSecGrp, remoteSecGrp), secRule);
    }

    public Set<NeutronSecurityRule> getSecRulesByOwnerSecGrpId(EndpointGroupId secGrpId) {
        return secRulesByOwnerSecGrpId.get(secGrpId);
    }

    public Set<NeutronSecurityRule> getSecRulesBySecGrpIdAndRemoteSecGrpId(EndpointGroupId ownerSecGrpId,
            @Nullable EndpointGroupId remoteSecGrpId) {
        return secRulesByRemoteSecGrpId.get(new OwnerAndRemoteOfSecRule(ownerSecGrpId, remoteSecGrpId));
    }

    public Set<NeutronSecurityRule> getSecRulesWithoutRemoteSecGrpBySecGrpId(EndpointGroupId ownerSecGrpId) {
        return secRulesByRemoteSecGrpId.get(new OwnerAndRemoteOfSecRule(ownerSecGrpId, null));
    }

    public Set<EndpointGroupId> getAllOwnerSecGrps() {
        return secRulesByOwnerSecGrpId.keySet();
    }

    public void removeSecRule(NeutronSecurityRule secRule) {
        Preconditions.checkNotNull(secRule);
        EndpointGroupId ownerSecGrp = SecRuleEntityDecoder.getProviderEpgId(secRule);
        EndpointGroupId remoteSecGrp = SecRuleEntityDecoder.getConsumerEpgId(secRule);
        secRulesByOwnerSecGrpId.remove(ownerSecGrp, secRule);
        secRulesByRemoteSecGrpId.remove(new OwnerAndRemoteOfSecRule(ownerSecGrp, remoteSecGrp), secRule);
    }

    static class OwnerAndRemoteOfSecRule {

        private final EndpointGroupId owner;
        private final EndpointGroupId remote;

        private OwnerAndRemoteOfSecRule(EndpointGroupId owner, EndpointGroupId remote) {
            this.owner = Preconditions.checkNotNull(owner);
            this.remote = remote;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((owner == null) ? 0 : owner.hashCode());
            result = prime * result + ((remote == null) ? 0 : remote.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            OwnerAndRemoteOfSecRule other = (OwnerAndRemoteOfSecRule) obj;
            if (owner == null) {
                if (other.owner != null)
                    return false;
            } else if (!owner.equals(other.owner))
                return false;
            if (remote == null) {
                if (other.remote != null)
                    return false;
            } else if (!remote.equals(other.remote))
                return false;
            return true;
        }

    }

}
