/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class CloseOnFailTransactionChain implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(CloseOnFailTransactionChain.class);
    private final String owner;

    public CloseOnFailTransactionChain(@Nonnull String owner) {
        this.owner = Preconditions.checkNotNull(owner);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.info("Transaction chain owned by {} was successful. {}", owner, chain);
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
            Throwable cause) {
        LOG.warn("Transaction chain owned by {} failed. Transaction which caused the chain to fail {}", owner,
                transaction, cause);
        chain.close();
    }

}
