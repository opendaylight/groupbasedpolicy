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
import org.opendaylight.groupbasedpolicy.sf.actions.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.sf.actions.ChainActionDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.sf.classifiers.L4ClassifierDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On creation, puts known Subject Feature Definitions to operational datastore; deletes them on #close()
 */
public class SubjectFeatureDefinitionProvider implements AutoCloseable {
    private static final Logger LOG =
            LoggerFactory.getLogger(SubjectFeatureDefinitionProvider.class);

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

        wt.put(LogicalDatastoreType.OPERATIONAL, EtherTypeClassifierDefinition.IID,
                EtherTypeClassifierDefinition.DEFINITION);
        wt.put(LogicalDatastoreType.OPERATIONAL, IpProtoClassifierDefinition.IID,
                IpProtoClassifierDefinition.DEFINITION);
        wt.put(LogicalDatastoreType.OPERATIONAL, L4ClassifierDefinition.IID,
                L4ClassifierDefinition.DEFINITION);

        wt.put(LogicalDatastoreType.OPERATIONAL, AllowActionDefinition.IID,
                AllowActionDefinition.DEFINITION);
        wt.put(LogicalDatastoreType.OPERATIONAL, ChainActionDefinition.IID,
                ChainActionDefinition.DEFINITION);

        wt.submit().checkedGet();
    }

    private void deleteSubjectFeatureDefinitions() throws TransactionCommitFailedException {
        WriteTransaction wt = this.dataProvider.newWriteOnlyTransaction();

        wt.delete(LogicalDatastoreType.OPERATIONAL, EtherTypeClassifierDefinition.IID);
        wt.delete(LogicalDatastoreType.OPERATIONAL, IpProtoClassifierDefinition.IID);
        wt.delete(LogicalDatastoreType.OPERATIONAL, L4ClassifierDefinition.IID);

        wt.delete(LogicalDatastoreType.OPERATIONAL, AllowActionDefinition.IID);
        wt.delete(LogicalDatastoreType.OPERATIONAL, ChainActionDefinition.IID);

        wt.submit().checkedGet();
    }

}
