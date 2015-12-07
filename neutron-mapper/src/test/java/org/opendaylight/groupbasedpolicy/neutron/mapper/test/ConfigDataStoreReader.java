package org.opendaylight.groupbasedpolicy.neutron.mapper.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.Tenant;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.Contract;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.EndpointGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.contract.Clause;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.endpoint.group.ConsumerNamedSelector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ActionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.policy.subject.feature.instances.ClassifierInstance;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public final class ConfigDataStoreReader {

    private ConfigDataStoreReader() {
        throw new UnsupportedOperationException("Cannot create an instance");
    }

    public static Optional<Tenant> readTenant(DataBroker dataBroker, String tenantId) throws Exception {
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.tenantIid(new TenantId(tenantId)), rTx);
        }
    }

    public static Optional<Contract> readContract(DataBroker dataBroker, String tenantId, String contractId)
            throws Exception {
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.contractIid(new TenantId(tenantId), new ContractId(contractId)), rTx);
        }
    }

    public static Optional<EndpointGroup> readEndpointGroup(DataBroker dataBroker, String tenantId,
            String endpointGroupId) throws Exception {
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION,
                    IidFactory.endpointGroupIid(new TenantId(tenantId), new EndpointGroupId(endpointGroupId)), rTx);
        }
    }

    public static Optional<ClassifierInstance> readClassifierInstance(DataBroker dataBroker, String tenantId,
            ClassifierName classifierName) throws Exception {
        InstanceIdentifier<ClassifierInstance> clsfInstanceIid = IidFactory.classifierInstanceIid(
                new TenantId(tenantId), classifierName);
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, clsfInstanceIid, rTx);
        }
    }

    public static Optional<ActionInstance> readActionInstance(DataBroker dataBroker, String tenantId,
            ActionName actionName) throws Exception {
        InstanceIdentifier<ActionInstance> actionIid = IidFactory.actionInstanceIid(new TenantId(tenantId), actionName);
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, actionIid, rTx);
        }
    }

    public static Optional<ConsumerNamedSelector> readConsumerNamedSelector(DataBroker dataBroker, String tenantId, String egId,
                                                                            String selectorName) {
        InstanceIdentifier<ConsumerNamedSelector> cnsIid = IidFactory.consumerNamedSelectorIid(new TenantId(tenantId),
                new EndpointGroupId(egId), new SelectorName(selectorName));
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, cnsIid, rTx);
        }
    }

    public static Optional<Clause> readClause(DataBroker dataBroker, String tenantId, String contractId, String clauseName) {
        InstanceIdentifier<Clause> clauseIid = IidFactory.clauseIid(new TenantId(tenantId), new ContractId(contractId),
                new ClauseName(clauseName));
        try (ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction()) {
            return DataStoreHelper.readFromDs(LogicalDatastoreType.CONFIGURATION, clauseIid, rTx);
        }
    }
}
