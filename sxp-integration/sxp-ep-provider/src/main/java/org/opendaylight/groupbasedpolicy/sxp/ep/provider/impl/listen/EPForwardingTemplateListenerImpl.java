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
import java.util.Collection;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.EPTemplateListener;
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
    private final DSAsyncDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao;

    public EPForwardingTemplateListenerImpl(final DataBroker dataBroker,
                                            final SxpMapperReactor sxpMapperReactor,
                                            final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> templateCachedDao,
                                            final DSAsyncDao<IpPrefix, MasterDatabaseBinding> masterDBBindingDao,
                                            final DSAsyncDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.templateCachedDao = Preconditions.checkNotNull(templateCachedDao);
        this.masterDBBindingDao = Preconditions.checkNotNull(masterDBBindingDao);
        this.epPolicyTemplateDao = Preconditions.checkNotNull(epPolicyTemplateDao);
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
            if (epForwardingTemplate == null) {
                LOG.debug("EPForwarding template removed - NOOP {}", change.getRootNode().getDataBefore());
                //TODO: handle removal (update cache)
            } else {
                processWithEPTemplates(epForwardingTemplate);
            }
        }
    }

    private void processWithEPTemplates(final EndpointForwardingTemplateBySubnet epForwardingTemplate) {
        final ListenableFuture<Optional<MasterDatabaseBinding>> sxpMasterDbItemRead =
                masterDBBindingDao.read(epForwardingTemplate.getIpPrefix());

        // find all available epForwardingTemplates and pair those to sxpMasterDBBinding
        final ListenableFuture<Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>>> searchResult =
                Futures.transform(sxpMasterDbItemRead, createReadAndPairTemplateToBindingFunction(epForwardingTemplate));

        // invoke sxpMapperReactor.process for every valid combination of sxpMasterDBBinding, epPolicyTemplate, epForwardingTemplate
        final ListenableFuture<RpcResult<Void>> rpcResult =
                Futures.transform(searchResult, createProcessAllFunction(epForwardingTemplate));

        Futures.addCallback(rpcResult, ANY_RPC_FUTURE_CALLBACK);
    }

    private AsyncFunction<Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>>, RpcResult<Void>>
    createProcessAllFunction(final EndpointForwardingTemplateBySubnet epForwardingTemplate) {
        return new AsyncFunction<Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>>
            apply(final Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>> input) throws Exception {
                final ListenableFuture<RpcResult<Void>> result;
                if (! input.isPresent()) {
                    LOG.debug("no pair [epPolicyTemplate, ip-sgt-binding] available for ip-prefix: {}", epForwardingTemplate.getIpPrefix());
                    result = Futures.immediateFuture(
                            RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION,
                                            "no pair [epPolicyTemplate, ip-sgt-binding] available for ip-prefix " + epForwardingTemplate.getIpPrefix())
                                    .build());
                } else {
                    LOG.trace("processing epForwardingTemplate event for ip-prefix: {}", epForwardingTemplate.getIpPrefix());
                    final Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt> pair = input.get();
                    final MasterDatabaseBinding sxpMasterDBBinding = pair.getLeft();
                    final EndpointPolicyTemplateBySgt epPolicyTemplate = pair.getRight();
                    if (epPolicyTemplate != null && sxpMasterDBBinding != null) {
                        LOG.trace("processing epForwardingTemplate event with resolved sxpMasterDb entry and " +
                                        "epPolicyTemplate for sgt/ip-prefix: {}/{}",
                                sxpMasterDBBinding.getSecurityGroupTag(), sxpMasterDBBinding.getImplementedInterface());
                        result = sxpMapperReactor.processTemplatesAndSxpMasterDB(epPolicyTemplate, epForwardingTemplate, sxpMasterDBBinding);
                    } else {
                        LOG.debug("Skipped ep-forwarding-template processing");
                        result = Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
                    }
                }

                return result;
            }
        };
    }

    private AsyncFunction<Optional<MasterDatabaseBinding>, Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>>>
    createReadAndPairTemplateToBindingFunction(final EndpointForwardingTemplateBySubnet epFowardingTemplate) {
        return new AsyncFunction<Optional<MasterDatabaseBinding>, Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>>>() {
            @Override
            public ListenableFuture<Optional<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>>>
            apply(final Optional<MasterDatabaseBinding> input) throws Exception {
                final ListenableFuture<Pair<MasterDatabaseBinding, EndpointPolicyTemplateBySgt>> result;
                if (! input.isPresent()) {
                    LOG.debug("no sxpMasterDB entry available for ip-prefix: {}", epFowardingTemplate.getIpPrefix());
                    result = Futures.immediateFuture(null);
                } else {
                    LOG.trace("processing sxpMasterDB entry for ip-prefix: {}", epFowardingTemplate.getIpPrefix());
                    final MasterDatabaseBinding masterDBItem = input.get();
                    final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> epPolicyTemplateRead =
                            epPolicyTemplateDao.read(masterDBItem.getSecurityGroupTag());
                    result = EPTemplateUtil.wrapToPair(masterDBItem, epPolicyTemplateRead);
                }

                return EPTemplateUtil.wrapToOptional(result);
            }
        };
    }


    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", templatePath);
        listenerRegistration.close();
    }
}
