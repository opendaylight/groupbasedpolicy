package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EndpointManagerListener implements DataChangeListener, AutoCloseable {

    private final ListenerRegistration<DataChangeListener> registerListener;
    private final EndpointManager endpointManager;

    public EndpointManagerListener(DataBroker dataProvider, EndpointManager endpointManager) {
        this.endpointManager = checkNotNull(endpointManager);
        this.registerListener = checkNotNull(dataProvider).registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, IidFactory.endpointsIidWildcard(), this, AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        //Create
        for (DataObject dao : change.getCreatedData().values()) {
            if (dao instanceof Endpoint) {
                endpointManager.processEndpoint(null, (Endpoint) dao);
            } else if (dao instanceof EndpointL3) {
                endpointManager.processL3Endpoint(null, (EndpointL3) dao);
            } else if (dao instanceof EndpointL3Prefix) {
                //noinspection UnnecessaryContinue
                continue;
            }
        }
        //Update
        Map<InstanceIdentifier<?>, DataObject> dao = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dao.entrySet()) {
            if (entry.getValue() instanceof Endpoint) {
                Endpoint oldEp = (Endpoint) change.getOriginalData().get(entry.getKey());
                endpointManager.processEndpoint(oldEp, (Endpoint) entry.getValue());
            } else if (entry.getValue() instanceof EndpointL3) {
                EndpointL3 oldEp3 = (EndpointL3) change.getOriginalData().get(entry.getKey());
                endpointManager.processL3Endpoint(oldEp3, (EndpointL3) entry.getValue());
            } else if (entry.getValue() instanceof EndpointL3Prefix) {
                //noinspection UnnecessaryContinue
                continue;
            }
        }
        //Remove
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            DataObject old = change.getOriginalData().get(iid);
            if (old == null) {
                continue;
            }
            if (old instanceof Endpoint) {
                endpointManager.processEndpoint((Endpoint) old, null);
            } else if (old instanceof EndpointL3) {
                endpointManager.processL3Endpoint((EndpointL3) old, null);
            } else if (old instanceof EndpointL3Prefix) {
                //noinspection UnnecessaryContinue
                continue;
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (registerListener != null)
            registerListener.close();
    }
}
