/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.L3EPServiceUtil;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listens to EP forwarding template and propagates change events for further processing
 */
public class EPForwardingTemplateListenerImpl implements EPTemplateListener<EndpointForwardingTemplateBySubnet> {

    private static final Logger LOG = LoggerFactory.getLogger(EPForwardingTemplateListenerImpl.class);

    public static final FutureCallback<RpcResult<Void>> ANY_RPC_FUTURE_CALLBACK =
            L3EPServiceUtil.createFailureLoggingCallback("failed to read epForwardingTemplate");

    private final SxpMapperReactor sxpMapperReactor;
    private final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> templateCachedDao;
    private final DSAsyncDao<IpPrefix, MasterDatabaseBinding> masterDBBindingDao;
    private final ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    private final InstanceIdentifier<EndpointForwardingTemplateBySubnet> templatePath;

    public EPForwardingTemplateListenerImpl(final DataBroker dataBroker,
                                            final SxpMapperReactor sxpMapperReactor,
                                            final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> templateCachedDao,
                                            final DSAsyncDao<IpPrefix, MasterDatabaseBinding> masterDBBindingDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.templateCachedDao = Preconditions.checkNotNull(templateCachedDao);
        this.masterDBBindingDao = Preconditions.checkNotNull(masterDBBindingDao);
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

            final EndpointForwardingTemplateBySubnet epForwardingTemplate = change.getRootNode().getDataAfter();
            processWithSxpMasterDB(changeKey, epForwardingTemplate);
        }
    }

    private void processWithSxpMasterDB(final IpPrefix changeKey, final EndpointForwardingTemplateBySubnet epForwardingTemplate) {
        final ListenableFuture<Optional<MasterDatabaseBinding>> sxpMasterDbItemFuture = masterDBBindingDao.read(changeKey);

        final ListenableFuture<RpcResult<Void>> allRpcResult = Futures.transform(sxpMasterDbItemFuture, new AsyncFunction<Optional<MasterDatabaseBinding>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(final Optional<MasterDatabaseBinding> input) throws Exception {
                final ListenableFuture<RpcResult<Void>> rpcResult;
                if (input == null || !input.isPresent()) {
                    LOG.debug("no epForwardingTemplate available for sgt: {}", changeKey);
                    rpcResult = RpcResultBuilder.<Void>failed()
                            .withError(RpcError.ErrorType.APPLICATION,
                                    "no ip-sgt mapping in sxpMasterDB available for " + changeKey)
                            .buildFuture();
                } else {
                    LOG.trace("processing sxpMasterDB event and epForwardingTemplate for sgt: {}", changeKey);
                    rpcResult = sxpMapperReactor.processForwardingAndSxpMasterDB(epForwardingTemplate, input.get());
                }
                return rpcResult;
            }
        });

        Futures.addCallback(allRpcResult, ANY_RPC_FUTURE_CALLBACK);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", templatePath);
        listenerRegistration.close();
    }
}
