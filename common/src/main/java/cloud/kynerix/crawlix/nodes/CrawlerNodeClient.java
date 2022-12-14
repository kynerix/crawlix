package cloud.kynerix.crawlix.nodes;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/crawler-node")
@RegisterRestClient
public interface CrawlerNodeClient {

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
            @QueryParam("crawler") String crawlerKey,
            @QueryParam("store-results") @DefaultValue("false") boolean persist
    );

    @GET
    @Path("/javascript")
    @Produces("application/javascript")
    Response javascript();
}
