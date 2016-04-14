/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoCached;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.ForwardingTemplateUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;

/**
 * Purpose: generic implementation of {@link DSDaoCached}
 */
public class DSDaoCachedEPForwardingTemplateImpl implements DSDaoCached<IpPrefix, EndpointForwardingTemplateBySubnet> {

    private final ConcurrentMap<IpPrefix, EndpointForwardingTemplateBySubnet> plainCache;
    private final ConcurrentMap<SubnetUtils.SubnetInfo, EndpointForwardingTemplateBySubnet> subnetCache;

    public DSDaoCachedEPForwardingTemplateImpl() {
        plainCache = new ConcurrentHashMap<>();
        subnetCache = new ConcurrentHashMap<>();
    }

    @Override
    public EndpointForwardingTemplateBySubnet update(@Nonnull final IpPrefix key, @Nullable final EndpointForwardingTemplateBySubnet value) {
        final EndpointForwardingTemplateBySubnet previousValue;
        if (ForwardingTemplateUtil.isPlain(key)) {
            previousValue = updatePlainCache(key, value);
        } else {
            previousValue = updateSubnetCache(key, value);
        }

        return previousValue;
    }

    private EndpointForwardingTemplateBySubnet updateSubnetCache(final IpPrefix key, final EndpointForwardingTemplateBySubnet value) {
        final EndpointForwardingTemplateBySubnet previousValue;
        final SubnetUtils.SubnetInfo subnetKey = ForwardingTemplateUtil.buildSubnetInfo(key);
        if (value != null) {
            previousValue = subnetCache.put(subnetKey, value);
        } else {
            previousValue = subnetCache.remove(subnetKey);
        }
        return previousValue;
    }

    private EndpointForwardingTemplateBySubnet updatePlainCache(final @Nonnull IpPrefix key, final @Nullable EndpointForwardingTemplateBySubnet value) {
        final EndpointForwardingTemplateBySubnet previousValue;
        if (value != null) {
            previousValue = plainCache.put(key, value);
        } else {
            previousValue = plainCache.remove(key);
        }
        return previousValue;
    }

    @Override
    public Optional<EndpointForwardingTemplateBySubnet> read(@Nonnull final IpPrefix key) {
        final Optional<EndpointForwardingTemplateBySubnet> template;
        if (ForwardingTemplateUtil.isPlain(key)) {
            template = Optional.fromNullable(plainCache.get(key));
        } else {
            template = Optional.fromNullable(subnetCache.get(ForwardingTemplateUtil.buildSubnetInfo(key)));
        }
        return template;
    }

    @Override
    public void invalidateCache() {
        plainCache.clear();
        subnetCache.clear();
    }
}
