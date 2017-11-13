/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import com.google.common.base.Strings;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.DataTreeChangeHandler;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.NeutronByGbpMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.ProviderPhysicalNetworksAsL2FloodDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.neutron.gbp.mapper.rev150513.mappings.neutron.by.gbp.mappings.provider.physical.networks.as.l2.flood.domains.ProviderPhysicalNetworkAsL2FloodDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.Segmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.SegmentationBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderPhysicalNetworkListener extends DataTreeChangeHandler<ProviderPhysicalNetworkAsL2FloodDomain> {

    private static final Logger LOG = LoggerFactory.getLogger(ProviderPhysicalNetworkListener.class);

    protected ProviderPhysicalNetworkListener(DataBroker dataProvider) {
        super(dataProvider);
        registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifier.builder(Mappings.class)
                            .child(NeutronByGbpMappings.class)
                            .child(ProviderPhysicalNetworksAsL2FloodDomains.class)
                            .child(ProviderPhysicalNetworkAsL2FloodDomain.class)
                            .build()));
    }

    @Override
    protected void onWrite(DataObjectModification<ProviderPhysicalNetworkAsL2FloodDomain> rootNode,
            InstanceIdentifier<ProviderPhysicalNetworkAsL2FloodDomain> rootIdentifier) {
        ProviderPhysicalNetworkAsL2FloodDomain dataAfter = rootNode.getDataAfter();
        if (dataAfter != null) {
            L2FloodDomainId l2FdId = dataAfter.getL2FloodDomainId();
            TenantId tenantId = dataAfter.getTenantId();
            String segmentationId = dataAfter.getSegmentationId();
            augmentSegmentationToFloodDomain(tenantId, l2FdId, segmentationId);
        }
    }

    @Override
    protected void onDelete(DataObjectModification<ProviderPhysicalNetworkAsL2FloodDomain> rootNode,
            InstanceIdentifier<ProviderPhysicalNetworkAsL2FloodDomain> rootIdentifier) {
        ProviderPhysicalNetworkAsL2FloodDomain dataBefore = rootNode.getDataBefore();
        if (dataBefore != null) {
            L2FloodDomainId l2FdId = dataBefore.getL2FloodDomainId();
            TenantId tenantId = dataBefore.getTenantId();
            augmentSegmentationToFloodDomain(tenantId, l2FdId, null);
        }
    }

    @Override
    protected void onSubtreeModified(DataObjectModification<ProviderPhysicalNetworkAsL2FloodDomain> rootNode,
            InstanceIdentifier<ProviderPhysicalNetworkAsL2FloodDomain> rootIdentifier) {
        ProviderPhysicalNetworkAsL2FloodDomain dataAfter = rootNode.getDataAfter();
        if (dataAfter != null) {
            L2FloodDomainId l2FdId = dataAfter.getL2FloodDomainId();
            TenantId tenantId = dataAfter.getTenantId();
            String segmentationId = dataAfter.getSegmentationId();
            augmentSegmentationToFloodDomain(tenantId, l2FdId, segmentationId);
        }
    }

    private void augmentSegmentationToFloodDomain(TenantId tenantId, L2FloodDomainId l2FdId, String segmentationId) {
        if (Strings.isNullOrEmpty(segmentationId)) {
            ReadWriteTransaction rwTx = dataProvider.newReadWriteTransaction();
            DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.l2FloodDomainIid(tenantId, l2FdId).augmentation(Segmentation.class), rwTx);
            DataStoreHelper.submitToDs(rwTx);
        } else {
            try {
                Segmentation segmentation =
                        new SegmentationBuilder().setSegmentationId(Integer.valueOf(segmentationId)).build();
                WriteTransaction writeTx = dataProvider.newWriteOnlyTransaction();
                writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                        IidFactory.l2FloodDomainIid(tenantId, l2FdId).augmentation(Segmentation.class), segmentation);
                DataStoreHelper.submitToDs(writeTx);
            } catch (NumberFormatException e) {
                LOG.info("Segmentation ID of Neutron Provider Physical Network {} is not a number but is {}.",
                        l2FdId.getValue(), segmentationId);
            }
        }
    }

}
