package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.workspaces.Workspace;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/crawlix-content")
@ApplicationScoped
public class ContentService extends BaseService {
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    public Response searchContent(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("store") String store,
            @QueryParam("offset") @DefaultValue("0") int startOffset,
            @QueryParam("max-results") @DefaultValue("1000") int maxResults,
            @QueryParam("filter") String queryFilter
    ) {
        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        try {
            List<Content> results = contentManager.search(workspace, store, queryFilter, startOffset, maxResults);
            return Response.ok(results).build();
        } catch (Exception e) {
            LOGGER.error("Error running query - workspace: " + workspace + " filter: " + queryFilter, e);
            return operationResults(false, "Error running query: filter: '" + queryFilter + "' - " + e.getMessage());
        }
    }

    @GET
    @Path("/javascript")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendJavascript() {
        // Execute remotely and return response
        WorkerNode crawlerWorkerNode = workerNodesManager.getRandomNode();
        if (crawlerWorkerNode == null || !crawlerWorkerNode.isActive()) {
            LOGGER.error("No crawling node available - cancelling");
            return Response.serverError().build();
        } else {
            return workerNodesManager.getJavascript(crawlerWorkerNode.getKey());
        }
    }
}
