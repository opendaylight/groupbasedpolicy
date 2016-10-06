/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import java.util.Optional;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.EPTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.rev160722.SgtGeneratorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;

/**
 * Purpose: generate {@link Sgt} value on demand
 */
public class SgtGeneratorImpl {
    private final Ordering<Sgt> sgtOrdering;
    private Optional<Range<Integer>> sgtRange = Optional.empty();

    public SgtGeneratorImpl(final SgtGeneratorConfig sgtGenerator) {
        if (sgtGenerator != null) {
            sgtRange = Optional.of(
                    Range.closed(sgtGenerator.getSgtLow().getValue(), sgtGenerator.getSgtHigh().getValue()));
        }
        sgtOrdering = EPTemplateUtil.createSgtOrdering();
    }

    /**
     * @param templateCache source of used sgt items
     * @return next free sgt
     */
    public java.util.Optional<Sgt> generateNextSgt(SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> templateCache) {
        return sgtRange.flatMap(range ->
                findTopUsedSgt(templateCache.keySet(), range.lowerEndpoint())
                        .map(topUsedSgt -> incrementSafely(range, topUsedSgt))
        );
    }

    private Optional<Sgt> findTopUsedSgt(final Iterable<Sgt> sgts, final Integer lowerEndPoint) {
        final Sgt sgt = java.util.Optional.ofNullable(sgts)
                .filter(sgtBag -> ! Iterables.isEmpty(sgtBag))
                .map(sgtOrdering::max)
                .orElseGet(() -> new Sgt(lowerEndPoint - 1));
        return Optional.of(sgt);
    }

    private Sgt incrementSafely(final Range<Integer> range, final Sgt topUsedSgt) {
        final Sgt applicableSgt;

        final int nextMax = topUsedSgt.getValue() + 1;
        if (range.contains(nextMax)) {
            applicableSgt = new Sgt(nextMax);
        } else if (nextMax < range.lowerEndpoint()) {
            applicableSgt = new Sgt(range.lowerEndpoint());
        } else {
            applicableSgt = null;
        }

        return applicableSgt;
    }
}
