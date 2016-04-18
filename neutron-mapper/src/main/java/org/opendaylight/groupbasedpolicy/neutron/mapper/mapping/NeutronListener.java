/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.neutron.spi.NeutronObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class NeutronListener implements DataTreeChangeListener<Neutron>, Closeable {

    private final ListenerRegistration<NeutronListener> registerDataTreeChangeListener;
    private final Map<InstanceIdentifier<? extends DataObject>, MappingProcessor<? extends DataObject, ? extends NeutronObject>> dataChangeProviders = new LinkedHashMap<>();
    private static DataObjectModification<Neutron> neutron;

    public NeutronListener(DataBroker dataProvider) {
        registerDataTreeChangeListener = dataProvider.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Neutron.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Neutron>> changes) {
        for (DataTreeModification<Neutron> change : changes) {
            neutron = change.getRootNode();
            for (InstanceIdentifier<? extends DataObject> iid : dataChangeProviders.keySet()) {
                for (DataObjectModification<? extends DataObject> modifDto : findModifiedData(iid, change.getRootNode())) {
                    processChangedData(modifDto, modifDto.getModificationType(), getMappingProvider(iid));
                }
            }
        }
    }

    /**
     * Finds all modified subnodes of given type in {@link Neutron} node.
     *
     * @param iid path to data in root node
     * @param rootNode modified data of {@link Neutron} node
     * @return {@link List} of modified subnodes
     */
    private List<DataObjectModification<? extends DataObject>> findModifiedData(
            InstanceIdentifier<? extends DataObject> iid, DataObjectModification<Neutron> rootNode) {
        List<DataObjectModification<? extends DataObject>> modDtos = new ArrayList<>();
        PeekingIterator<PathArgument> pathArgs = Iterators.peekingIterator(iid.getPathArguments().iterator());
        DataObjectModification<? extends DataObject> modifDto = rootNode;
        while (pathArgs.hasNext()) {
            pathArgs.next();
            for (DataObjectModification<? extends DataObject> childDto : modifDto.getModifiedChildren()) {
                if (pathArgs.hasNext() && childDto.getDataType().equals(pathArgs.peek().getType())) {
                    if (childDto.getDataType().equals(iid.getTargetType())) {
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

    static Neutron getNeutronDataBefore() {
        return neutron.getDataBefore();
    }

    static Neutron getNeutronDataAfter() {
        return neutron.getDataAfter();
    }

    @SuppressWarnings("unchecked")
    <T extends DataObject, X extends NeutronObject> void processChangedData(DataObjectModification<?> dto,
            ModificationType m, MappingProcessor<T, X> d) {
        switch (m) {
            case WRITE: {
                T dataAfter = (T) dto.getDataAfter();
                X neutronObject = d.convertToNeutron(dataAfter);
                if (StatusCode.OK == d.canCreate(neutronObject)) {
                    d.created(neutronObject);
                }
                break;
            }
            case SUBTREE_MODIFIED: {
                X neutronObjectBefore = d.convertToNeutron((T) dto.getDataBefore());
                X neutronObjectAfter = d.convertToNeutron((T) dto.getDataAfter());
                if (StatusCode.OK == d.canUpdate(neutronObjectAfter, neutronObjectBefore)) {
                    d.updated(neutronObjectAfter);
                }
                break;
            }
            case DELETE: {
                X neutronObjectBefore = d.convertToNeutron((T) dto.getDataBefore());
                if (StatusCode.OK == d.canDelete(neutronObjectBefore)) {
                    d.deleted(neutronObjectBefore);
                }
                break;
            }
        }
    }

    public <D extends DataObject, N extends NeutronObject> void registerMappingProviders(
            InstanceIdentifier<D> iid, MappingProcessor<D, N> np) {
        dataChangeProviders.put(iid, np);
    }

    @SuppressWarnings("unchecked")
    <D extends DataObject, N extends NeutronObject> MappingProcessor<D, N> getMappingProvider(
            InstanceIdentifier<D> iid) {
        return (MappingProcessor<D, N>) dataChangeProviders.get(iid);
    }

    @Override
    public void close() throws IOException {
        registerDataTreeChangeListener.close();
    }

}

