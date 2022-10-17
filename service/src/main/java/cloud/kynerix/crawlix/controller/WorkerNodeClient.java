package cloud.kynerix.crawlix.controller;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/crawler-node")
@RegisterRestClient
public interface WorkerNodeClient {

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response status();

    @GET
    @Path("/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response stopNode();

    @GET
    @Path("/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response startNode();

    @GET
    @Path("/execute")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response execute(
            @QueryParam("workspace") String workspaceId,
            @QueryParam("plugin") String pluginId,
            @QueryParam("store-results") @DefaultValue("false") boolean persist
    );

    @GET
    @Path("/javascript")
    @Produces(MediaType.TEXT_PLAIN)
    Response javascript();
}
