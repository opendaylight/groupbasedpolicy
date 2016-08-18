/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp_ise_adapter.impl;

import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.gbp.sxp.ise.adapter.model.rev160630.gbp.sxp.ise.adapter.IseSourceConfig;

/**
 * Purpose: wrap {@link EPPolicyTemplateProvider} and {@link IseSourceConfig} injection
 */
public interface EPPolicyTemplateProviderFacade extends EPPolicyTemplateProvider {

    /**
     * @param iseSourceConfig current ise configuration
     */
    void assignIseSourceConfig(final IseSourceConfig iseSourceConfig);

}
