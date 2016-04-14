/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoAsync;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoCached;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listens to EP policy template and propagates change events for further processing
 */
public class EPPolicyTemplateListenerImpl implements EPTemplateListener<EndpointPolicyTemplateBySgt> {

    private static final Logger LOG = LoggerFactory.getLogger(EPPolicyTemplateListenerImpl.class);
    private final ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    private final InstanceIdentifier<EndpointPolicyTemplateBySgt> templatePath;
    private final SxpMapperReactor sxpMapperReactor;
    private final DSDaoCached<Sgt, EndpointPolicyTemplateBySgt> templateCachedDao;
    private final DSDaoAsync<IpPrefix, MasterDatabaseBinding> masterDBBindingDao;
    private final DSDaoAsync<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao;

    public EPPolicyTemplateListenerImpl(final DataBroker dataBroker,
                                        final SxpMapperReactor sxpMapperReactor,
                                        final DSDaoCached<Sgt, EndpointPolicyTemplateBySgt> templateCachedDao,
                                        final DSDaoAsync<IpPrefix, MasterDatabaseBinding> masterDBBindingDao,
                                        final DSDaoAsync<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.templateCachedDao = Preconditions.checkNotNull(templateCachedDao);
        this.masterDBBindingDao = Preconditions.checkNotNull(masterDBBindingDao);
        this.epForwardingTemplateDao = epForwardingTemplateDao;
        templatePath = EPTemplateListener.SXP_MAPPER_TEMPLATE_PARENT_PATH.child(EndpointPolicyTemplateBySgt.class);

        final DataTreeIdentifier<EndpointPolicyTemplateBySgt> dataTreeIdentifier = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, templatePath);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIdentifier, this);
        LOG.debug("started listening to {}", templatePath);
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<EndpointPolicyTemplateBySgt>> collection) {
        for (DataTreeModification<EndpointPolicyTemplateBySgt> change : collection) {
            LOG.trace("received modification: {} -> {}", change.getRootPath(), change.getRootNode().getModificationType());
            // update cached dao
            final InstanceIdentifier<EndpointPolicyTemplateBySgt> changePath = change.getRootPath().getRootIdentifier();
            final Sgt changeKey = changePath.firstKeyOf(EndpointPolicyTemplateBySgt.class).getSgt();
            SxpListenerUtil.updateCachedDao(templateCachedDao, changeKey, change);

            // TODO: sxpMapperReactor.process(template)
        }
    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", templatePath);
        listenerRegistration.close();
    }
}
