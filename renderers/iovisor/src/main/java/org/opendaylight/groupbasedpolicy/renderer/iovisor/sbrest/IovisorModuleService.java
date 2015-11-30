/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sbrest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/policies")
public class IovisorModuleService {

    private static final Logger LOG = LoggerFactory.getLogger(IovisorModuleService.class);

    private static final String RESOLVED_POLICY_URI = "/restconf/operational/resolved-policy:resolved-policies/resolved-policy:resolved-policy";

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response sendResolvedPolicies(List<String> resolvedPoliciesID) {
        // TODO create resolvedPolicies coder/decoder
        LOG.info("Sent resolvedPolicies to IOVisorModule {}", "MODULE_INSTANCE");
        return Response.status(200).entity(resolvedPoliciesID).build();
    }

    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public Response test() {
      String result = "Our SBREST Module is up and running\n\n";
      return Response.status(200).entity(result).build();
    }
}
