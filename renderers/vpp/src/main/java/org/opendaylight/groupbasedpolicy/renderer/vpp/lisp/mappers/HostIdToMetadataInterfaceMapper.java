/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.lisp.mappers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;


/**
 * Created by Shakib Ahmed on 7/10/17.
 */
public class HostIdToMetadataInterfaceMapper {
    private SetMultimap<String, String> hostIdToMetadataInterfaceNameMapper;

    private static final HostIdToMetadataInterfaceMapper INSTANCE = new HostIdToMetadataInterfaceMapper();

    private HostIdToMetadataInterfaceMapper() {
        hostIdToMetadataInterfaceNameMapper = HashMultimap.create();
    }

    public static HostIdToMetadataInterfaceMapper getInstance() {
        return INSTANCE;
    }

    public boolean isMetadataInterfaceConfigured(String hostId, String metadataInterfaceName) {
        return hostIdToMetadataInterfaceNameMapper.get(hostId).contains(metadataInterfaceName);
    }

    public void addMetadataInterfaceInHost(String hostId, String metadataInterfaceName) {
        hostIdToMetadataInterfaceNameMapper.put(hostId, metadataInterfaceName);
    }
}
