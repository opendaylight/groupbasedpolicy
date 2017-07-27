/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.config;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.function.Consumer;

/**
 * Created by Shakib Ahmed on 4/17/17.
 */
public class ConfigurationService implements ManagedService{
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    HashMap<String, Consumer> configMethods;

    ConfigUtil configUtil = ConfigUtil.getInstance();

    public ConfigurationService() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.opendaylight.groupbasedpolicy.renderer.vpp.startup");
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        BundleContext context = null;
        if (bundle != null) {
            context = bundle.getBundleContext();
        }

        //this function needs to be called before context.registerService() method
        mapConfigMethods();

        context.registerService(ManagedService.class.getName(), this, properties);
    }

    @Override
    public void updated(Dictionary dictionary) throws ConfigurationException {
        if (dictionary == null) {
            return;
        }

        Enumeration keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();

            if (configMethods.containsKey(key)) {
                configMethods.get(key).accept(dictionary.get(key));
                LOG.info("Property {} being updated", key);
            } else {
                LOG.debug("Configuration {} = {} being ignored because no consumer for this " +
                        "configuration key has been mapped", keys, dictionary.get(key));
            }
        }
    }

    private void mapConfigMethods() {
        configMethods = new HashMap<>();

        configMethods.put(ConfigUtil.ODL_IP,
                ip -> configUtil.configureOdlIp((String) ip));
        configMethods.put(ConfigUtil.LISP_MAPREGISTER_ENABLED,
                mrConfig -> configUtil.configureMapRegister((String) mrConfig));
        configMethods.put(ConfigUtil.LISP_OVERLAY_ENABLED,
                overlayConfig -> configUtil.configureLispOverlayEnabled((String) overlayConfig));
        configMethods.put(ConfigUtil.L3_FLAT_ENABLED,
                l3FlatConfig -> configUtil.configL3FlatEnabled((String) l3FlatConfig));
    }
}
