/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.iface;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.renderer.util.AddressEndpointUtils;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.PolicyContext;
import org.opendaylight.groupbasedpolicy.renderer.vpp.policy.acl.AccessListUtil;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.MountedDataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.renderer.endpoint.PeerEndpointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class AclManager {

    private static final Logger LOG = LoggerFactory.getLogger(AclManager.class);
    private final MountedDataBrokerProvider mountDataProvider;

    public AclManager(@Nonnull MountedDataBrokerProvider mountDataProvider) {
        this.mountDataProvider = Preconditions.checkNotNull(mountDataProvider);
    }

    public void updateAclsForPeers(PolicyContext policyCtx, RendererEndpointKey rEpKey) {
        ImmutableSet<PeerEndpointKey> peers = policyCtx.getPolicyTable().row(rEpKey).keySet();
        for (RendererEndpointKey peerRendEp : peers.stream()
            .map(AddressEndpointUtils::fromPeerEpKey)
            .collect(Collectors.toList())
            .stream()
            .map(AddressEndpointUtils::toRendererEpKey)
            .collect(Collectors.toList())) {
            updateAclsForRendEp(peerRendEp, policyCtx);
        }
    }

    public void updateAclsForRendEp(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        LOG.info("Updating policy for endpoint {}", rEpKey);
        AddressEndpointWithLocation peerAddrEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase epLoc;
        try {
            epLoc = InterfaceManager.resolveAndValidateLocation(peerAddrEp);
        } catch (NullPointerException | IllegalArgumentException e) {
            //TODO investigate, don't just move on.
            LOG.warn("Peer {} has no location. Moving on...", peerAddrEp, e.getMessage());
            return;
        }
        InstanceIdentifier<?> vppNodeIid = epLoc.getExternalNodeMountPoint();
        Optional<InstanceIdentifier<Interface>> optInterfaceIid =
                VppPathMapper.interfaceToInstanceIdentifier(epLoc.getExternalNodeConnector());
        if (!optInterfaceIid.isPresent()) {
            LOG.warn("Cannot  find interface for endpoint {}. ACLs for endpoint not updated {}. ", rEpKey);
            return;
        }
        Optional<DataBroker> optMountPoint = mountDataProvider.getDataBrokerForMountPoint(vppNodeIid);
        AccessListUtil.resolveAclsOnInterface(rEpKey, policyCtx).forEach(aclWrapper -> aclWrapper
            .writeAcl(optMountPoint.get(), optInterfaceIid.get().firstKeyOf(Interface.class)));
    }
}
