/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.api;

import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Contract: based on given {@link Sgt} and {@link TenantId} deliver {@link EndpointPolicyTemplateBySgt} value
 */
public interface EPPolicyTemplateProvider {

    /**
     * @param sgt of template
     * @param tenantId of template
     * @return template if available (expecting empty conditions field)
     */
    Optional<EndpointPolicyTemplateBySgt> provideTemplate(final Sgt sgt, final TenantId tenantId);
}
