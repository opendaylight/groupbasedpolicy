/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * The purpose of this class is to eliminate boilerplate code used in most of
 * {@link DataTreeChangeListener} implementations.
 * 
 * @param <T>
 */
public abstract class DataTreeChangeHandler<T extends DataObject> implements DataTreeChangeListener<T>, AutoCloseable {

    protected final DataBroker dataProvider;
    protected final ListenerRegistration<DataTreeChangeHandler<T>> registeredListener;

    /**
     * Registers {@link DataTreeChangeListener} for {@code pointOfInterest} by using
     * {@code dataProvider}
     * 
     * @param dataProvider cannot be {@code null}
     * @param pointOfInterest cannot be {@code null}
     * @throws NullPointerException if at least one paramter is {@code null}
     */
    protected DataTreeChangeHandler(DataBroker dataProvider, DataTreeIdentifier<T> pointOfInterest) {
        this.dataProvider = checkNotNull(dataProvider);
        registeredListener = dataProvider.registerDataTreeChangeListener(checkNotNull(pointOfInterest), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        for (DataTreeModification<T> change : changes) {
            DataObjectModification<T> rootNode = change.getRootNode();
            InstanceIdentifier<T> rootIdentifier = change.getRootPath().getRootIdentifier();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    onWrite(rootNode, rootIdentifier);
                    break;
                case DELETE:
                    onDelete(rootNode, rootIdentifier);
                    break;
                case SUBTREE_MODIFIED:
                    onSubreeModified(rootNode, rootIdentifier);
                    break;
            }
        }
    }

    /**
     * Handles case where {@link DataObjectModification#getModificationType()} is
     * {@link ModificationType#WRITE}. <br>
     * <b>Parameters of this method are never {@code null}.</b>
     * 
     * @param rootNode represents {@link DataObjectModification} as result of
     *        {@link DataTreeModification#getRootNode()}
     * @param rootIdentifier represents {@link InstanceIdentifier} obtained from result of
     *        {@link DataTreeModification#getRootPath()}
     */
    protected abstract void onWrite(DataObjectModification<T> rootNode, InstanceIdentifier<T> rootIdentifier);

    /**
     * Handles case where {@link DataObjectModification#getModificationType()} is
     * {@link ModificationType#DELETE}. <br>
     * <b>Parameters of this method are never {@code null}.</b>
     * 
     * @param rootNode represents {@link DataObjectModification} as result of
     *        {@link DataTreeModification#getRootNode()}
     * @param rootIdentifier represents {@link InstanceIdentifier} obtained from result of
     *        {@link DataTreeModification#getRootPath()}
     */
    protected abstract void onDelete(DataObjectModification<T> rootNode, InstanceIdentifier<T> rootIdentifier);

    /**
     * Handles case where {@link DataObjectModification#getModificationType()} is
     * {@link ModificationType#SUBTREE_MODIFIED}. <br>
     * <b>Parameters of this method are never {@code null}.</b>
     * 
     * @param rootNode represents {@link DataObjectModification} as result of
     *        {@link DataTreeModification#getRootNode()}
     * @param rootIdentifier represents {@link InstanceIdentifier} obtained from result of
     *        {@link DataTreeModification#getRootPath()}
     */
    protected abstract void onSubreeModified(DataObjectModification<T> rootNode, InstanceIdentifier<T> rootIdentifier);

    @Override
    public void close() throws Exception {
        registeredListener.close();
    }

}
