/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.api;

public interface BaseEndpointRendererAugmentationRegistry {

    /**
     * Registers renderer's endpoints augmentation.
     *
     * @param baseEndpointRendererAugmentation cannot be {@code null}
     * @throws NullPointerException
     */
    void register(BaseEndpointRendererAugmentation baseEndpointRendererAugmentation);

    /**
     * Unregisters renderer's endpoints augmentation.
     *
     * @param baseEndpointRendererAugmentation cannot be {@code null}
     * @throws NullPointerException
     */
    void unregister(BaseEndpointRendererAugmentation baseEndpointRendererAugmentation);
}
