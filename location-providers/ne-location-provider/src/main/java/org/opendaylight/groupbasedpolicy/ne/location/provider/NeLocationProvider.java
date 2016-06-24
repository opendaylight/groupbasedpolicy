/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.ne.location.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.groupbasedpolicy.util.NetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.endpoints.address.endpoints.AddressEndpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.AbsoluteLocationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.NetworkElements;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.NetworkElementsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.NetworkElement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.network.elements.rev160407.network.elements.network.element._interface.EndpointNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.IpPrefixType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.forwarding.l2_l3.rev160427.L3Context;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class NeLocationProvider implements DataTreeChangeListener<NetworkElements>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NeLocationProvider.class);
    public static final String NE_LOCATION_PROVIDER_NAME = "ne-location-provider";

    private List<AddressEndpoint> endpoints;
    private NetworkElements networkElements;
    private DataBroker dataBroker;
    private ListenerRegistration<NeLocationProvider> listenerRegistration;
    private EndpointsListener endpointsListener;

    public NeLocationProvider(DataBroker dataBroker) {
        this.listenerRegistration =
                dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.builder(NetworkElements.class).build()), this);
        this.endpoints = new ArrayList<>();
        this.networkElements = new NetworkElementsBuilder().build();
        this.dataBroker = dataBroker;
        this.endpointsListener = new EndpointsListener(dataBroker, this);
        LOG.info("NE location provider created");
    }

    @Override
    public void close() {
        this.listenerRegistration.close();
        this.endpointsListener.close();
    }

    public synchronized void onEndpointsChange(Collection<DataTreeModification<AddressEndpoint>> changes) {
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        for (DataTreeModification<AddressEndpoint> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    AddressEndpoint endpoint = change.getRootNode().getDataBefore();
                    removeLocationForEndpoint(endpoint, wtx);
                    this.endpoints.remove(endpoint);
                    break;
                }
                case WRITE: {
                    AddressEndpoint endpoint = change.getRootNode().getDataBefore();
                    if (endpoint != null) {
                        this.endpoints.remove(endpoint);
                        this.endpoints.add(change.getRootNode().getDataAfter());
                        break;
                    }
                    endpoint = change.getRootNode().getDataAfter();
                    createLocationForEndpoint(endpoint, wtx);
                    this.endpoints.add(endpoint);
                    break;
                }
                case SUBTREE_MODIFIED: {
                    this.endpoints.remove(change.getRootNode().getDataBefore());
                    this.endpoints.add(change.getRootNode().getDataAfter());
                    break;
                }
            }
        }
        DataStoreHelper.submitToDs(wtx);
    }

    private void createLocationForEndpoint(AddressEndpoint endpoint, WriteTransaction wtx) {
        for (NetworkElement ne : nullToEmpty(networkElements.getNetworkElement())) {
            for (Interface iface : nullToEmpty(ne.getInterface())) {
                for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                    if (endpoint.getContextType().isAssignableFrom(L3Context.class)
                            && endpoint.getContextId().equals(en.getL3ContextId())
                            && endpoint.getAddressType().isAssignableFrom(IpPrefixType.class) && NetUtils
                                .samePrefix(new IpPrefix(endpoint.getAddress().toCharArray()), en.getIpPrefix())) {
                        InstanceIdentifier<AbsoluteLocation> iid = IidFactory
                            .providerAddressEndpointLocationIid(NE_LOCATION_PROVIDER_NAME, IpPrefixType.class,
                                    endpoint.getAddress(), endpoint.getContextType(), endpoint.getContextId())
                            .child(AbsoluteLocation.class);
                        wtx.put(LogicalDatastoreType.CONFIGURATION, iid, createRealLocation(ne.getIid(), iface.getIid()),
                                true);
                        LOG.debug("New location created for endpoint {}", endpoint);
                        return;
                    }
                }
            }
        }
    }

    private void removeLocationForEndpoint(AddressEndpoint endpoint, WriteTransaction wtx) {
        for (NetworkElement ne : nullToEmpty(networkElements.getNetworkElement())) {
            for (Interface iface : nullToEmpty(ne.getInterface())) {
                for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                    if (endpoint.getContextType().isAssignableFrom(L3Context.class)
                            && endpoint.getContextId().equals(en.getL3ContextId())
                            && endpoint.getAddressType().isAssignableFrom(IpPrefixType.class) && NetUtils
                                .samePrefix(new IpPrefix(endpoint.getAddress().toCharArray()), en.getIpPrefix())) {
                        InstanceIdentifier<AbsoluteLocation> iid = IidFactory
                            .providerAddressEndpointLocationIid(NE_LOCATION_PROVIDER_NAME, IpPrefixType.class,
                                    endpoint.getAddress(), endpoint.getContextType(), endpoint.getContextId())
                            .child(AbsoluteLocation.class);
                        wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
                        LOG.debug("Location deleted for endpoint {}", endpoint);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public synchronized void onDataTreeChanged(Collection<DataTreeModification<NetworkElements>> changes) {
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        for (DataTreeModification<NetworkElements> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    NetworkElements nes = change.getRootNode().getDataBefore();
                    for (NetworkElement ne : nullToEmpty(nes.getNetworkElement())) {
                        for (Interface iface : nullToEmpty(ne.getInterface())) {
                            for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                                processDeletedEN(en, wtx);
                            }
                        }
                    }
                    networkElements = new NetworkElementsBuilder().build();
                    LOG.debug("Network elements removed");
                    break;
                }
                case WRITE: {
                    NetworkElements nes = change.getRootNode().getDataBefore();
                    if (nes != null) {
                        for (NetworkElement ne : nullToEmpty(nes.getNetworkElement())) {
                            for (Interface iface : nullToEmpty(ne.getInterface())) {
                                for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                                    processDeletedEN(en, wtx);
                                }
                            }
                        }
                    }
                    nes = change.getRootNode().getDataAfter();
                    for (NetworkElement ne : nullToEmpty(nes.getNetworkElement())) {
                        for (Interface iface : nullToEmpty(ne.getInterface())) {
                            for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                                processCreatedEN(en, ne.getIid(), iface.getIid(), wtx);
                            }
                        }
                    }
                    networkElements = nes;
                    LOG.debug("New Network elements created {}", change.getRootNode().getDataAfter());
                    break;
                }
                case SUBTREE_MODIFIED: {
                    List<DataObjectModification<NetworkElement>> modifiedNetworkElements =
                            getModifiedNetworkElements(change.getRootNode());
                    for (DataObjectModification<NetworkElement> netElement : modifiedNetworkElements) {
                        processNetworkElementChange(netElement, wtx);
                    }
                    break;
                }
            }
        }
        DataStoreHelper.submitToDs(wtx);
    }

    private List<DataObjectModification<NetworkElement>> getModifiedNetworkElements(
            DataObjectModification<NetworkElements> modifiedNEs) {
        Collection<DataObjectModification<? extends DataObject>> potentialModifiedNetworkElements =
                modifiedNEs.getModifiedChildren();
        if (potentialModifiedNetworkElements == null) {
            return Collections.emptyList();
        }
        List<DataObjectModification<NetworkElement>> nes = new ArrayList<>();
        for (DataObjectModification<? extends DataObject> potentialModifiedNetworkElement : potentialModifiedNetworkElements) {
            if (potentialModifiedNetworkElement.getDataType().isAssignableFrom(NetworkElement.class)) {
                nes.add((DataObjectModification<NetworkElement>) potentialModifiedNetworkElement);
            }
        }
        return nes;
    }

    private void processNetworkElementChange(DataObjectModification<NetworkElement> netElement, WriteTransaction wtx) {
        switch (netElement.getModificationType()) {
            case DELETE: {
                NetworkElement ne = netElement.getDataBefore();
                for (Interface iface : nullToEmpty(ne.getInterface())) {
                    for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                        processDeletedEN(en, wtx);
                    }
                }
                networkElements.getNetworkElement().remove(ne);
                LOG.debug("Netowrk element {} removed", netElement.getDataBefore());
                break;
            }
            case WRITE: {
                NetworkElement ne = netElement.getDataBefore();
                if (ne != null) {
                    for (Interface iface : nullToEmpty(ne.getInterface())) {
                        for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                            processDeletedEN(en, wtx);
                        }
                    }
                    networkElements.getNetworkElement().remove(ne);
                }
                ne = netElement.getDataAfter();
                for (Interface iface : nullToEmpty(ne.getInterface())) {
                    for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                        processCreatedEN(en, ne.getIid(), iface.getIid(), wtx);
                    }
                }
                networkElements.getNetworkElement().add(ne);
                LOG.debug("Created new Network element {}", netElement.getDataAfter());
                break;
            }
            case SUBTREE_MODIFIED: {
                List<DataObjectModification<Interface>> modifiedInterfaces = getModifiedInterfaces(netElement);
                for (DataObjectModification<Interface> modifiedInterface : modifiedInterfaces) {
                    processInterfaceChange(modifiedInterface, netElement.getDataBefore(), wtx);
                }
                break;
            }
        }
    }

    private List<DataObjectModification<Interface>> getModifiedInterfaces(
            DataObjectModification<NetworkElement> netElement) {
        Collection<DataObjectModification<? extends DataObject>> potentialModifiedInterfaces =
                netElement.getModifiedChildren();
        if (potentialModifiedInterfaces == null) {
            return Collections.emptyList();
        }
        List<DataObjectModification<Interface>> interfaces = new ArrayList<>();
        for (DataObjectModification<? extends DataObject> potentialModifiedInterface : potentialModifiedInterfaces) {
            if (potentialModifiedInterface.getDataType().isAssignableFrom(Interface.class)) {
                interfaces.add((DataObjectModification<Interface>) potentialModifiedInterface);
            }
        }
        return interfaces;
    }

    private void processInterfaceChange(DataObjectModification<Interface> modifiedInterface,
            NetworkElement nodeBefore, WriteTransaction wtx) {
        switch (modifiedInterface.getModificationType()) {
            case DELETE: {
                Interface iface = modifiedInterface.getDataBefore();
                for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                    processDeletedEN(en, wtx);
                }
                int nodeIndex = getIndexOf(nodeBefore);
                networkElements.getNetworkElement().get(nodeIndex).getInterface().remove(iface);
                LOG.debug("Interface {} removed", modifiedInterface.getDataBefore());
                break;
            }
            case WRITE: {
                Interface iface = modifiedInterface.getDataBefore();
                int nodeIndex = getIndexOf(nodeBefore);
                if (iface != null) {
                    for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                        processDeletedEN(en, wtx);
                    }
                    networkElements.getNetworkElement().get(nodeIndex).getInterface().remove(iface);
                }
                iface = modifiedInterface.getDataAfter();
                for (EndpointNetwork en : nullToEmpty(iface.getEndpointNetwork())) {
                    processCreatedEN(en, nodeBefore.getIid(), iface.getIid(), wtx);
                }
                networkElements.getNetworkElement().get(nodeIndex).getInterface().add(iface);
                LOG.debug("Created new Interface {}", modifiedInterface.getDataAfter());
                break;
            }
            case SUBTREE_MODIFIED: {
                List<DataObjectModification<EndpointNetwork>> modifiedENs =
                        getModifiedEndpointNetworks(modifiedInterface);
                for (DataObjectModification<EndpointNetwork> modifiedEN : modifiedENs) {
                    processEndpointNetworkChange(modifiedEN, nodeBefore, modifiedInterface.getDataBefore(), wtx);
                }
                break;
            }
        }
    }

    private List<DataObjectModification<EndpointNetwork>> getModifiedEndpointNetworks(
            DataObjectModification<Interface> modifiedInterface) {
        Collection<DataObjectModification<? extends DataObject>> potentialModifiedEPs =
                modifiedInterface.getModifiedChildren();
        if (potentialModifiedEPs == null) {
            return Collections.emptyList();
        }
        List<DataObjectModification<EndpointNetwork>> eps = new ArrayList<>();
        for (DataObjectModification<? extends DataObject> potentialModifiedEP : potentialModifiedEPs) {
            if (potentialModifiedEP.getDataType().isAssignableFrom(EndpointNetwork.class)) {
                eps.add((DataObjectModification<EndpointNetwork>) potentialModifiedEP);
            }
        }
        return eps;
    }

    private void processEndpointNetworkChange(DataObjectModification<EndpointNetwork> modifiedEN,
            NetworkElement nodeBefore, Interface ifaceBefore, WriteTransaction wtx) {
        switch (modifiedEN.getModificationType()) {
            case DELETE: {
                processDeletedEN(modifiedEN.getDataBefore(), wtx);
                int nodeIndex = getIndexOf(nodeBefore);
                int ifaceIndex = getIndexOf(ifaceBefore, nodeIndex);
                networkElements.getNetworkElement()
                    .get(nodeIndex)
                    .getInterface()
                    .get(ifaceIndex)
                    .getEndpointNetwork()
                    .remove(modifiedEN.getDataBefore());
                LOG.debug("Endpoint network {} removed", modifiedEN.getDataBefore());
                break;
            }
            case WRITE: {
                processCreatedEN(modifiedEN.getDataAfter(), nodeBefore.getIid(), ifaceBefore.getIid(), wtx);
                int nodeIndex = getIndexOf(nodeBefore);
                int ifaceIndex = getIndexOf(ifaceBefore, nodeIndex);
                networkElements.getNetworkElement()
                    .get(nodeIndex)
                    .getInterface()
                    .get(ifaceIndex)
                    .getEndpointNetwork()
                    .add(modifiedEN.getDataAfter());
                LOG.debug("Created new Endpoint network {}", modifiedEN.getDataAfter());
                break;
            }
            case SUBTREE_MODIFIED: {
                LOG.debug("EndpointNetwork {} changed", modifiedEN.getDataAfter().getKey());
                break;
            }
        }
    }

    private void processCreatedEN(EndpointNetwork en, InstanceIdentifier<?> nodeIID,
            String connectorIID, WriteTransaction wtx) {
        for (AddressEndpoint endpoint : endpoints) {
            if (endpoint.getContextType().isAssignableFrom(L3Context.class)
                    && endpoint.getContextId().equals(en.getL3ContextId())
                    && endpoint.getAddressType().isAssignableFrom(IpPrefixType.class)
                    && NetUtils.samePrefix(new IpPrefix(endpoint.getAddress().toCharArray()), en.getIpPrefix())) {
                InstanceIdentifier<AbsoluteLocation> iid = IidFactory
                    .providerAddressEndpointLocationIid(NE_LOCATION_PROVIDER_NAME, IpPrefixType.class,
                            endpoint.getAddress(), endpoint.getContextType(), endpoint.getContextId())
                    .child(AbsoluteLocation.class);
                wtx.put(LogicalDatastoreType.CONFIGURATION, iid, createRealLocation(nodeIID, connectorIID), true);
                LOG.debug("New location created for endpoint {}", endpoint);
                return;
            }
        }
    }

    private void processDeletedEN(EndpointNetwork en, WriteTransaction wtx) {
        for (AddressEndpoint endpoint : endpoints) {
            if (endpoint.getContextType().isAssignableFrom(L3Context.class)
                    && endpoint.getContextId().equals(en.getL3ContextId())
                    && endpoint.getAddressType().isAssignableFrom(IpPrefixType.class)
                    && NetUtils.samePrefix(new IpPrefix(endpoint.getAddress().toCharArray()), en.getIpPrefix())) {
                InstanceIdentifier<AbsoluteLocation> iid = IidFactory
                    .providerAddressEndpointLocationIid(NE_LOCATION_PROVIDER_NAME, IpPrefixType.class,
                            endpoint.getAddress(), endpoint.getContextType(), endpoint.getContextId())
                    .child(AbsoluteLocation.class);
                wtx.delete(LogicalDatastoreType.CONFIGURATION, iid);
                LOG.debug("Location deleted for endpoint {}", endpoint);
                return;
            }
        }
    }

    private AbsoluteLocation createRealLocation(InstanceIdentifier<?> node, String iface) {
        return new AbsoluteLocationBuilder()
            .setLocationType(new ExternalLocationCaseBuilder().setExternalNodeMountPoint(node)
                    .setExternalNodeConnector(iface).build()).build();
    }

    private <T> List<T> nullToEmpty(@Nullable List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private int getIndexOf(NetworkElement ne) {
        for (NetworkElement listNE : networkElements.getNetworkElement()) {
            if (ne.getIid().equals(listNE.getIid())) {
                return networkElements.getNetworkElement().indexOf(listNE);
            }
        }
        return -1;
    }

    private int getIndexOf(Interface iface, int nodeIndex) {
        for (Interface listIface : networkElements.getNetworkElement().get(nodeIndex).getInterface()) {
            if (iface.getIid().equals(listIface.getIid())) {
                return networkElements.getNetworkElement().get(nodeIndex).getInterface().indexOf(listIface);
            }
        }
        return -1;
    }

    @VisibleForTesting
    synchronized List<AddressEndpoint> getEndpoints() {
        return endpoints;
    }

    @VisibleForTesting
    synchronized NetworkElements getNetworkElements() {
        return networkElements;
    }
}
