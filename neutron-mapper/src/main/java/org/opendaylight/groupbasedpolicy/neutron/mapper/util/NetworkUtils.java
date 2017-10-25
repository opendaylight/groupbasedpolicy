/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.mapper.util;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.ext.rev150712.NetworkL3Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev150712.NetworkProviderExtension;

public class NetworkUtils {

    public static List<Network> findRouterExternalNetworks(@Nullable Networks networks) {
        if (networks == null || networks.getNetwork() == null) {
            return Collections.emptyList();
        }
        List<Network> result = new ArrayList<>();
        for (Network network : networks.getNetwork()) {
            if (isRouterExternal(network)) {
                result.add(network);
            }
        }
        return result;
    }

    public static Optional<Network> findNetwork(Uuid uuid, @Nullable Networks networks) {
        if (networks == null || networks.getNetwork() == null) {
            return Optional.absent();
        }
        for (Network network : networks.getNetwork()) {
            if (network.getUuid().equals(uuid)) {
                return Optional.of(network);
            }
        }
        return Optional.absent();
    }

    public static boolean isTenantNetwork(Network network) {
        return getPhysicalNetwork(network).isEmpty();
    }

    public static boolean isProviderPhysicalNetwork(Network network) {
        return (!isRouterExternal(network) && !getPhysicalNetwork(network).isEmpty());
    }

    public static boolean isRouterExternal(Network network) {
        NetworkL3Extension l3Extension = network.getAugmentation(NetworkL3Extension.class);
        if (l3Extension == null) {
            return false;
        }
        Boolean external = l3Extension.isExternal();
        if (external == null) {
            return false;
        }
        return external;
    }

    public static @Nonnull String getPhysicalNetwork(Network network) {
        NetworkProviderExtension providerExtension = network.getAugmentation(NetworkProviderExtension.class);
        if (providerExtension == null) {
            return "";
        }
        return Strings.nullToEmpty(providerExtension.getPhysicalNetwork());
    }

    public static @Nonnull String getSegmentationId(Network network) {
        NetworkProviderExtension providerExtension = network.getAugmentation(NetworkProviderExtension.class);
        if (providerExtension == null) {
            return "";
        }
        return Strings.nullToEmpty(providerExtension.getSegmentationId());
    }
}
