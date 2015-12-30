/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.restclient;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.restclient.RestClient;

import com.google.common.collect.ImmutableList;

public class RestClientTest {

    private RestClient client;
    private String uri;
    private String resolvedPolicy =
            " { \"resolved-policy-uri\" : \"/restconf/operational/resolved-policy:resolved-policies/resolved-policy/tenant-red/client/tenant-red/webserver/\" } ";

}
