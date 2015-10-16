package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Loads only modules of GBP and it's dependencies for data broker.
 * <br>Therefore this implementation is faster than {@link AbstractDataBrokerTest}
 */
public class GbpDataBrokerTest extends AbstractDataBrokerTest {

    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        Builder<YangModuleInfo> moduleInfoSet = ImmutableSet.<YangModuleInfo>builder();
        loadModuleInfos(Tenant.class, moduleInfoSet);
        return moduleInfoSet.build();
    }

    public static void loadModuleInfos(Class<?> clazzFromModule, Builder<YangModuleInfo> moduleInfoSet) throws Exception {
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
}
