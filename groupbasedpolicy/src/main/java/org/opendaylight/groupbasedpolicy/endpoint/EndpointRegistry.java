package org.opendaylight.groupbasedpolicy.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.Endpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.EndpointsL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.UnregisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoint.fields.L3Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.l3.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.l3.EndpointL3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.l3.EndpointL3Key;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

/**
 * Endpoint registry provides a scalable store for accessing and 
 * updating information about endpoints.
 * @author readams
 */
public class EndpointRegistry implements AutoCloseable, EndpointService {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointRegistry.class);

    private final DataBrokerService dataProvider;
    private final static String APPLICATION_TAG = 
            "groupbasedpolicy:endpoint-registry";
    
    final BindingAwareBroker.RpcRegistration<EndpointService> rpcRegistration;

    public EndpointRegistry(DataBrokerService dataProvider,
                            RpcProviderRegistry rpcRegistry) {
        super();
        this.dataProvider = dataProvider;

        rpcRegistration =
                rpcRegistry.addRpcImplementation(EndpointService.class, this);

        LOG.info("Created endpoint registry");
    }

    @Override
    public void close() throws Exception {
        rpcRegistration.close();
    }

    private void docommit(DataModificationTransaction t,
                          String objecttag, String action,
                          Collection<RpcError> errors) {

        try {
            RpcResult<TransactionStatus> commitresult = t.commit().get();
            if (!commitresult.isSuccessful()) {
                errors.addAll(commitresult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            RpcError error = 
                    RpcErrors.getRpcError(APPLICATION_TAG, 
                                          objecttag, "commit error",
                                          ErrorSeverity.ERROR,
                                          "Could not " + action + " "
                                          + objecttag, 
                                          ErrorType.RPC, e);
            errors.add(error);
        }
    }
    
    @Override
    public Future<RpcResult<Void>>
        registerEndpoint(RegisterEndpointInput input) {
        Endpoint ep = new EndpointBuilder(input).build();
        EndpointKey key = 
                new EndpointKey(ep.getL2Namespace(), ep.getMacAddress());
        InstanceIdentifier<Endpoint> iid = 
                InstanceIdentifier.builder(Endpoints.class)
                    .child(Endpoint.class, key).build();
        DataModificationTransaction t = dataProvider.beginTransaction();
        t.putOperationalData(iid, ep);
        Collection<RpcError> errors = new ArrayList<>();
        
        docommit(t, iid.toString(), "register", errors);
        
        if (input.getL3Address() != null) {
            for (L3Address l3addr : input.getL3Address()) {
                t = dataProvider.beginTransaction();
                EndpointL3Key key3 = new EndpointL3Key(l3addr.getIpAddress(), 
                                                       l3addr.getL3Namespace());
                EndpointL3 ep3 = new EndpointL3Builder(input)
                    .setIpAddress(key3.getIpAddress())
                    .setL3Namespace(l3addr.getL3Namespace())
                    .build();
                InstanceIdentifier<EndpointL3> iid_l3 = 
                        InstanceIdentifier.builder(EndpointsL3.class)
                            .child(EndpointL3.class, key3).build();
                t.putOperationalData(iid_l3, ep3);
                
                docommit(t, iid_l3.toString(), "register", errors);
            }
        }
        
        RpcResult<Void> result = Rpcs.<Void>getRpcResult(errors.isEmpty(), 
                                                         errors);
        return Futures.immediateFuture(result);
    }

    @Override
    public Future<RpcResult<Void>>
        unregisterEndpoint(UnregisterEndpointInput input) {
        EndpointKey key = 
                new EndpointKey(input.getL2Namespace(), input.getMacAddress());
        InstanceIdentifier<Endpoint> iid = 
                InstanceIdentifier.builder(Endpoints.class)
                    .child(Endpoint.class, key).build();
        DataObject dao = dataProvider.readOperationalData(iid);

        Collection<RpcError> errors = new ArrayList<>();
        
        if (dao != null && dao instanceof Endpoint) {
            Endpoint ep = (Endpoint)dao;
            
            if (ep.getL3Address() != null) {
                for (L3Address l3addr : ep.getL3Address()) {
                    EndpointL3Key key3 = 
                            new EndpointL3Key(l3addr.getIpAddress(), 
                                              l3addr.getL3Namespace());
                    InstanceIdentifier<EndpointL3> iid_l3 = 
                            InstanceIdentifier.builder(EndpointsL3.class)
                                .child(EndpointL3.class, key3).build();
                    DataModificationTransaction t =
                            dataProvider.beginTransaction();
                    t.removeOperationalData(iid_l3);
                    docommit(t, iid_l3.toString(), "unregister", errors);
                }
            }
            
            DataModificationTransaction t =
                    dataProvider.beginTransaction();
            t.removeOperationalData(iid);
            docommit(t, iid.toString(), "unregister", errors);
        }

        // note that deleting an object that doesn't exist is fine.
        RpcResult<Void> result = Rpcs.<Void>getRpcResult(errors.isEmpty(), 
                                                         errors);
        return Futures.immediateFuture(result);
    }
}
