package org.opendaylight.groupbasedpolicy.resolver;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;

/**
 * A tuple referencing an endpoint group and its enclosing tenant
 * @author readams
 */
@Immutable
public class EgKey {
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