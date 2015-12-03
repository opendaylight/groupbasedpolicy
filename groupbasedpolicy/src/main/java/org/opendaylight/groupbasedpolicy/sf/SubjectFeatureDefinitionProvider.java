/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.sf;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;

/**
 * On creation, puts known Subject Feature Definitions to config datastore; deletes them on #close()
 */
public class SubjectFeatureDefinitionProvider implements AutoCloseable {

    private final DataBroker dataProvider;

    /**
     * Puts known Subject Feature Definitions to operational datastore
     *
     * @param dataProvider DataBroker
     * @throws TransactionCommitFailedException
     */
    public SubjectFeatureDefinitionProvider(DataBroker dataProvider)
            throws TransactionCommitFailedException {
        this.dataProvider = dataProvider;
        putSubjectFeatureDefinitions();
    }

    @Override
    public void close() throws Exception {
        deleteSubjectFeatureDefinitions();
    }

    private void putSubjectFeatureDefinitions() throws TransactionCommitFailedException {
        WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();

        wt.put(LogicalDatastoreType.CONFIGURATION, EtherTypeClassifierDefinition.IID,
                EtherTypeClassifierDefinition.DEFINITION);
        wt.put(LogicalDatastoreType.CONFIGURATION, IpProtoClassifierDefinition.IID,
                IpProtoClassifierDefinition.DEFINITION);
        wt.put(LogicalDatastoreType.CONFIGURATION, L4ClassifierDefinition.IID,
                L4ClassifierDefinition.DEFINITION);

        wt.put(LogicalDatastoreType.CONFIGURATION, AllowActionDefinition.IID,
                AllowActionDefinition.DEFINITION);
        wt.put(LogicalDatastoreType.CONFIGURATION, ChainActionDefinition.IID,
                ChainActionDefinition.DEFINITION);

        wt.submit().checkedGet();
    }

    private void deleteSubjectFeatureDefinitions() throws TransactionCommitFailedException {
        WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();

        wt.delete(LogicalDatastoreType.CONFIGURATION, EtherTypeClassifierDefinition.IID);
        wt.delete(LogicalDatastoreType.CONFIGURATION, IpProtoClassifierDefinition.IID);
        wt.delete(LogicalDatastoreType.CONFIGURATION, L4ClassifierDefinition.IID);

        wt.delete(LogicalDatastoreType.CONFIGURATION, AllowActionDefinition.IID);
        wt.delete(LogicalDatastoreType.CONFIGURATION, ChainActionDefinition.IID);

        wt.submit().checkedGet();
    }

}
