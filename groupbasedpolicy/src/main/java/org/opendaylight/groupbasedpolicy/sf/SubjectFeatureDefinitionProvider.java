/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On creation, puts known Subject Feature Definitions to config datastore; deletes them on #close()
 */
public class SubjectFeatureDefinitionProvider implements AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(SubjectFeatureDefinitionProvider.class);
    private final DataBroker dataProvider;
    // Cached transaction to prevent problems if data provider service will not be available during shutdown
    private WriteTransaction removeDefinitionsTransaction;

    /**
     * Puts known Subject Feature Definitions to operational datastore
     *
     * @param dataProvider DataBroker
     */
    public SubjectFeatureDefinitionProvider(DataBroker dataProvider) {
        this.dataProvider = dataProvider;
        putSubjectFeatureDefinitions();
    }

    @Override
    public void close() {
        deleteSubjectFeatureDefinitions();
    }

    private void putSubjectFeatureDefinitions() {
        // Write subject feature definitions
        WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();
        wt.put(CONFIGURATION, EtherTypeClassifierDefinition.IID, EtherTypeClassifierDefinition.DEFINITION);
        wt.put(CONFIGURATION, IpProtoClassifierDefinition.IID, IpProtoClassifierDefinition.DEFINITION);
        wt.put(CONFIGURATION, L4ClassifierDefinition.IID, L4ClassifierDefinition.DEFINITION);
        wt.put(CONFIGURATION, AllowActionDefinition.IID, AllowActionDefinition.DEFINITION);
        wt.put(CONFIGURATION, ChainActionDefinition.IID, ChainActionDefinition.DEFINITION);

        DataStoreHelper.submitToDs(wt);

        // Prepare transaction where subject feature definitions will be removed when closed
        removeDefinitionsTransaction = this.dataProvider.newWriteOnlyTransaction();
    }

    private void deleteSubjectFeatureDefinitions() {
        try {
            removeDefinitionsTransaction.delete(LogicalDatastoreType.CONFIGURATION, EtherTypeClassifierDefinition.IID);
            removeDefinitionsTransaction.delete(LogicalDatastoreType.CONFIGURATION, IpProtoClassifierDefinition.IID);
            removeDefinitionsTransaction.delete(LogicalDatastoreType.CONFIGURATION, L4ClassifierDefinition.IID);
            removeDefinitionsTransaction.delete(LogicalDatastoreType.CONFIGURATION, AllowActionDefinition.IID);
            removeDefinitionsTransaction.delete(LogicalDatastoreType.CONFIGURATION, ChainActionDefinition.IID);
            removeDefinitionsTransaction.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Transaction failed", e);
        }
    }

}
