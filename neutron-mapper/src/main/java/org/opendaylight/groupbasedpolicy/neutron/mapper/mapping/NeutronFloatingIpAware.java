package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.IidFactory;
import org.opendaylight.groupbasedpolicy.neutron.mapper.util.Utils;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.NeutronCRUDInterfaces;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.UniqueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.floating.ip.ports.by.internal.ports.FloatingIpPortByInternalPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.floating.ip.association.mappings.internal.ports.by.floating.ip.ports.InternalPortByFloatingIpPortBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class NeutronFloatingIpAware implements INeutronFloatingIPAware {

    public static final Logger LOG = LoggerFactory.getLogger(NeutronFloatingIpAware.class);
    private final DataBroker dataProvider;
    private final EndpointService epService;

    public NeutronFloatingIpAware(DataBroker dataProvider, EndpointService epService) {
        this.dataProvider = checkNotNull(dataProvider);
        this.epService = checkNotNull(epService);
    }

    @Override
    public int canCreateFloatingIP(NeutronFloatingIP floatingIP) {
        LOG.trace("canCreateFloatingIP - {}", floatingIP);
        return StatusCode.OK;
    }

    @Override
    public void neutronFloatingIPCreated(NeutronFloatingIP floatingIP) {
        LOG.trace("neutronFloatingIPCreated - {}", floatingIP);
    }

    @Override
    public int canUpdateFloatingIP(NeutronFloatingIP delta, NeutronFloatingIP original) {
        LOG.trace("canUpdateFloatingIP - delta: {} original: {}", delta, original);
        // floating IP UUID is same as device ID of a port representing floating IP
        UniqueId floatingIpPortId = NeutronPortAware.getFloatingIpPortIdByDeviceId(original.getFloatingIPUUID());
        if (floatingIpPortId == null) {
            LOG.warn("Illegal state - Port representing floating ip where floating IP uuid is {} does not exist.",
                    original.getFloatingIPUUID());
            return StatusCode.INTERNAL_SERVER_ERROR;
        }

        ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
        String oldFixedIPAddress = Strings.nullToEmpty(original.getFixedIPAddress());
        String oldPortUUID = Strings.nullToEmpty(original.getPortUUID());
        String newFixedIPAddress = Strings.nullToEmpty(delta.getFixedIPAddress());
        String newPortUUID = Strings.nullToEmpty(delta.getPortUUID());
        if (oldFixedIPAddress.equals(newFixedIPAddress) && oldPortUUID.equals(newPortUUID)) {
            // interesting fields were not changed
            return StatusCode.OK;
        }

        if ((!oldFixedIPAddress.isEmpty() && newFixedIPAddress.isEmpty())
                || (!oldPortUUID.isEmpty() && newPortUUID.isEmpty())) {
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.internalPortByFloatingIpPortIid(floatingIpPortId), rwTx);
            DataStoreHelper.removeIfExists(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.floatingIpPortByInternalPortIid(new UniqueId(oldPortUUID)), rwTx);
            // TODO unregister EP representing floating ip port
        } else if (!newFixedIPAddress.isEmpty() && !newPortUUID.isEmpty()) {
            // workaround for https://bugs.opendaylight.org/show_bug.cgi?id=3368
            // otherwise we will create port representing floating IP in NeutronPortAware
            Integer errorCode = registerFloatingIpPort(original.getTenantUUID(), floatingIpPortId.getValue(), rwTx);
            if (errorCode != null) {
                rwTx.cancel();
                return errorCode;
            }

            UniqueId internalPortId = new UniqueId(newPortUUID);
            InternalPortByFloatingIpPort internalPortByFloatingIpPort = new InternalPortByFloatingIpPortBuilder().setFloatingIpPortId(
                    floatingIpPortId)
                .setFloatingIpPortIpAddress(Utils.createIpAddress(original.getFloatingIPAddress()))
                .setInternalPortId(internalPortId)
                .setInternalPortIpAddress(Utils.createIpAddress(newFixedIPAddress))
                .build();
            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.internalPortByFloatingIpPortIid(floatingIpPortId),
                    internalPortByFloatingIpPort, true);
            rwTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.floatingIpPortByInternalPortIid(internalPortId),
                    new FloatingIpPortByInternalPortBuilder(internalPortByFloatingIpPort).build(), true);
        }
        boolean isSubmitToDsSuccessful = DataStoreHelper.submitToDs(rwTx);
        if (!isSubmitToDsSuccessful) {
            return StatusCode.INTERNAL_SERVER_ERROR;
        }

        return StatusCode.OK;
    }

    private Integer registerFloatingIpPort(String tenantUUID, String floatingIpPortUUID, ReadWriteTransaction rwTx) {
        INeutronPortCRUD portInterface = NeutronCRUDInterfaces.getINeutronPortCRUD(this);
        if (portInterface == null) {
            LOG.warn("Illegal state - No provider for {}", INeutronPortCRUD.class.getName());
            return StatusCode.INTERNAL_SERVER_ERROR;
        }
        NeutronPort floatingIpPort = portInterface.getPort(floatingIpPortUUID);
        // TenantId tenantId = new TenantId(Utils.normalizeUuid());
        floatingIpPort.setTenantID(tenantUUID);
        boolean isNeutronPortCreated = NeutronPortAware.addNeutronPort(floatingIpPort, rwTx, epService);
        if (!isNeutronPortCreated) {
            rwTx.cancel();
            return StatusCode.INTERNAL_SERVER_ERROR;
        }
        return null;
    }

    @Override
    public void neutronFloatingIPUpdated(NeutronFloatingIP floatingIP) {
        LOG.trace("neutronFloatingIPUpdated - {}", floatingIP);
    }

    @Override
    public int canDeleteFloatingIP(NeutronFloatingIP floatingIP) {
        LOG.trace("canDeleteFloatingIP - {}", floatingIP);
        return StatusCode.OK;
    }

    @Override
    public void neutronFloatingIPDeleted(NeutronFloatingIP floatingIP) {
        LOG.trace("neutronFloatingIPDeleted - {}", floatingIP);
    }

}
