package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.mapping.NeutronPortAware;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3Context;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstanceBuilder;

import com.google.common.base.Optional;

public final class MappingUtils {

    public static final String NEUTRON_RULE__ = "neutron_rule__";
    public static final String NEUTRON_NETWORK__ = "neutron_network__";
    public static final String NEUTRON_ROUTER__ = "neutron_router__";
    public static final String NEUTRON_EXTERNAL__ = "neutron_external_network__";
    public static final String NEUTRON_GROUP__ = "neutron_group__";
    public static final ActionInstance ACTION_ALLOW = new ActionInstanceBuilder().setName(
            new ActionName(NEUTRON_RULE__ + "allow"))
        .setActionDefinitionId(AllowAction.DEFINITION.getId())
        .build();
    public static final EndpointGroupId EPG_ANY_ID = new EndpointGroupId("aaaec0ce-dd5a-11e4-b9d6-1681e6b88ec1");
    public static final EndpointGroupId EPG_DHCP_ID = new EndpointGroupId("ddd6cfe6-dfe5-11e4-8a00-1681e6b88ec1");
    public static final EndpointGroupId EPG_ROUTER_ID = new EndpointGroupId("1118172e-cd84-4933-a35f-749f9a651de9");
    public static final EndpointGroupId EPG_EXTERNAL_ID = new EndpointGroupId("eeeaa3a2-e9ba-44e0-a462-bea923d30e38");

    private MappingUtils() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    public static ForwardingCtx createForwardingContext(TenantId tenantId, L2FloodDomainId l2FdId, ReadTransaction rTx) {
        Optional<L2FloodDomain> potentialL2Fd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2FloodDomainIid(tenantId, l2FdId), rTx);
        if (!potentialL2Fd.isPresent()) {
            return new ForwardingCtx(null, null, null);
        }
        L2BridgeDomainId l2BdId = potentialL2Fd.get().getParent();
        if (l2BdId == null) {
            return new ForwardingCtx(potentialL2Fd.get(), null, null);
        }
        Optional<L2BridgeDomain> potentialL2Bd = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l2BridgeDomainIid(tenantId, l2BdId), rTx);
        if (!potentialL2Bd.isPresent()) {
            return new ForwardingCtx(potentialL2Fd.get(), null, null);
        }
        L3ContextId l3ContextId = potentialL2Bd.get().getParent();
        if (l3ContextId == null) {
            return new ForwardingCtx(potentialL2Fd.get(), potentialL2Bd.get(), null);
        }
        Optional<L3Context> potentialL3Context = DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                IidFactory.l3ContextIid(tenantId, l3ContextId), rTx);
        if (!potentialL3Context.isPresent()) {
            return new ForwardingCtx(potentialL2Fd.get(), potentialL2Bd.get(), null);
        }
        return new ForwardingCtx(potentialL2Fd.get(), potentialL2Bd.get(), potentialL3Context.get());
    }

    public static final class ForwardingCtx {

        private final L2FloodDomain l2FloodDomain;
        private final L2BridgeDomain l2BridgeDomain;
        private final L3Context l3Context;

        private ForwardingCtx(L2FloodDomain l2Fd, L2BridgeDomain l2Bd, L3Context l3Context) {
            this.l2FloodDomain = l2Fd;
            this.l2BridgeDomain = l2Bd;
            this.l3Context = l3Context;
        }

        public L2FloodDomain getL2FloodDomain() {
            return l2FloodDomain;
        }

        public L2BridgeDomain getL2BridgeDomain() {
            return l2BridgeDomain;
        }

        public L3Context getL3Context() {
            return l3Context;
        }

    }

    public static Neutron_IPs getFirstIp(List<Neutron_IPs> fixedIPs) {
        if (fixedIPs == null || fixedIPs.isEmpty()) {
            return null;
        }
        Neutron_IPs neutron_Ip = fixedIPs.get(0);
        if (fixedIPs.size() > 1) {
            NeutronPortAware.LOG.warn("Neutron mapper does not support multiple IPs on the same port. Only first IP is selected {}",
                    neutron_Ip);
        }
        return neutron_Ip;
    }
}
