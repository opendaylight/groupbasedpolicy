/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.child.endpoints.ChildEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.RelativeLocationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.relative.location.relative.locations.ExternalLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.ParentEndpointChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.ParentEndpointCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.parent.child.endpoints.parent.endpoint.choice.parent.endpoint._case.ParentEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint_location_provider.rev160419.location.providers.location.provider.ProviderAddressEndpointLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev170511.MacAddressType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.vpp_renderer.rev160425.config.VppEndpointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class VppLocationUtils {

    private VppLocationUtils() {}

    static ProviderAddressEndpointLocationKey locationProviderKey(AddressEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    static ProviderAddressEndpointLocationKey providerLocationKey(AddressEndpointWithLocationKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    static ProviderAddressEndpointLocationKey providerLocationKey(ParentEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    static ProviderAddressEndpointLocationKey providerLocationKey(ChildEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    static VppEndpointKey vppEndpointKey(AddressEndpointKey key) {
        return new VppEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    static VppEndpointKey vppEndpointKey(ParentEndpointKey key) {
        return new VppEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    static VppEndpointKey vppEndpointKey(ChildEndpointKey key) {
        return new VppEndpointKey(key.getAddress(), key.getAddressType(), key.getContextId(), key.getContextType());
    }

    public static @Nonnull List<ChildEndpoint> getL2ChildEndpoints(@Nonnull AddressEndpoint addrEp) {
        if (addrEp.getChildEndpoint() == null) {
            return Collections.emptyList();
        }
        return addrEp.getChildEndpoint()
            .stream()
            .filter(child -> child.getAddressType().equals(MacAddressType.class))
            .collect(Collectors.toList());
    }

    static boolean validateEndpoint(AddressEndpoint addrEp) {
        return addrEp.getAddressType().equals(IpPrefixType.class) && addrEp.getChildEndpoint() != null
                && !addrEp.getChildEndpoint().isEmpty();
    }

    static AbsoluteLocation createAbsLocation(VppEndpoint vppEndpoint) {
        InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(vppEndpoint.getVppNodeId());
        String restIfacePath = VppPathMapper.interfaceToRestPath(vppEndpoint.getVppInterfaceName());
        return new AbsoluteLocationBuilder().setLocationType(new ExternalLocationCaseBuilder()
            .setExternalNodeMountPoint(vppNodeIid).setExternalNodeConnector(restIfacePath).build()).build();
    }

    static RelativeLocations createRelLocations(List<VppEndpoint> vppEndpoints) {
        List<ExternalLocation> extLocations = vppEndpoints.stream().map(vppEndpoint -> {
            InstanceIdentifier<Node> vppNodeIid = VppIidFactory.getNetconfNodeIid(vppEndpoint.getVppNodeId());
            String restIfacePath = VppPathMapper.interfaceToRestPath(vppEndpoint.getVppInterfaceName());
            return new ExternalLocationBuilder().setExternalNodeMountPoint(vppNodeIid)
                .setExternalNodeConnector(restIfacePath)
                .build();
        }).collect(Collectors.toList());
        return new RelativeLocationsBuilder().setExternalLocation(extLocations).build();
    }

    static RelativeLocations createRelativeAddressEndpointLocation(@Nonnull AddressEndpointKey addrEp,
            @Nonnull Map<NodeId, String> publicIntfNamesByNodes) {
        return new RelativeLocationsBuilder()
            .setExternalLocation(
                    publicIntfNamesByNodes.keySet()
                        .stream()
                        .filter(nodeId -> publicIntfNamesByNodes.get(nodeId) != null)
                        .map(nodeId -> new ExternalLocationBuilder()
                            .setExternalNodeMountPoint(VppIidFactory.getNetconfNodeIid(nodeId))
                            .setExternalNodeConnector(
                                    VppPathMapper.interfaceToRestPath(publicIntfNamesByNodes.get(nodeId)))
                            .build())
                        .collect(Collectors.toList()))
            .build();
    }

    static ProviderAddressEndpointLocation createLocation(AddressEndpointKey addrEpKey,
            AbsoluteLocation absoluteLocation) {
        return new ProviderAddressEndpointLocationBuilder().setKey(createProviderAddressEndpointLocationKey(addrEpKey))
            .setAbsoluteLocation(absoluteLocation)
            .build();
    }

    static ProviderAddressEndpointLocation createLocation(AddressEndpointKey key, RelativeLocations relativeLocations) {
        return new ProviderAddressEndpointLocationBuilder().setRelativeLocations(relativeLocations)
            .setKey(createProviderAddressEndpointLocationKey(key))
            .build();
    }

    public static ProviderAddressEndpointLocationKey createProviderAddressEndpointLocationKey(AddressEndpointKey key) {
        return new ProviderAddressEndpointLocationKey(key.getAddress(), key.getAddressType(), key.getContextId(),
                key.getContextType());
    }

    public static ProviderAddressEndpointLocationKey createProviderAddressEndpointLocationKey(VppEndpoint vpp) {
        return new ProviderAddressEndpointLocationKey(vpp.getAddress(), vpp.getAddressType(), vpp.getContextId(),
                vpp.getContextType());
    }

    static boolean hasMultihomeParent(ReadTransaction rTx, @Nullable ParentEndpointChoice parentChoice) {
        if (parentChoice == null || ((ParentEndpointCase) parentChoice).getParentEndpoint() == null) {
            return false;
        }
        ParentEndpointCase parents = (ParentEndpointCase) parentChoice;
        return parents.getParentEndpoint().stream().map(parent -> parent.getKey()).anyMatch(key -> {
            AddressEndpointKey addrEpKey = new AddressEndpointKey(key.getAddress(), key.getAddressType(),
                    key.getContextId(), key.getContextType());
            Optional<AddressEndpoint> addressEndpoint = DataStoreHelper.readFromDs(LogicalDatastoreType.OPERATIONAL,
                    IidFactory.addressEndpointIid(addrEpKey), rTx);
            return (addressEndpoint.isPresent() && addressEndpoint.get().getChildEndpoint().size() > 1);
        });
    }
}
