package org.opendaylight.groupbasedpolicy.resolver;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * A tuple referencing an endpoint group and its enclosing tenant
 * @author readams
 */
@Immutable
public class EgKey implements Comparable<EgKey> {
    private final TenantId tenantId;
    private final EndpointGroupId egId;
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((egId == null) ? 0 : egId.hashCode());
        result = prime * result +
                 ((tenantId == null) ? 0 : tenantId.hashCode());
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
        EgKey other = (EgKey) obj;
        if (egId == null) {
            if (other.egId != null)
                return false;
        } else if (!egId.equals(other.egId))
            return false;
        if (tenantId == null) {
            if (other.tenantId != null)
                return false;
        } else if (!tenantId.equals(other.tenantId))
            return false;
        return true;
    }
    public EgKey(TenantId tenantId, EndpointGroupId egId) {
        super();
        this.tenantId = tenantId;
        this.egId = egId;
    }

    @Override
    public int compareTo(EgKey o) {
        String tid = null;
        if (tenantId != null) tid = tenantId.getValue();
        String otid = null;
        if (o.tenantId != null) otid = o.tenantId.getValue();
        String egid = null;
        if (egId != null) tid = egId.getValue();
        String oegid = null;
        if (o.egId != null) oegid = o.egId.getValue();
        return ComparisonChain.start()
            .compare(tid, otid, 
                     Ordering.natural().nullsLast())
            .compare(egid, oegid, 
                     Ordering.natural().nullsLast())
            .result();
    }

    @Override
    public String toString() {
        return "EgKey [tenantId=" + tenantId + ", egId=" + egId + "]";
    }
    public TenantId getTenantId() {
        return tenantId;
    }
    public EndpointGroupId getEgId() {
        return egId;
    }
}