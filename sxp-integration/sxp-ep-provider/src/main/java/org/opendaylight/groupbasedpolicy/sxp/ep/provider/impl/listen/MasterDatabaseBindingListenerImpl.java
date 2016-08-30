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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.ep.provider.impl.MasterDatabaseBindingListener;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.sxp.domains.SxpDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * listens to sxp master database and propagates change events for further processing
 */
public class MasterDatabaseBindingListenerImpl implements MasterDatabaseBindingListener {

    private static final Logger LOG = LoggerFactory.getLogger(MasterDatabaseBindingListenerImpl.class);

    private static final FutureCallback<RpcResult<Void>> RPC_POLICY_RESULT_FUTURE_CALLBACK =
            L3EPServiceUtil.createFailureLoggingCallback("failed to read epPolicyTemplate");

    public static final FutureCallback<RpcResult<Void>> RPC_FW_RESULT_FUTURE_CALLBACK =
            L3EPServiceUtil.createFailureLoggingCallback("failed to read epForwardingTemplate");

    private final SxpMapperReactor sxpMapperReactor;
    private final SimpleCachedDao<IpPrefix, MasterDatabaseBinding> masterDBBindingDaoCached;
    private final DSAsyncDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao;
    private final DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao;

    private final ListenerRegistration<? extends ClusteredDataTreeChangeListener> listenerRegistration;
    private final InstanceIdentifier<MasterDatabaseBinding> sxpDbPath;

    public MasterDatabaseBindingListenerImpl(final DataBroker dataBroker,
                                             final SxpMapperReactor sxpMapperReactor,
                                             final SimpleCachedDao<IpPrefix, MasterDatabaseBinding> masterDBBindingDaoCached,
                                             final DSAsyncDao<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao,
                                             final DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.masterDBBindingDaoCached = Preconditions.checkNotNull(masterDBBindingDaoCached);
        this.epPolicyTemplateDao = Preconditions.checkNotNull(epPolicyTemplateDao);
        this.epForwardingTemplateDao = Preconditions.checkNotNull(epForwardingTemplateDao);

        //TODO: get exact sxp-node path from config (pointing to ise listener).. start listening later - when config appears
        sxpDbPath = MasterDatabaseBindingListener.SXP_TOPOLOGY_PATH
                .child(Node.class)
                .augmentation(SxpNodeIdentity.class)
                .child(SxpDomains.class)
                .child(SxpDomain.class)
                .child(MasterDatabase.class)
                .child(MasterDatabaseBinding.class);

        final DataTreeIdentifier<MasterDatabaseBinding> dataTreeIdentifier = new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, sxpDbPath);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIdentifier, this);
        LOG.debug("started listening to {}", sxpDbPath);
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<MasterDatabaseBinding>> collection) {
        for (DataTreeModification<MasterDatabaseBinding> change : collection) {
            LOG.trace("received modification: {} -> {}", change.getRootPath(), change.getRootNode().getModificationType());
            // update cached dao
            final MasterDatabaseBinding sxpMasterDBItem = change.getRootNode().getDataAfter();
            if (sxpMasterDBItem == null) {
                //TODO: cover sgt-ip mapping removal
                LOG.debug("ip-sgt mapping was removed - NOOP: {}",
                        change.getRootPath().getRootIdentifier().firstKeyOf(MasterDatabaseBinding.class));
            } else {
                final IpPrefix ipPrefixKey = sxpMasterDBItem.getIpPrefix();
                SxpListenerUtil.updateCachedDao(masterDBBindingDaoCached, ipPrefixKey, change);
                processWithEPTemplates(sxpMasterDBItem);
            }
        }
    }

    private void processWithEPTemplates(final MasterDatabaseBinding sxpMasterDBItem) {
        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> epPolicyTemplateFuture =
                epPolicyTemplateDao.read(sxpMasterDBItem.getSecurityGroupTag());

        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> epForwardingTemplateFuture =
                epForwardingTemplateDao.read(sxpMasterDBItem.getIpPrefix());

        final ListenableFuture<EPTemplateUtil.OptionalMutablePair<EndpointPolicyTemplateBySgt, EndpointForwardingTemplateBySubnet>> compositeRead
                = EPTemplateUtil.compositeRead(epPolicyTemplateFuture, epForwardingTemplateFuture);

        final ListenableFuture<RpcResult<Void>> rpcResult = Futures.transform(compositeRead,
                new AsyncFunction<EPTemplateUtil.OptionalMutablePair<EndpointPolicyTemplateBySgt, EndpointForwardingTemplateBySubnet>, RpcResult<Void>>() {
                    @Override
                    public ListenableFuture<RpcResult<Void>> apply(final EPTemplateUtil.OptionalMutablePair<EndpointPolicyTemplateBySgt, EndpointForwardingTemplateBySubnet> input) throws Exception {
                        final ListenableFuture<RpcResult<Void>> result;
                        if (input == null) {
                            LOG.debug("no ep*Templates available for sgt/ip-prefix: {}/{}",
                                    sxpMasterDBItem.getSecurityGroupTag(),
                                    sxpMasterDBItem.getIpPrefix());
                            result = RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION,
                                            "no ep-templates available for" + sxpMasterDBItem)
                                    .buildFuture();

                        } else if (!input.getLeft().isPresent()) {
                            LOG.debug("no epPolicyTemplate available for sgt: {}", sxpMasterDBItem.getSecurityGroupTag());
                            result = RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION,
                                            "no epPolicyTemplate available for " + sxpMasterDBItem)
                                    .buildFuture();
                        } else if (!input.getRight().isPresent()) {
                            LOG.debug("no epForwardingTemplate available for ip-prefix: {}",
                                    sxpMasterDBItem.getIpPrefix());
                            result = RpcResultBuilder.<Void>failed()
                                    .withError(RpcError.ErrorType.APPLICATION,
                                            "no epForwardingTemplate available for " + sxpMasterDBItem)
                                    .buildFuture();
                        } else {
                            LOG.trace("processing sxpMasterDB event and epPolicyTemplate for sgt/ip-prefix: {}/{}",
                                    sxpMasterDBItem.getSecurityGroupTag(),
                                    sxpMasterDBItem.getIpPrefix());
                            result = sxpMapperReactor.processTemplatesAndSxpMasterDB(input.getLeft().get(),
                                    input.getRight().get(), sxpMasterDBItem);
                }
                return result;
            }
        });

        Futures.addCallback(rpcResult, RPC_POLICY_RESULT_FUTURE_CALLBACK);
    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", sxpDbPath);
        listenerRegistration.close();
    }
}
