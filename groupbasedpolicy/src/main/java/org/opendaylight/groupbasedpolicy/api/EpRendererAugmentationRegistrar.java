/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

public interface EpRendererAugmentationRegistrar {

    /**
     * Registers renderer's endpoint augmentation.
     * 
     * @param epRendererAugmentation cannot be {@code null}
     * @throws NullPointerException
     */
    void register(EpRendererAugmentation epRendererAugmentation);

    /**
     * Unregisters renderer's endpoint augmentation.
     * 
     * @param epRendererAugmentation cannot be {@code null}
     * @throws NullPointerException
     */
    void unregister(EpRendererAugmentation epRendererAugmentation);
}
