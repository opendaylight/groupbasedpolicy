/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoCached;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listens to EP forwarding template and propagates change events for further processing
 */
public class EPForwardingTemplateListenerImpl implements EPTemplateListener<EndpointForwardingTemplateBySubnet> {

    private static final Logger LOG = LoggerFactory.getLogger(EPForwardingTemplateListenerImpl.class);
    private final SxpMapperReactor sxpMapperReactor;
    private final DSDaoCached<IpPrefix, EndpointForwardingTemplateBySubnet> templateCachedDao;
    private final ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    private final InstanceIdentifier<EndpointForwardingTemplateBySubnet> templatePath;

    public EPForwardingTemplateListenerImpl(final DataBroker dataBroker,
                                            final SxpMapperReactor sxpMapperReactor,
                                            final DSDaoCached<IpPrefix, EndpointForwardingTemplateBySubnet> templateCachedDao) {
        this.sxpMapperReactor = sxpMapperReactor;
        this.templateCachedDao = templateCachedDao;
        templatePath = EPTemplateListener.SXP_MAPPER_TEMPLATE_PARENT_PATH.child(EndpointForwardingTemplateBySubnet.class);

        final DataTreeIdentifier<EndpointForwardingTemplateBySubnet> dataTreeIdentifier = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, templatePath);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIdentifier, this);
        LOG.debug("started listening to {}", templatePath);
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<EndpointForwardingTemplateBySubnet>> collection) {
        for (DataTreeModification<EndpointForwardingTemplateBySubnet> change : collection) {
            LOG.trace("received modification: {} -> {}", change.getRootPath(), change.getRootNode().getModificationType());
            // update cached dao
            final InstanceIdentifier<EndpointForwardingTemplateBySubnet> changePath = change.getRootPath().getRootIdentifier();
            final IpPrefix changeKey = changePath.firstKeyOf(EndpointForwardingTemplateBySubnet.class).getIpPrefix();
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
