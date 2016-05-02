/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.EPTemplateUtil;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SubnetInfoKeyDecorator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;

/**
 * Purpose: generic implementation of {@link SimpleCachedDao}
 */
public class SimpleCachedDaoEPForwardingTemplateImpl implements SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> {

    private final ConcurrentMap<IpPrefix, EndpointForwardingTemplateBySubnet> plainCache;
    private final ConcurrentMap<SubnetInfoKeyDecorator, EndpointForwardingTemplateBySubnet> subnetCache;
    private final Pattern IP_MASK_EATER_RE = Pattern.compile("/[0-9]+");

    public SimpleCachedDaoEPForwardingTemplateImpl() {
        plainCache = new ConcurrentHashMap<>();
        subnetCache = new ConcurrentHashMap<>();
    }

    @Override
    public EndpointForwardingTemplateBySubnet update(@Nonnull final IpPrefix key, @Nullable final EndpointForwardingTemplateBySubnet value) {
        final EndpointForwardingTemplateBySubnet previousValue;
        if (EPTemplateUtil.isPlain(key)) {
            previousValue = updatePlainCache(key, value);
        } else {
            previousValue = updateSubnetCache(key, value);
        }

        return previousValue;
    }

    private EndpointForwardingTemplateBySubnet updateSubnetCache(final IpPrefix key, final EndpointForwardingTemplateBySubnet value) {
        final EndpointForwardingTemplateBySubnet previousValue;
        final SubnetInfoKeyDecorator subnetKey = EPTemplateUtil.buildSubnetInfoKey(key);
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
    public Optional<EndpointForwardingTemplateBySubnet> find(@Nonnull final IpPrefix key) {
        final Optional<EndpointForwardingTemplateBySubnet> template;
        if (EPTemplateUtil.isPlain(key)) {
            final Optional<EndpointForwardingTemplateBySubnet> fastPlain = Optional.fromNullable(plainCache.get(key));
            if (fastPlain.isPresent()) {
                template = fastPlain;
            } else {
                template = lookupSlowSubnet(key.getIpv4Prefix().getValue());
            }
        } else {
            final SubnetInfoKeyDecorator keyDecorator = EPTemplateUtil.buildSubnetInfoKey(key);
            final Optional<EndpointForwardingTemplateBySubnet> fastSubnet =
                    Optional.fromNullable(subnetCache.get(keyDecorator));
            if (fastSubnet.isPresent()) {
                template = fastSubnet;
            } else {
                template = Optional.absent();
            }
        }
        return template;
    }

    private Optional<EndpointForwardingTemplateBySubnet> lookupSlowSubnet(final String value) {
        final String plainIp = IP_MASK_EATER_RE.matcher(value).replaceFirst("");
        EndpointForwardingTemplateBySubnet valueCandidate = null;
        int addressCount = 0;
        for (Map.Entry<SubnetInfoKeyDecorator, EndpointForwardingTemplateBySubnet> entry : subnetCache.entrySet()) {
            final SubnetUtils.SubnetInfo subnetInfo = entry.getKey().getDelegate();
            if (subnetInfo.isInRange(plainIp)) {
                final int addressCountTmp = subnetInfo.getAddressCount();
                if (valueCandidate == null || addressCount > addressCountTmp) {
                    valueCandidate = entry.getValue();
                    addressCount = addressCountTmp;
                }
            }
        }
        return Optional.fromNullable(valueCandidate);
    }

    @Override
    public void invalidateCache() {
        plainCache.clear();
        subnetCache.clear();
    }

    @Override
    public boolean isEmpty() {
        return plainCache.isEmpty() && subnetCache.isEmpty();
    }

    @Override
    public Iterable<EndpointForwardingTemplateBySubnet> values() {
        return Iterables.unmodifiableIterable(Iterables.concat(plainCache.values(), subnetCache.values()));
    }
}
