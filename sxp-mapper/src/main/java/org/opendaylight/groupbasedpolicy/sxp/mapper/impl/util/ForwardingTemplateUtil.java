/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util;

import javax.annotation.Nonnull;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;

/**
 * Purpose: util methods for {@link org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet}
 */
public final class ForwardingTemplateUtil {

    public static final String FULL_IPV4_MASK_SUFFIX = "/32";

    private ForwardingTemplateUtil() {
        throw new IllegalAccessError("constructing util class");
    }

    public static boolean isPlain(final IpPrefix key) {
        return key.getIpv4Prefix().getValue().endsWith(FULL_IPV4_MASK_SUFFIX);
    }

    public static SubnetInfoKeyDecorator buildSubnetInfoKey(@Nonnull final IpPrefix value) {
        return new SubnetInfoKeyDecorator(new SubnetUtils(value.getIpv4Prefix().getValue()).getInfo());
    }
}
