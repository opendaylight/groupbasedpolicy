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

    private static boolean DEFAULT_LISP_OVERLAY_ENABLED = false;
    private static boolean DEFAULT_LISP_MAPREGISTER_ENABLED = true;

    private IpAddress odlTenantIp;
    private boolean lispOverlayEnabled = DEFAULT_LISP_OVERLAY_ENABLED;
    private boolean lispMapRegisterEnbled = DEFAULT_LISP_MAPREGISTER_ENABLED;

    public static String ODL_TENANT_IP = "odl.ip.tenant";
    public static String LISP_OVERLAY_ENABLED = "gbp.lisp.enabled";
    public static String LISP_MAPREGISTER_ENABLED = "vpp.lisp.mapregister.enabled";

    private static ConfigUtil INSTANCE = new ConfigUtil();

    private ConfigUtil() {
        configureDefaults();
    }

    public static ConfigUtil getInstance() {
        return INSTANCE;
    }

    private void configureDefaults() {
        configureOdlTenantIp(null);
        configureLispOverlayEnabled(null);
        configureMapRegister(null);
    }

    public void configureLispOverlayEnabled(String configStr) {
        if (configStr == null) {
            configStr = System.getProperty(LISP_OVERLAY_ENABLED);

            if (configStr == null) {
                lispOverlayEnabled = DEFAULT_LISP_OVERLAY_ENABLED;
                LOG.debug("Configuration variable {} is being unset. Setting the variable to {}",
                        LISP_OVERLAY_ENABLED, DEFAULT_LISP_OVERLAY_ENABLED);
                return;
            }
        }

        configStr = configStr.trim();

        if (configStr.equalsIgnoreCase("true")) {
            lispOverlayEnabled = true;
        } else {
            lispOverlayEnabled = false;
        }
    }

    public void configureOdlTenantIp(String configStr) {
        if (configStr == null) {
            odlTenantIp = null;
            LOG.debug("Configuration variable {} is being unset. Setting the variable to null",
                    ODL_TENANT_IP);
            return;
        }

        configStr = configStr.trim();
        odlTenantIp = new IpAddress(configStr.toCharArray());
    }

    public void configureMapRegister(String configStr) {
        if (configStr == null) {
            lispMapRegisterEnbled = DEFAULT_LISP_MAPREGISTER_ENABLED;
            LOG.debug("Configuration variable {} is being unset. Setting the variable to {}",
                    LISP_MAPREGISTER_ENABLED, DEFAULT_LISP_MAPREGISTER_ENABLED);
            return;
        }

        configStr = configStr.trim();

        if (configStr.equalsIgnoreCase("true")) {
            lispMapRegisterEnbled = true;
        } else {
            lispOverlayEnabled = false;
        }
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

    public boolean isLispMapRegisterEnbled() {
        return lispMapRegisterEnbled;
    }
}
