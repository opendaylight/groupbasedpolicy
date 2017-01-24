/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class NeutronListener implements ClusteredDataTreeChangeListener<Neutron>, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronListener.class);

    private final Set<MappingProvider<? extends DataObject>> dataChangeProviders = new LinkedHashSet<>();
    protected ListenerRegistration<NeutronListener> registeredListener;

    public NeutronListener(DataBroker dataBroker) {
        registerHandlersAndListeners(dataBroker);
        registeredListener = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Neutron.class).build()), this);
    }

    private void registerHandlersAndListeners(DataBroker dataBroker) {
        PortHandler portHandler = new PortHandler(dataBroker);
        dataChangeProviders.add(new PortAware(portHandler, dataBroker));
        dataChangeProviders.add(new NetworkAware(dataBroker));
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Neutron>> changes) {
        for (DataTreeModification<Neutron> change : changes) {
            DataObjectModification<Neutron> rootNode = change.getRootNode();
            for (MappingProvider<? extends DataObject> provider : dataChangeProviders) {
                for (DataObjectModification<? extends DataObject> modDto : findModifiedData(provider, rootNode)) {
                    try {
                        processChangedData(modDto, modDto.getModificationType(), provider);
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.error("Failed to process {} modification of node: {}. {}", modDto.getModificationType(),
                                modDto.getIdentifier(), e.getStackTrace());
                    }
                }
            }
        }
    }

    List<DataObjectModification<? extends DataObject>> findModifiedData(MappingProvider<? extends DataObject> provider,
            DataObjectModification<Neutron> rootNode) {
        List<DataObjectModification<? extends DataObject>> modDtos = new ArrayList<>();
        PeekingIterator<PathArgument> pathArgs = Iterators.peekingIterator(provider.getNeutronDtoIid()
            .getPathArguments()
            .iterator());
        DataObjectModification<? extends DataObject> modifDto = rootNode;
        while (pathArgs.hasNext()) {
            pathArgs.next();
            for (DataObjectModification<? extends DataObject> childDto : modifDto.getModifiedChildren()) {
                if (pathArgs.hasNext() && childDto.getDataType().equals(pathArgs.peek().getType())) {
                    if (childDto.getDataType().equals(provider.getNeutronDtoIid().getTargetType())) {
                        modDtos.add(childDto);
                    } else {
                        modifDto = childDto;
                        break;
                    }
                }
            }
        }
        return modDtos;
    }

    @SuppressWarnings("unchecked")
    <T extends DataObject> void processChangedData(DataObjectModification<?> dto, ModificationType m,
            MappingProvider<T> processor) throws InterruptedException, ExecutionException {
        switch (m) {
            case WRITE: {
                if (dto.getDataBefore() != null) {
                    processor.processUpdatedNeutronDto((T) dto.getDataBefore(), (T) dto.getDataAfter());
                } else {
                    processor.processCreatedNeutronDto((T) dto.getDataAfter());
                }
                break;
            }
            case SUBTREE_MODIFIED: {
                processor.processUpdatedNeutronDto((T) dto.getDataBefore(), (T) dto.getDataAfter());
                break;
            }
            case DELETE: {
                processor.processDeletedNeutronDto((T) dto.getDataBefore());
                break;
            }
        }
    }

    @VisibleForTesting
    void clearDataChangeProviders() {
        dataChangeProviders.clear();
    }

    @VisibleForTesting
    <T extends DataObject> void addDataChangeProvider(MappingProvider<T> t) {
        dataChangeProviders.add(t);
    }

    @Override
    public void close() {
        registeredListener.close();
    }
}
