/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Loads only modules of GBP and it's dependencies for data broker.
 * <br>
 * Therefore this implementation is faster than {@link AbstractDataBrokerTest}
 */
public abstract class CustomDataBrokerTest extends AbstractDataBrokerTest {

    public static void loadModuleInfos(Class<?> clazzFromModule, Builder<YangModuleInfo> moduleInfoSet)
            throws Exception {
        YangModuleInfo moduleInfo = BindingReflections.getModuleInfo(clazzFromModule);
        checkState(moduleInfo != null, "Module Info for %s is not available.", clazzFromModule);
        collectYangModuleInfo(moduleInfo, moduleInfoSet);
    }

    private static void collectYangModuleInfo(final YangModuleInfo moduleInfo,
            final Builder<YangModuleInfo> moduleInfoSet) throws IOException {
        moduleInfoSet.add(moduleInfo);
        for (YangModuleInfo dependency : moduleInfo.getImportedModules()) {
            collectYangModuleInfo(dependency, moduleInfoSet);
        }
    }

    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        Builder<YangModuleInfo> moduleInfoSet = ImmutableSet.<YangModuleInfo>builder();
        for (Class<?> clazz : getClassesFromModules()) {
            loadModuleInfos(clazz, moduleInfoSet);
        }
        return moduleInfoSet.build();
    }

    /**
     * @return a class from every yang module which needs to be loaded. Cannot return {@code null}
     *         or empty collection.
     */
    public abstract @Nonnull Collection<Class<?>> getClassesFromModules();
}
