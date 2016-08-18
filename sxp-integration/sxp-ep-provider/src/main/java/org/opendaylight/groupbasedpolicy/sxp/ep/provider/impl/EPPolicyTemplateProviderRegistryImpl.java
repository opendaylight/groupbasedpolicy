/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProvider;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.EPPolicyTemplateProviderRegistry;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.api.TemplateProviderDistributionTarget;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Purpose: provide registration and internal distribution for obtained {@link EPPolicyTemplateProvider}
 */
public class EPPolicyTemplateProviderRegistryImpl implements EPPolicyTemplateProviderRegistry {
    private EPPolicyTemplateProvider templateProvider;
    private Set<TemplateProviderDistributionTarget<EPPolicyTemplateProvider>> distributionTargets = Sets.newConcurrentHashSet();

    @Override
    public ObjectRegistration<EPPolicyTemplateProvider> registerTemplateProvider(final EPPolicyTemplateProvider templateProvider) {
        distributeTemplateProvider(templateProvider);
        return new AbstractObjectRegistration<EPPolicyTemplateProvider>(templateProvider) {
            @Override
            protected void removeRegistration() {
                distributeTemplateProvider(null);
            }
        };
    }

    @Override
    public void addDistributionTarget(final TemplateProviderDistributionTarget<EPPolicyTemplateProvider> templateProviderDistributionTarget) {
        distributionTargets.add(templateProviderDistributionTarget);
        if (templateProvider != null) {
            templateProviderDistributionTarget.setTemplateProvider(templateProvider);
        }
    }

    /**
     * inject given templateProvider into available targets
     * @param templateProvider current provider
     */
    private void distributeTemplateProvider(final EPPolicyTemplateProvider templateProvider) {
        this.templateProvider = templateProvider;
        for (TemplateProviderDistributionTarget<EPPolicyTemplateProvider> distributionTarget : distributionTargets) {
            distributionTarget.setTemplateProvider(templateProvider);
        }
    }

    @Override
    public void close() {
        distributeTemplateProvider(null);
        distributionTargets.clear();
    }
}
