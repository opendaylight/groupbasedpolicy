/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.config;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Shakib Ahmed on 4/13/17.
 */
public class ConfigUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigUtil.class);

    private static final boolean DEFAULT_LISP_OVERLAY_ENABLED = false;
    private static final boolean DEFAULT_LISP_MAPREGISTER_ENABLED = true;
    private static final boolean DEFAULT_L3_FLAT_ENABLED = false;
    private static final String DEFAULT_TRUE_STRING_VALUE = "true";
    private static final String CONFIGURATION_VARIABLE_MESSAGE =
        "Configuration variable {} is being unset. Setting the variable to {}";

    private IpAddress odlTenantIp;
    private boolean lispOverlayEnabled = DEFAULT_LISP_OVERLAY_ENABLED;
    private boolean lispMapRegisterEnabled = DEFAULT_LISP_MAPREGISTER_ENABLED;
    private boolean l3FlatEnabled = DEFAULT_L3_FLAT_ENABLED;

    static final String ODL_IP = "odl.ip";
    static final String LISP_OVERLAY_ENABLED = "gbp.lisp.enabled";
    static final String LISP_MAPREGISTER_ENABLED = "vpp.lisp.mapregister.enabled";
    static final String L3_FLAT_ENABLED = "vpp.l3.flat.enabled";

    private static final ConfigUtil INSTANCE = new ConfigUtil();

    private ConfigUtil() {
        configureOdlTenantIp(null);
        configureLispOverlayEnabled(null);
        configureMapRegister(null);
        configL3FlatEnabled(null);
    }

    public static ConfigUtil getInstance() {
        return INSTANCE;
    }

    void configureLispOverlayEnabled(String configStr) {
        if (configStr == null) {
            configStr = System.getProperty(LISP_OVERLAY_ENABLED);

            if (configStr == null) {
                lispOverlayEnabled = DEFAULT_LISP_OVERLAY_ENABLED;
                LOG.debug(CONFIGURATION_VARIABLE_MESSAGE, LISP_OVERLAY_ENABLED, DEFAULT_LISP_OVERLAY_ENABLED);
                return;
            }
        }
        lispOverlayEnabled = configStr.trim().equalsIgnoreCase(DEFAULT_TRUE_STRING_VALUE);
    }

    void configureOdlTenantIp(String configStr) {
        if (configStr == null) {
            odlTenantIp = null;
            LOG.debug("Configuration variable {} is being unset. Setting the variable to null",
                    ODL_IP);
            return;
        }
        odlTenantIp = new IpAddress(configStr.trim().toCharArray());
    }

    void configureMapRegister(String configStr) {
        if (configStr == null) {
            lispMapRegisterEnabled = DEFAULT_LISP_MAPREGISTER_ENABLED;
            LOG.debug(CONFIGURATION_VARIABLE_MESSAGE, LISP_MAPREGISTER_ENABLED, DEFAULT_LISP_MAPREGISTER_ENABLED);
            return;
        }
        lispMapRegisterEnabled = configStr.trim().equalsIgnoreCase(DEFAULT_TRUE_STRING_VALUE);
    }

    void configL3FlatEnabled(String configStr) {
        if (configStr == null) {
            l3FlatEnabled = DEFAULT_L3_FLAT_ENABLED;
            LOG.debug(CONFIGURATION_VARIABLE_MESSAGE, L3_FLAT_ENABLED, DEFAULT_L3_FLAT_ENABLED);
            return;
        }
        l3FlatEnabled = configStr.trim().equalsIgnoreCase(DEFAULT_TRUE_STRING_VALUE);
    }

    public IpAddress getOdlTenantIp() {
        return odlTenantIp;
    }

    public boolean isLispOverlayEnabled() {
        if (lispOverlayEnabled) {
            Preconditions.checkNotNull(odlTenantIp, "Configuration variable {} is not set. " +
                    "So, {} can't be used for chances of invalid config!", LISP_OVERLAY_ENABLED);
        }
        return lispOverlayEnabled;
    }

    public boolean isLispMapRegisterEnabled() {
        return lispMapRegisterEnabled;
    }

    public boolean isL3FlatEnabled() {
        return l3FlatEnabled;
    }
}
