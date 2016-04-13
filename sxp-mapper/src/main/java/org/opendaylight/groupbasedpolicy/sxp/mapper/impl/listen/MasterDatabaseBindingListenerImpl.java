/*
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoAsync;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSDaoCached;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.MasterDatabaseBindingListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SxpMapperReactor;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.sxp.databases.fields.MasterDatabase;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * listens to sxp master database and propagates change events for further processing
 */
public class MasterDatabaseBindingListenerImpl implements MasterDatabaseBindingListener {

    private static final Logger LOG = LoggerFactory.getLogger(MasterDatabaseBindingListenerImpl.class);

    private final SxpMapperReactor sxpMapperReactor;
    private final DSDaoCached<Sgt, MasterDatabaseBinding> masterDBBindingDaoCached;
    private final DSDaoAsync<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao;
    private final DSDaoAsync<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao;

    private final ListenerRegistration<? extends DataTreeChangeListener> listenerRegistration;
    private final InstanceIdentifier<MasterDatabaseBinding> sxpDbPath;

    public MasterDatabaseBindingListenerImpl(final DataBroker dataBroker,
                                             final SxpMapperReactor sxpMapperReactor,
                                             final DSDaoCached<Sgt, MasterDatabaseBinding> masterDBBindingDaoCached,
                                             final DSDaoAsync<Sgt, EndpointPolicyTemplateBySgt> epPolicyTemplateDao,
                                             final DSDaoAsync<IpPrefix, EndpointForwardingTemplateBySubnet> epForwardingTemplateDao) {
        this.sxpMapperReactor = Preconditions.checkNotNull(sxpMapperReactor);
        this.masterDBBindingDaoCached = Preconditions.checkNotNull(masterDBBindingDaoCached);
        this.epPolicyTemplateDao = Preconditions.checkNotNull(epPolicyTemplateDao);
        this.epForwardingTemplateDao = Preconditions.checkNotNull(epForwardingTemplateDao);
        sxpDbPath = MasterDatabaseBindingListener.SXP_TOPOLOGY_PATH
                .child(Node.class)
                .augmentation(SxpNodeIdentity.class)
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
            } else {
                final Sgt sgtKey = sxpMasterDBItem.getSecurityGroupTag();
                final IpPrefix ipPrefixKey = sxpMasterDBItem.getIpPrefix();
                SxpListenerUtil.updateCachedDao(masterDBBindingDaoCached, sgtKey, change);

                processWithEPPolicyTemplate(sgtKey, sxpMasterDBItem);
                processWithEPForwardingTemplate(ipPrefixKey, sxpMasterDBItem);
            }
        }
    }

    private void processWithEPForwardingTemplate(final IpPrefix changeKey, final MasterDatabaseBinding sxpMasterDBItem) {
        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> epForwardingTemplateFuture =
                epForwardingTemplateDao.read(changeKey);

        final ListenableFuture<RpcResult<Void>> rpcResult = Futures.transform(epForwardingTemplateFuture, new AsyncFunction<Optional<EndpointForwardingTemplateBySubnet>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(final Optional<EndpointForwardingTemplateBySubnet> input) throws Exception {
                if (input == null || !input.isPresent()) {
                    LOG.debug("no epForwardingTemplate available for ipPrefix: {}", changeKey);
                    throw new IllegalArgumentException("no epForwardingTemplate available");
                } else {
                    // TODO: invoke reactor
                    return null;
                }
            }
        });
    }

    private void processWithEPPolicyTemplate(final Sgt changeKey, final MasterDatabaseBinding sxpMasterDBItem) {
        final ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> epPolicyTemplateFuture =
                epPolicyTemplateDao.read(changeKey);

        final ListenableFuture<RpcResult<Void>> rpcResult = Futures.transform(epPolicyTemplateFuture, new AsyncFunction<Optional<EndpointPolicyTemplateBySgt>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(final Optional<EndpointPolicyTemplateBySgt> input) throws Exception {
                if (input == null || !input.isPresent()) {
                    LOG.debug("no epPolicyTemplate available for sgt: {}", changeKey);
                    throw new IllegalArgumentException("no epPolicyTemplate available");
                } else {
                    LOG.trace("processing sxpMasterDB event and epPolicyTemplate for sgt: {}", changeKey);
                    // TODO: invoke reactor
                    return null;
                }
            }
        });

    }

    @Override
    public void close() throws Exception {
        LOG.debug("closing listener registration to {}", sxpDbPath);
        listenerRegistration.close();
    }
}
