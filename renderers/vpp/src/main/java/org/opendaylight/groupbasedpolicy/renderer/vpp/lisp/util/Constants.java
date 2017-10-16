/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.util;


/**
 * Created by Shakib Ahmed on 7/20/17.
 */
public class Constants {
    public static final String METADATA_IP = "169.254.169.254";
    public static final String METADATA_SUBNET_UUID = "local-metadata-subnet";
    public static final String ROUTING_PROTOCOL_NAME_PREFIX = "static-routing-";
    public static final String DEFAULT_ROUTING_DESCRIPTION = "Static route added from GBP for flat L3 overlay";
    public static final String GW_NAME_PREFIX = "loop-";
    public static final String GPE_ENTRY_PREFIX = "gpe-entry-";
    public static final String DUMMY_PROTOCOL_BRIDGE_DOMAIN = "bridge-domain-dummy-protocol";
    public static final String TENANT_INTERFACE = "tenant-interface";
    public static final String PUBLIC_SUBNET_UUID = "public-interface-subnet-uuid";

}
