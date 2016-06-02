/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.SFlowQueryParams;

public class JsonRestClientTest extends JerseyTest {

    private static final int CONNECT_TIMEOUT_MILLISEC = 20000;
    private static final int READ_TIMEOUT_MILLISEC = 30000;
    private static final String SFLOW_HOST = "localhost";
    private static final int SFLOW_PORT = 1234;
    private static HttpServer server;
    private static final String SFLOW_URI = "http://" + SFLOW_HOST + ":" + SFLOW_PORT;

    private JsonRestClient client;
    private String uri;
    private static String responseJson =
            " { \"resolved-policy-uri\" : \"/restconf/operational/resolved-policy:resolved-policies/resolved-policy/tenant-red/client/tenant-red/webserver/\" } ";

    private static HttpServer startServer() throws IOException {
        final ResourceConfig resourceConfig = new ClassNamesResourceConfig(dumbServer.class);
        HttpServer httpServer;
        httpServer = GrizzlyServerFactory.createHttpServer(java.net.URI.create(SFLOW_URI), resourceConfig);
        return httpServer;
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        server = startServer();
    }

    @AfterClass
    public static void tearDownClass() {
        if (server != null && server.isStarted())
            server.stop();
    }

    @Before
    public void init() {
        client = new JsonRestClient(SFLOW_URI, CONNECT_TIMEOUT_MILLISEC, READ_TIMEOUT_MILLISEC);
    }

    @Test
    public void testGetHost() {
        String host = client.getHost();

        assertEquals(SFLOW_HOST, host);
    }

    @Test
    public void testGet_coverageOnly() {
        client.get("/");
    }

    @Test
    public void testGet_params_coverageOnly() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add(SFlowQueryParams.MAX_FLOWS, "20");
        params.add(SFlowQueryParams.MIN_VALUE, "0.1");
        params.add(SFlowQueryParams.AGG_MODE, "sum");

        client.get("/", params);
    }

    @Test
    public void testPost_coverageOnly() {
        client.post("/", "json");
    }

    @Test
    public void testPut_coverageOnly() {
        client.put("/", "json");
    }

    @Test
    public void testDelete_coverageOnly() {
        client.delete("/");
    }

    @Override
    protected AppDescriptor configure() {
        return new WebAppDescriptor.Builder().build();
    }

    @Path("/")
    public static class dumbServer {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response get200() {
            return Response.status(Response.Status.OK).entity(responseJson).build();
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public Response post200(String json) {
            return Response.status(Response.Status.OK).build();
        }

    }
}
