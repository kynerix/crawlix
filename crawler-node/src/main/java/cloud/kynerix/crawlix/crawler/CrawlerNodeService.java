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
    CrawlersManager crawlersManager;

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
            @QueryParam("crawler") String crawlerKey,
            @QueryParam("store-results") @DefaultValue("false") boolean persist
    ) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(workspaceId);
        if (workspace == null) {
            LOGGER.error("Invalid workspace: " + workspaceId);
            return Response.serverError().build();
        }

        Crawler crawler = crawlersManager.getCrawler(workspace, crawlerKey);
        if (crawler == null) {
            LOGGER.error("Invalid crawler: " + crawlerKey);
            return Response.serverError().build();
        }

        CrawlResults crawlResults = crawlerNodeManager.runCrawler(
                crawler.getDefaultURL(),
                crawler,
                null,
                persist);

        LOGGER.debug(crawlResults.toString());

        return Response.accepted(crawlResults).build();
    }

    @GET
    @Path("/javascript")
    @Produces("application/javascript")
    public Response javascript() {
        String js = crawlerNodeManager.getInjectedJS();
        if (js == null) {
            LOGGER.error("Can't find JS");
        }
        if (js == null) return Response.noContent().build();
        return Response.accepted(js).build();
    }
}