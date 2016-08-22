/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.spi;

import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProvider;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProviderRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPToSgtMapper;

/**
 * Purpose: spi for sxp-ep-provider
 */
public interface SxpEpProviderProvider extends AutoCloseable {

    /**
     * @return registry point for {@link EPPolicyTemplateProvider}
     */
    EPPolicyTemplateProviderRegistry getEPPolicyTemplateProviderRegistry();

    /**
     * @return endpoint-to-sgt mapper
     */
    EPToSgtMapper getEPToSgtMapper();
}
