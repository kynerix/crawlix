package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/crawler-node")
public class CrawlerNodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerNodeService.class.getName());

    @Inject
    CrawlerNodeManager crawlerNodeManager;

    @Inject
    PluginsManager pluginsManager;

    @Inject
    WorkspaceManager workspaceManager;

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.ok(Boolean.TRUE).build();
    }

    @GET
    @Path("/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopNode() {
        crawlerNodeManager.stop();
        return Response.ok(Boolean.TRUE).build();
    }

    @GET
    @Path("/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startNode() {
        crawlerNodeManager.start();
        return Response.ok(Boolean.TRUE).build();
    }

    @GET
    @Path("/execute")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response execute(
            @QueryParam("workspace") String workspaceId,
            @QueryParam("plugin") String pluginId,
            @QueryParam("store-results") @DefaultValue("false") boolean persist
    ) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(workspaceId);
        if (workspace == null) {
            LOGGER.error("Invalid workspace: " + workspaceId);
            return Response.serverError().build();
        }

        Plugin plugin = pluginsManager.getPlugin(workspace, pluginId);
        if (plugin == null) {
            LOGGER.error("Invalid plugin: " + pluginId);
            return Response.serverError().build();
        }

        CrawlingResults crawlingResults = crawlerNodeManager.runCrawlerExecution(workspace, null, plugin, persist);

        return Response.accepted(crawlingResults).build();
    }

    @GET
    @Path("/javascript")
    @Produces(MediaType.TEXT_PLAIN)
    public Response javascript() {
        return Response.accepted(crawlerNodeManager.getInjectedJS()).build();

    }
}