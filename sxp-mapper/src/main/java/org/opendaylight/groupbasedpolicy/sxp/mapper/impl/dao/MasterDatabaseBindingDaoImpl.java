/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.MasterDatabaseBindingListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.ReadableAsyncByKey;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.MasterDatabaseFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.master.database.fields.MasterDatabaseBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpDatabasesFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.SxpNodeIdentity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.node.rev160308.network.topology.topology.node.SxpDomains;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: general dao for EndPoint templates
 */
public class MasterDatabaseBindingDaoImpl implements DSAsyncDao<IpPrefix, MasterDatabaseBinding>,
        ReadableAsyncByKey<Sgt, MasterDatabaseBinding> {

    private static final Logger LOG = LoggerFactory.getLogger(MasterDatabaseBindingDaoImpl.class);
    private static final ListenableFuture<Optional<MasterDatabaseBinding>> READ_FUTURE_ABSENT = Futures.immediateFuture(Optional.absent());

    private final DataBroker dataBroker;
    private final SimpleCachedDao<IpPrefix, MasterDatabaseBinding> cachedDao;

    public MasterDatabaseBindingDaoImpl(final DataBroker dataBroker,
                              final SimpleCachedDao<IpPrefix, MasterDatabaseBinding> cachedDao) {
        this.dataBroker = dataBroker;
        this.cachedDao = cachedDao;
    }

    @Override
    public ListenableFuture<Optional<MasterDatabaseBinding>> read(@Nonnull final IpPrefix key) {
        final Optional<MasterDatabaseBinding> cachedMasterDatabaseBinding = lookup(cachedDao, key);
        if (cachedMasterDatabaseBinding.isPresent()) {
            return Futures.immediateFuture(cachedMasterDatabaseBinding);
        } else if (!cachedDao.isEmpty()) {
            return READ_FUTURE_ABSENT;
        } else {
            final ListenableFuture<Void> cacheUpdatedFt = updateCache();

            return Futures.transform(cacheUpdatedFt, new Function<Void, Optional<MasterDatabaseBinding>>() {
                @Nullable
                @Override
                public Optional<MasterDatabaseBinding> apply(@Nullable final Void input) {
                    return lookup(cachedDao, key);
                }
            });
        }
    }

    private ListenableFuture<Void> updateCache() {
        final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<Topology>, ReadFailedException> read =
                rTx.read(LogicalDatastoreType.CONFIGURATION, buildReadPath(null));

        Futures.addCallback(read, SxpListenerUtil.createTxCloseCallback(rTx));

        return Futures.transform(read, new Function<Optional<Topology>, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable final Optional<Topology> input) {
                if (input.isPresent()) {
                    // clean cache
                    cachedDao.invalidateCache();

                    for (Node node : input.get().getNode()) {
                        java.util.Optional.ofNullable(node.getAugmentation(SxpNodeIdentity.class))
                                .map(SxpNodeIdentity::getSxpDomains)
                                .map(SxpDomains::getSxpDomain)
                                .ifPresent((sxpDomain) -> {
                                    final List<MasterDatabaseBinding> masterDBBindings = sxpDomain.stream()
                                            .map(SxpDatabasesFields::getMasterDatabase)
                                            .filter(masterDb -> masterDb != null)
                                            .map(MasterDatabaseFields::getMasterDatabaseBinding)
                                            .filter(binding -> binding != null)
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toList());

                                    for (MasterDatabaseBinding masterDBItem : masterDBBindings) {
                                        // update all
                                        final MasterDatabaseBinding
                                                previousValue =
                                                cachedDao.update(masterDBItem.getIpPrefix(), masterDBItem);
                                        if (previousValue != null) {
                                            LOG.warn("updated key already obtained: [node:{}, sgt:{}]",
                                                    node.getNodeId().getValue(), masterDBItem.getSecurityGroupTag());
                                        }
                                    }
                                });
                    }
                } else {
                    LOG.warn("failed to update cache of SxpMasterDB - no data");
                }
                return null;
            }
        });
    }

    private InstanceIdentifier<Topology> buildReadPath(final Sgt key) {
        return MasterDatabaseBindingListener.SXP_TOPOLOGY_PATH;
    }

    private Optional<MasterDatabaseBinding> lookup(final SimpleCachedDao<IpPrefix, MasterDatabaseBinding> cachedDao, final IpPrefix key) {
        return cachedDao.find(key);
    }

    @Override
    public ListenableFuture<Collection<MasterDatabaseBinding>> readBy(@Nonnull final Sgt specialKey) {
        final ListenableFuture<Void> cacheUpdatedFt;
        if (!cachedDao.isEmpty()) {
            cacheUpdatedFt = Futures.immediateFuture(null);
        } else {
            cacheUpdatedFt = updateCache();
        }

        return Futures.transform(cacheUpdatedFt, new Function<Void, Collection<MasterDatabaseBinding>>() {
            @Nullable
            @Override
            public Collection<MasterDatabaseBinding> apply(@Nullable final Void input) {
                final Collection<MasterDatabaseBinding> foundGroups = new ArrayList<>();

                for (MasterDatabaseBinding masterDBItem : cachedDao.values()) {
                    if (masterDBItem.getSecurityGroupTag().equals(specialKey)) {
                        foundGroups.add(masterDBItem);
                    }
                }
                return foundGroups;
            }
        });
    }
}
