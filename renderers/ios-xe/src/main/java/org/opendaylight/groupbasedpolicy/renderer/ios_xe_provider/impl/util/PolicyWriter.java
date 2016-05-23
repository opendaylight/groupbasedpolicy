/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMap;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ClassMapKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.policy.map.Class;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PolicyWriter {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWriter.class);

    private final DataBroker mountpoint;
    // Local cache
    private List<ClassMap> classMapEntries;
    private List<Class> policyMapEntries;

    public PolicyWriter(final DataBroker dataBroker) {
        mountpoint = Preconditions.checkNotNull(dataBroker);
        classMapEntries = new ArrayList<>();
        policyMapEntries = new ArrayList<>();
    }

    public void write(ClassMap classMap) {
        classMapEntries.add(classMap);
    }

    public void write(List<Class> policyMapEntries) {
        this.policyMapEntries.addAll(policyMapEntries);
    }

    public void commitToDatastore() {
        // create and write service-policy
        // create and write policy-map with policyMapEntries
        // create and write class-maps

        WriteTransaction wtx = mountpoint.newWriteOnlyTransaction();
        // Class maps
        for (ClassMap entry : classMapEntries) {
            InstanceIdentifier<ClassMap> iid = classMapInstanceIdentifier(entry);
            try {
                wtx.merge(LogicalDatastoreType.CONFIGURATION, iid, entry, true);
                CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wtx.submit();
                submitFuture.checkedGet();
                // Clear cache
                classMapEntries.clear();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Write transaction failed to {}", e.getMessage());
            } catch (Exception e) {
                LOG.error("Failed to .. {}", e.getMessage());
            }
        }
    }

    private InstanceIdentifier<ClassMap> classMapInstanceIdentifier(ClassMap classMap) {
        return InstanceIdentifier.builder(Native.class)
                .child(ClassMap.class, new ClassMapKey(classMap.getName())).build();
    }
}
