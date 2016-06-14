/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import javax.annotation.Nonnull;

import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.NodeOperEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.RendererPolicyConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.iface.InterfaceManager;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.NetworkContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.Containment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.ForwardingContextContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.common.endpoint.fields.network.containment.containment.NetworkDomainContainment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.LocationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class VppRendererPolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(VppRendererPolicyManager.class);
    private final InterfaceManager ifaceManager;
    private final DataBroker dataProvider;

    public VppRendererPolicyManager(@Nonnull InterfaceManager ifaceManager, @Nonnull DataBroker dataProvider) {
        this.ifaceManager = Preconditions.checkNotNull(ifaceManager);
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
    }

    @Subscribe
    public void rendererPolicyChanged(RendererPolicyConfEvent event) {
        RendererPolicyBuilder responseBulder = new RendererPolicyBuilder();
        switch (event.getDtoModificationType()) {
            case CREATED:
                responseBulder.setVersion(event.getAfter().get().getVersion());
                rendererPolicyCreated(event.getAfter().get());
                break;
            case UPDATED:
                RendererPolicy rPolicyBefore = event.getBefore().get();
                RendererPolicy rPolicyAfter = event.getAfter().get();
                responseBulder.setVersion(rPolicyAfter.getVersion());
                if (!isConfigurationChanged(rPolicyBefore, rPolicyAfter)) {
                    LOG.debug("Configuration is not changed only updating config version from {} to {}",
                            rPolicyBefore.getVersion(), rPolicyAfter.getVersion());
                } else {
                    // TODO collect unconfigured rules and put them to responseBuilder
                    rendererPolicyUpdated(rPolicyBefore, rPolicyAfter);
                }
                break;
            case DELETED:
                responseBulder.setVersion(event.getBefore().get().getVersion());
                rendererPolicyDeleted(event.getBefore().get());
                break;
        }
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        RendererPolicy response = responseBulder.build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.rendererIid(VppRenderer.NAME).child(RendererPolicy.class),
                response, true);
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Renderer updated renderer policy {}", response);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Renderer DIDN'T update renderer-policy {}", response);
            }
        });
    }

    private boolean isConfigurationChanged(RendererPolicy before, RendererPolicy after) {
        if (before.getConfiguration() == null && after.getConfiguration() == null) {
            return false;
        }
        return true;
    }

    private void rendererPolicyUpdated(RendererPolicy rPolicyBefore, RendererPolicy rPolicyAfter) {
        PolicyContext policyCtxBefore = new PolicyContext(rPolicyBefore);
        PolicyContext policyCtxAfter = new PolicyContext(rPolicyAfter);
        ImmutableSet<RendererEndpointKey> rendEpsBefore = policyCtxBefore.getPolicyTable().rowKeySet();
        ImmutableSet<RendererEndpointKey> rendEpsAfter = policyCtxAfter.getPolicyTable().rowKeySet();

        SetView<RendererEndpointKey> removedRendEps = Sets.difference(rendEpsBefore, rendEpsAfter);
        removedRendEps.forEach(rEpKey -> rendererEndpointDeleted(rEpKey, policyCtxBefore));

        SetView<RendererEndpointKey> createdRendEps = Sets.difference(rendEpsAfter, rendEpsBefore);
        createdRendEps.forEach(rEpKey -> rendererEndpointCreated(rEpKey, policyCtxAfter));

        SetView<RendererEndpointKey> updatedRendEps = Sets.intersection(rendEpsBefore, rendEpsAfter);
        // TODO think about all cases, but keep it simple for now
        updatedRendEps.forEach(rEpKey -> rendererEndpointDeleted(rEpKey, policyCtxBefore));
        updatedRendEps.forEach(rEpKey -> rendererEndpointCreated(rEpKey, policyCtxAfter));
    }

    private void rendererPolicyCreated(RendererPolicy rPolicy) {
        PolicyContext policyCtx = new PolicyContext(rPolicy);

        // TODO create topology for each L2FloodDomain in RendererForwarding

        policyCtx.getPolicyTable().rowKeySet().forEach(rEpKey -> rendererEndpointCreated(rEpKey, policyCtx));
    }

    private void rendererEndpointCreated(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        AddressEndpointWithLocation rEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase rEpLoc = resolveAndValidateLocation(rEp);
        if (Strings.isNullOrEmpty(rEpLoc.getExternalNodeConnector())) {
            // TODO add it to the status for renderer manager
            LOG.info("Rednerer endpoint does not have external-node-connector therefore it is ignored {}", rEp);
            return;
        }

        // TODO add rEpLoc.getExternalNodeMountPoint() to VBD as node

        if (Strings.isNullOrEmpty(rEpLoc.getExternalNode())) {
            String l2FloodDomain = resolveL2FloodDomain(rEp.getNetworkContainment());
            if (Strings.isNullOrEmpty(l2FloodDomain)) {
                // TODO add it to the status for renderer manager
                LOG.info("Rednerer endpoint does not have l2FloodDomain as network containment {}", rEp);
                return;
            }
            ListenableFuture<Void> futureAddBridgeDomainToInterface =
                    ifaceManager.addBridgeDomainToInterface(l2FloodDomain, rEp);
            Futures.addCallback(futureAddBridgeDomainToInterface, new FutureCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    LOG.debug("Interface added to bridge-domain {} for endpoint {}", l2FloodDomain, rEp);
                }

                @Override
                public void onFailure(Throwable t) {
                    // TODO add it to the status for renderer manager
                    LOG.warn("Interface was not added to bridge-domain {} for endpoint {}", l2FloodDomain, rEp, t);
                }
            });
        }
    }

    private void rendererPolicyDeleted(RendererPolicy rendererPolicy) {
        PolicyContext policyCtx = new PolicyContext(rendererPolicy);

        // TODO delete topology for each L2FloodDomain in RendererForwarding

        policyCtx.getPolicyTable().rowKeySet().forEach(rEpKey -> rendererEndpointDeleted(rEpKey, policyCtx));
    }

    private void rendererEndpointDeleted(RendererEndpointKey rEpKey, PolicyContext policyCtx) {
        AddressEndpointWithLocation rEp = policyCtx.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
        ExternalLocationCase rEpLoc = resolveAndValidateLocation(rEp);
        if (Strings.isNullOrEmpty(rEpLoc.getExternalNodeConnector())) {
            // nothing was created for endpoint therefore nothing is removed
            return;
        }

        // TODO remove rEpLoc.getExternalNodeMountPoint() to VBD as node

        if (!Strings.isNullOrEmpty(rEpLoc.getExternalNode())) {
            ListenableFuture<Void> futureAddBridgeDomainToInterface = ifaceManager.deleteBridgeDomainFromInterface(rEp);
            Futures.addCallback(futureAddBridgeDomainToInterface, new FutureCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    LOG.debug("bridge-domain was deleted from interface for endpoint {}", rEp);
                }

                @Override
                public void onFailure(Throwable t) {
                    // TODO add it to the status for renderer manager
                    LOG.warn("bridge-domain was not deleted from interface for endpoint {}", rEp, t);
                }
            });
        }
    }

    private static String resolveL2FloodDomain(NetworkContainment netCont) {
        if (netCont == null) {
            return null;
        }
        Containment containment = netCont.getContainment();
        if (containment instanceof ForwardingContextContainment) {
            ForwardingContextContainment fwCtxCont = (ForwardingContextContainment) containment;
            if (fwCtxCont.getContextType().isAssignableFrom(L2FloodDomain.class)) {
                return fwCtxCont.getContextId() == null ? null : fwCtxCont.getContextId().getValue();
            }
        }
        if (containment instanceof NetworkDomainContainment) {
            // TODO address missing impl
            LOG.info("Network domain containment in endpoint is not supported yet. {}", netCont);
            return null;
        }
        return null;
    }

    private static ExternalLocationCase resolveAndValidateLocation(AddressEndpointWithLocation addrEpWithLoc) {
        LocationType locationType = addrEpWithLoc.getAbsoluteLocation().getLocationType();
        if (!(locationType instanceof ExternalLocationCase)) {
            throw new IllegalStateException("Endpoint does not have external location " + addrEpWithLoc);
        }
        ExternalLocationCase result = (ExternalLocationCase) locationType;
        if (result.getExternalNodeMountPoint() == null) {
            throw new IllegalStateException("Endpoint does not have external-node-mount-point " + addrEpWithLoc);
        }
        return result;
    }

    @Subscribe
    public void vppNodeChanged(NodeOperEvent event) {
        switch (event.getDtoModificationType()) {
            case CREATED:
                if (event.isAfterConnected()) {
                    // TODO
                }
                break;
            case UPDATED:
                if (!event.isBeforeConnected() && event.isAfterConnected()) {
                    // TODO
                }
                break;
            case DELETED:
                if (event.isBeforeConnected()) {
                    // TODO
                }
                break;
        }
    }
}
