/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.util;

import java.util.List;

import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.SocketInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.Hostconfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.hostconfig.attributes.hostconfigs.HostconfigBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class HostconfigUtil {

    public static final String L2_HOST_TYPE = "ODL L2";
    private static final String VHOST_USER = "vhostuser";
    private static final String VNIC_TYPE = "normal";
    private static final String HAS_DATAPATH_TYPE_NETDEV = "False";
    private static final String SUPPORT_VHOST_USER = "True";
    private static final String VHOSTUSER_MODE = "server";
    private static final List<String> supportedNetworkTypes = Lists.newArrayList("local", "vlan", "vxlan", "gre");

    public static Hostconfig createHostconfigsDataFor(NodeId nodeId, SocketInfo socketInfo) {
        Preconditions.checkNotNull(nodeId);
        Preconditions.checkNotNull(socketInfo);
        JsonObject odlL2 = new JsonObject();
        odlL2.add("allowed_network_types", buildSupportedNetworkTypes());
        odlL2.add("supported_vnic_types", buildSupportedVnicTypes(socketInfo));
        return new HostconfigBuilder().setHostId(nodeId.getValue())
            .setHostType(L2_HOST_TYPE)
            .setConfig(odlL2.toString())
            .build();
    }

    private static JsonArray buildSupportedNetworkTypes() {
        JsonArray networkTypes = new JsonArray();
        supportedNetworkTypes.forEach(networkTypes::add);
        return networkTypes;
    }

    private static JsonArray buildSupportedVnicTypes(SocketInfo socketInfo) {
        JsonArray supportedVnicTypes = new JsonArray();
        JsonObject supportedVnicType = new JsonObject();
        supportedVnicType.addProperty("vnic_type", VNIC_TYPE);
        supportedVnicType.addProperty("vif_type", VHOST_USER);
        supportedVnicType.add("vif_details", buildVifDetails(socketInfo));
        supportedVnicTypes.add(supportedVnicType);
        return supportedVnicTypes;
    }

    private static JsonObject buildVifDetails(SocketInfo socketInfo) {
        JsonObject vifDetails = new JsonObject();
        vifDetails.addProperty("has_datapath_type_netdev", HAS_DATAPATH_TYPE_NETDEV);
        vifDetails.addProperty("support_vhost_user", SUPPORT_VHOST_USER);
        vifDetails.addProperty("port_prefix", socketInfo.getSocketPrefix());
        vifDetails.addProperty("vhostuser_socket_dir", socketInfo.getSocketPath());
        vifDetails.addProperty("vhostuser_mode", VHOSTUSER_MODE);
        vifDetails.addProperty("vhostuser_socket", socketInfo.getVhostUserSocket());
        return vifDetails;
    }
}
