package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


public class NeutronFloatingIpAware implements INeutronFloatingIPAware {

    public static final Logger LOG = LoggerFactory.getLogger(NeutronFloatingIpAware.class);
    private final DataBroker dataProvider;

    public NeutronFloatingIpAware(DataBroker dataProvider) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
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
        return StatusCode.OK;
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
