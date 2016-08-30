/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.listen;

import com.google.common.base.Optional;
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
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.ReadableAsyncByKey;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.EPTemplateUtil;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.L3EPServiceUtil;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.integration.sxp.ep.provider.model.rev160302.sxp.ep.mapper.EndpointPolicyTemplateBySgt;
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
    private final ReadableAsyncByKey<Sgt, MasterDatabaseBinding> masterDBBindingDao;
    private final DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao;

    public EPPolicyTemplateListenerImpl(final DataBroker dataBroker,
                                        final SxpMapperReactor sxpMapperReactor,
                                        final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> templateCachedDao,
                                        final ReadableAsyncByKey<Sgt, MasterDatabaseBinding> masterDBBindingDao,
                                        final DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.templateCachedDao = Preconditions.checkNotNull(templateCachedDao);
        this.masterDBBindingDao = Preconditions.checkNotNull(masterDBBindingDao);
        this.epForwardingTemplateDao = Preconditions.checkNotNull(epForwardingTemplateDao);
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
            processWithEPTemplates(epPolicyTemplate);
        }
    }

    private void processWithEPTemplates(final EndpointPolicyTemplateBySgt epPolicyTemplate) {
        final ListenableFuture<Collection<MasterDatabaseBinding>> sxpMasterDbItemsRead =
                masterDBBindingDao.readBy(epPolicyTemplate.getSgt());

        // find all available epForwardingTemplates and pair those to sxpMasterDBBinding
        final ListenableFuture<List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>> epForwardingTemplatesRead =
                Futures.transform(sxpMasterDbItemsRead, createReadAndPairTemplateToBindingFunction(epPolicyTemplate));

        // invoke sxpMapperReactor.process for every valid combination of sxpMasterDBBinding, epPolicyTemplate, epForwardingTemplate
        final ListenableFuture<List<RpcResult<Void>>> rpcResult =
                Futures.transform(epForwardingTemplatesRead, createProcessAllFunction(epPolicyTemplate));

        Futures.addCallback(rpcResult, RPC_RESULT_FUTURE_CALLBACK);
    }

    private AsyncFunction<List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>, List<RpcResult<Void>>>
    createProcessAllFunction(final EndpointPolicyTemplateBySgt epPolicyTemplate) {
        return new AsyncFunction<List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>, List<RpcResult<Void>>>() {
            @Override
            public ListenableFuture<List<RpcResult<Void>>>
            apply(final List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>> input) throws Exception {
                final ListenableFuture<List<RpcResult<Void>>> result;
                if (input == null || input.isEmpty()) {
                    LOG.debug("no epForwardingTemplate available for sgt: {}", epPolicyTemplate.getSgt());
                    result = Futures.immediateFuture(Collections.singletonList(
                            RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION,
                                            "no epForwardingTemplate available for sgt " + epPolicyTemplate.getSgt())
                                    .build()));
                } else {
                    LOG.trace("processing epPolicyTemplate event for sgt: {}", epPolicyTemplate.getSgt());
                    List<ListenableFuture<RpcResult<Void>>> allResults = new ArrayList<>(input.size());
                    for (Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet> pair : input) {
                        final MasterDatabaseBinding sxpMasterDBBinding = pair.getLeft();
                        final EndpointForwardingTemplateBySubnet epForwardingTemplate = pair.getRight();
                        if (epForwardingTemplate != null) {
                            LOG.trace("processing epPolicyTemplate event with resolved sxpMasterDb entry and " +
                                            "epForwardingTemplate for sgt/ip-prefix: {}/{}",
                                    sxpMasterDBBinding.getSecurityGroupTag(), sxpMasterDBBinding.getImplementedInterface());
                            allResults.add(sxpMapperReactor.processTemplatesAndSxpMasterDB(
                                    epPolicyTemplate, epForwardingTemplate, sxpMasterDBBinding));
                        }
                    }
                    result = Futures.successfulAsList(allResults);
                }

                return result;
            }
};
    }

    private AsyncFunction<Collection<MasterDatabaseBinding>, List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>>
    createReadAndPairTemplateToBindingFunction(final EndpointPolicyTemplateBySgt epPolicyTemplate) {
        return new AsyncFunction<Collection<MasterDatabaseBinding>, List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>>() {
            @Override
            public ListenableFuture<List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>>
            apply(final Collection<MasterDatabaseBinding> input) throws Exception {
                final ListenableFuture<List<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>> result;
                if (input == null || input.isEmpty()) {
                    LOG.debug("no sxpMasterDB entry available for sgt: {}", epPolicyTemplate.getSgt());
                    result = Futures.immediateFuture(Collections.emptyList());
                } else {
                    LOG.trace("processing sxpMasterDB entries for sgt: {}", epPolicyTemplate.getSgt());
                    List<ListenableFuture<Pair<MasterDatabaseBinding, EndpointForwardingTemplateBySubnet>>> allResults =
                            new ArrayList<>(input.size());
                    for (MasterDatabaseBinding masterDBItem : input) {
                        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> epForwardingTemplateRead =
                                epForwardingTemplateDao.read(masterDBItem.getIpPrefix());
                        allResults.add(EPTemplateUtil.wrapToPair(masterDBItem, epForwardingTemplateRead));
                    }
                    result = Futures.successfulAsList(allResults);
                }

                return result;
            }
        };
    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", templatePath);
        listenerRegistration.close();
    }
}
