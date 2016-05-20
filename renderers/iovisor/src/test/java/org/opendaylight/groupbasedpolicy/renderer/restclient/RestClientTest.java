/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.restclient;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.restclient.RestClient;

public class RestClientTest extends JerseyTest {

    private RestClient client;
    private String uri;
    private static String resolvedPolicy =
            " { \"resolved-policy-uri\" : \"/restconf/operational/resolved-policy:resolved-policies/resolved-policy/tenant-red/client/tenant-red/webserver/\" } ";

    private static final String BASE_URI = "http://localhost";
    private static final int BASE_PORT = 1234;
    private static HttpServer server;

    private static HttpServer startServer() throws IOException {
        final ResourceConfig resourceConfig = new ClassNamesResourceConfig(dumbServer.class);
        HttpServer httpServer = null;
        httpServer = GrizzlyServerFactory.createHttpServer(URI.create(BASE_URI + ":" + BASE_PORT), resourceConfig);
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
        client = new RestClient("http://localhost:1234");
    }

    @Test
    public void testResolvedPoliciesJSON_coverage() {
        client.new ResolvedPoliciesJSON("string");

        List<String> uris = new ArrayList<>();
        uris.add("string1");
        uris.add("string2");
        client.new ResolvedPoliciesJSON(uris);
    }

    @Test
    public void testGet_coverage() {
        client.get("/");
        client.get("/warning");
        client.get("/error");
    }

    @Test
    public void testPost_coverage() {
        client.post("/", "json");
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
            return Response.status(Response.Status.OK).entity(resolvedPolicy).build();
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public Response post200(String json) {
            return Response.status(Response.Status.OK).build();
        }

        @Path("/warning")
        @GET
        public Response get202() {
            return Response.status(Response.Status.ACCEPTED).build();
        }

        @Path("/error")
        @GET
        public Response get404() {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }
}
