package org.opendaylight.controller.config.yang.config.groupbasedpolicy;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.sf.SubjectFeatureDefinitionProvider;
import org.opendaylight.groupbasedpolicy.sf.SupportedActionDefinitionListener;
import org.opendaylight.groupbasedpolicy.sf.SupportedClassifierDefinitionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupbasedpolicyModule extends org.opendaylight.controller.config.yang.config.groupbasedpolicy.AbstractGroupbasedpolicyModule {

    private static final Logger LOG = LoggerFactory.getLogger(GroupbasedpolicyModule.class);

    public GroupbasedpolicyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public GroupbasedpolicyModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.groupbasedpolicy.GroupbasedpolicyModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    /**
     * On module start, creates SubjectFeatureDefinitionProvider instance, in order to put known Subject Feature Definitions to operational Datastore
     *
     * @return SubjectFeatureDefinitionProvider
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataProvider = Preconditions.checkNotNull(getDataBrokerDependency());
        try {
            return new AutoCloseable() {

                SubjectFeatureDefinitionProvider sfdp = new SubjectFeatureDefinitionProvider(dataProvider);
                SupportedClassifierDefinitionListener supportedClassifierDefinitionListener =
                        new SupportedClassifierDefinitionListener(dataProvider);
                SupportedActionDefinitionListener supportedActionDefinitionListener =
                        new SupportedActionDefinitionListener(dataProvider);
                @Override
                public void close() throws Exception {
                    sfdp.close();
                    supportedClassifierDefinitionListener.close();
                    supportedActionDefinitionListener.close();
                }
            };
        } catch (TransactionCommitFailedException e) {
            LOG.error(
                    "Error creating instance of SubjectFeatureDefinitionProvider; Subject Feature Definitions were not put to Datastore");
            throw new RuntimeException(e);
        }
    }

}
