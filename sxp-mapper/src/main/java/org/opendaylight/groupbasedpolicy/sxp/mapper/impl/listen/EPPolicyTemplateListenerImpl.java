/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.listen;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.ReadableByKey;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.L3EPServiceUtil;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: listens to EP policy template and propagates change events for further processing
 */
public class EPPolicyTemplateListenerImpl implements EPTemplateListener<EndpointPolicyTemplateBySgt> {

    private static final Logger LOG = LoggerFactory.getLogger(EPPolicyTemplateListenerImpl.class);

    private static final FutureCallback<List<RpcResult<Void>>> RPC_RESULT_FUTURE_CALLBACK =
            L3EPServiceUtil.createFailureLoggingCallback("failed to apply epPolicyTemplate");

    private final ListenerRegistration<? extends EPTemplateListener> listenerRegistration;
    private final InstanceIdentifier<EndpointPolicyTemplateBySgt> templatePath;
    private final SxpMapperReactor sxpMapperReactor;
    private final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> templateCachedDao;
    private final ReadableByKey<Sgt, MasterDatabaseBinding> masterDBBindingDao;

    public EPPolicyTemplateListenerImpl(final DataBroker dataBroker,
                                        final SxpMapperReactor sxpMapperReactor,
                                        final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> templateCachedDao,
                                        final ReadableByKey<Sgt, MasterDatabaseBinding> masterDBBindingDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.templateCachedDao = Preconditions.checkNotNull(templateCachedDao);
        this.masterDBBindingDao = Preconditions.checkNotNull(masterDBBindingDao);
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

            final EndpointPolicyTemplateBySgt epPolicyTemplate = change.getRootNode().getDataAfter();
            processWithSxpMasterDB(changeKey, epPolicyTemplate);
        }
    }

    private void processWithSxpMasterDB(final Sgt changeKey, final EndpointPolicyTemplateBySgt epPolicyTemplate) {
        final ListenableFuture<Collection<MasterDatabaseBinding>> sxpMasterDbItemFuture = masterDBBindingDao.readBy(changeKey);
        final ListenableFuture<List<RpcResult<Void>>> rpcResult = Futures.transform(sxpMasterDbItemFuture, new AsyncFunction<Collection<MasterDatabaseBinding>, List<RpcResult<Void>>>() {
            @Override
            public ListenableFuture<List<RpcResult<Void>>> apply(final Collection<MasterDatabaseBinding> input) throws Exception {
                final ListenableFuture<List<RpcResult<Void>>> result;
                if (input == null || input.isEmpty()) {
                    LOG.debug("no epPolicyTemplate available from sgt: {}", changeKey);
                    result = Futures.immediateFuture(Collections.singletonList(
                            RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION,
                                            "no ip-sgt mapping in sxpMasterDB available for " + changeKey)
                                    .build()));
                } else {
                    LOG.trace("processing sxpMasterDB event and epPolicyTemplate for sgt: {}", changeKey);
                    List<ListenableFuture<RpcResult<Void>>> allResults = new ArrayList<>(input.size());
                    for (MasterDatabaseBinding masterDBItem : input) {
                        allResults.add(sxpMapperReactor.processPolicyAndSxpMasterDB(epPolicyTemplate, masterDBItem));
                    }
                    result = Futures.successfulAsList(allResults);
                }

                return result;
            }
        });

        Futures.addCallback(rpcResult, RPC_RESULT_FUTURE_CALLBACK);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", templatePath);
        listenerRegistration.close();
    }
}
