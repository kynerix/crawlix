package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.workspaces.Workspace;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/crawlix")
@ApplicationScoped
public class CrawlixService extends BaseService {

    @POST
    @Path("/{workspace}/install-crawler")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response installCrawler(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("workspace") String paramWorkspace,
            Crawler crawler) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }
        if (crawler.isValid()) {
            crawlersManager.save(workspace, crawler);
            crawlersManager.checkScriptURLForUpdate(workspace, crawler);
            return operationResults(true, "Crawler " + crawler.getKey() + " updated");
        } else {
            LOGGER.error("Invalid crawler definition: " + crawler);
            return operationResults(false, "Invalid crawler definition. Please check the field key is set with the right format.");
        }
    }

    @POST
    @Path("/{workspace}/install-crawlers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response installCrawlers(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("workspace") String paramWorkspace,
            List<Crawler> crawlerList) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }
        if (crawlerList == null || crawlerList.isEmpty()) {
            return operationResults(false, "List of crawlers must be provided for bulk installation");
        }

        crawlerList.forEach(crawler -> {
            try {
                if (crawler.isValid()) {
                    crawlersManager.save(workspace, crawler);
                    crawlersManager.checkScriptURLForUpdate(workspace, crawler);
                } else {
                    LOGGER.error("Invalid crawler definition: " + crawler);
                }
            } catch (Exception e) {
                LOGGER.error("Error updating crawler", e);
            }
        });

        return operationResults(true, crawlerList.size() + " crawlers(s) updated");
    }

    @POST
    @Path("/{workspace}/crawlers/{crawler}/script")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installScript(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace,
            String script) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }
        Crawler crawler = crawlersManager.getCrawler(workspace, crawlerKey);
        if (crawler == null) {
            return operationResults(false, "Crawler " + crawlerKey + " not found");
        }

        crawler.setScript(script);
        crawlersManager.save(workspace, crawler);

        return operationResults(true, "Crawler " + crawlerKey + " script updated");
    }

    @GET
    @Path("/{workspace}/crawlers/{crawler}/script")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getScript(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }

        Crawler crawler = crawlersManager.getCrawler(workspace, crawlerKey);
        if (crawler == null) {
            return operationResults(false, "Invalid crawler " + crawlerKey);
        }

        return Response.accepted(crawler.getScript()).build();
    }

    @GET
    @Path("/{workspace}/crawlers/{crawler}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCrawler(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }

        Crawler crawler = crawlersManager.getCrawler(workspace, crawlerKey);
        if (crawler == null) {
            return operationResults(false, "Invalid crawlerKey " + crawlerKey);
        }

        return Response.accepted(crawler).build();
    }


    @DELETE
    @Path("/{workspace}/crawlers/{crawler}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteCrawler(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }

        crawlersManager.delete(workspace, crawlerKey);
        crawlJobsManager.deleteJobs(workspace, crawlerKey);

        return operationResults(true, "Crawler " + crawlerKey + " deleted. Total number of crawlers currently installed is " + crawlersManager.size(workspace));
    }

    @GET
    @Path("/{workspace}/crawlers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listCrawlers(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("workspace") String paramWorkspace
    ) {
        List<Crawler> crawlers;

        if (paramWorkspace == null && authManager.isAdminToken(authHeader)) {
            crawlers = crawlersManager.getAllCrawlers();
        } else {
            Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
            if (!authManager.canAccessWorkspace(authHeader, workspace)) {
                return noAuth();
            }
            crawlers = crawlersManager.getAllCrawlers(workspace);
        }

        LOGGER.info("Listed " + crawlers.size() + " crawler crawlers");

        return Response.ok(crawlers).build();
    }

    @PUT
    @Path("/{workspace}/crawlers/{crawler}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enableCrawler(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace
    ) {
        return changeCrawlerStatus(authHeader, crawlerKey, paramWorkspace, Crawler.STATUS_ENABLED);
    }

    @PUT
    @Path("/{workspace}/crawlers/{crawler}/disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response disableCrawler(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace) {
        return changeCrawlerStatus(authHeader, crawlerKey, paramWorkspace, Crawler.STATUS_DISABLED);
    }

    private Response changeCrawlerStatus(String authHeader, String crawlerKey, String paramWorkspace, String status) {
        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }
        LOGGER.info("Starting crawlerKey " + crawlerKey);
        Crawler crawler = crawlersManager.getCrawler(workspace, crawlerKey);
        if (crawler != null) {
            crawler.setStatus(status);
            crawlersManager.save(workspace, crawler);
            return operationResults(true, "Crawler " + crawlerKey + " status set to " + status);
        } else {
            return operationResults(false, "Crawler " + crawlerKey + " not found");
        }
    }

    @PUT
    @Path("/{workspace}/crawlers/{crawler}/execute")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response execute(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathParam("crawler") String crawlerKey,
            @PathParam("workspace") String paramWorkspace,
            @QueryParam("store-results") @DefaultValue("false") boolean storeResults) {

        Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
        if (!authManager.canAccessWorkspace(authHeader, workspace)) {
            return noAuth();
        }
        LOGGER.info("Running crawler " + crawlerKey + " once");

        Crawler crawler = crawlersManager.getCrawler(workspace, crawlerKey);
        if (crawler == null) {
            return operationResults(false, "Crawler " + crawlerKey + " not found");
        }

        crawlersManager.checkScriptURLForUpdate(workspace, crawler);

        // Execute remotely and return response
        WorkerNode node = workerNodesManager.getRandomNode();
        if (node == null || !node.isActive()) {
            return operationResults(false, "Can't lock a node to execute crawler");
        } else {
            LOGGER.debug("Executing in worker node " + node.getKey());
            try {
                Response response = workerNodesManager.executeRemoteCrawler(node.getKey(), workspace.getKey(), crawlerKey, storeResults);
                if (response == null) {
                    return operationResults(false, "Unknown error");
                } else {
                    return response;
                }
            } catch (Exception e) {
                LOGGER.error("Error executing crawler " + crawlerKey + " in node " + node, e);
                return operationResults(false, "Error executing crawler " + crawlerKey + " - Exception: " + e.getClass().getName());
            }
        }
    }

    @GET
    @Path("/javascript")
    @Produces("application/javascript")
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