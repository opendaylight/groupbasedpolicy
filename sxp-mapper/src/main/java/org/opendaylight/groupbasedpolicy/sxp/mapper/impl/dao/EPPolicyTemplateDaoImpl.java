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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgtKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sxp.database.rev160308.Sgt;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Purpose: general dao for EndPoint templates
 */
public class EPPolicyTemplateDaoImpl implements DSAsyncDao<Sgt, EndpointPolicyTemplateBySgt> {

    private final DataBroker dataBroker;
    private final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> cachedDao;

    public EPPolicyTemplateDaoImpl(final DataBroker dataBroker,
                                   final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> cachedDao) {
        this.dataBroker = dataBroker;
        this.cachedDao = cachedDao;
    }

    @Override
    public ListenableFuture<Optional<EndpointPolicyTemplateBySgt>> read(@Nonnull final Sgt key) {
        final Optional<EndpointPolicyTemplateBySgt> cachedEndpointPolicyTemplateBySgtalue = lookup(cachedDao, key);
        if (cachedEndpointPolicyTemplateBySgtalue.isPresent()) {
            return Futures.immediateFuture(cachedEndpointPolicyTemplateBySgtalue);
        } else {
            final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            final CheckedFuture<Optional<EndpointPolicyTemplateBySgt>, ReadFailedException> read =
                    rTx.read(LogicalDatastoreType.CONFIGURATION, buildReadPath(key));

            Futures.addCallback(read, SxpListenerUtil.createTxCloseCallback(rTx));

            return Futures.transform(read, new Function<Optional<EndpointPolicyTemplateBySgt>, Optional<EndpointPolicyTemplateBySgt>>() {
                @Nullable
                @Override
                public Optional<EndpointPolicyTemplateBySgt> apply(@Nullable final Optional<EndpointPolicyTemplateBySgt> input) {
                    if (input.isPresent()) {
                        cachedDao.update(key, input.get());
                    }
                    return input;
                }
            });
        }
    }

    protected InstanceIdentifier<EndpointPolicyTemplateBySgt> buildReadPath(final Sgt key) {
        return EPTemplateListener.SXP_MAPPER_TEMPLATE_PARENT_PATH
                .child(EndpointPolicyTemplateBySgt.class, new EndpointPolicyTemplateBySgtKey(key));
    }

    private Optional<EndpointPolicyTemplateBySgt> lookup(final SimpleCachedDao<Sgt, EndpointPolicyTemplateBySgt> cachedDao, final Sgt key) {
        return cachedDao.find(key);
    }

}
