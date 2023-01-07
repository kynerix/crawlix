package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.crawler.CrawlJob;
import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.nodes.CrawlerNodesManager;
import cloud.kynerix.crawlix.workspaces.Workspace;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/admin")
@ApplicationScoped
public class CrawlixAdminService extends BaseService {

    @Inject
    CrawlerNodesManager crawlerWorkerNodesManager;

    @POST
    @Path("/auth")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticateAdmin(
            Map<String, String> loginParams
    ) {
        LOGGER.info("Authenticating admin user");

        if( loginParams == null ) {
            return operationResults(false, "Invalid request");
        }

        String user = loginParams.get("user");
        String password = loginParams.get("password");

        String adminToken = authManager.authAdminUser(user, password);
        if (adminToken == null) {
            LOGGER.error("Invalid admin authentication from");
            return operationResults(false, "Invalid credentials");
        } else {
            return operationResults(true, "Login successful", adminToken);
        }
    }

    @GET
    @Path("/list-nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listNodes(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }
        LOGGER.info("Retrieving all node status");
        return Response.ok(
                crawlerWorkerNodesManager.getAllNodes()
        ).build();
    }

    @GET
    @Path("/list-workspaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listWorkspaces(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }
        LOGGER.info("Retrieving all workspaces");
        return Response.ok(
                workspaceManager.getWorkspaces()
        ).build();
    }

    @GET
    @Path("/list-jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jobs(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("workspace") String workspaceKey,
            @QueryParam("crawler") String crawlerKey
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        LOGGER.info("Retrieving all jobs for workspace: " + workspaceKey + " and crawlerKey: " + crawlerKey);

        List<CrawlJob> jobs = new ArrayList<>();
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            if (workspaceKey == null || workspace.getKey().equals(workspaceKey)) {
                jobs.addAll(crawlJobsManager.findAllJobs(workspace).stream().filter(
                        (CrawlJob job) -> {
                            return crawlerKey == null || crawlerKey.equals(job.getCrawlerKey());
                        }
                ).collect(Collectors.toList()));
            }
        }

        return Response.ok(jobs).build();
    }

    @GET
    @Path("/list-crawlers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listCrawlers(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("workspace") String paramWorkspace
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        List<Crawler> crawlers;

        if (paramWorkspace == null) {
            crawlers = crawlersManager.getAllCrawlers();
        } else {
            Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
            crawlers = crawlersManager.getAllCrawlers(workspace);
        }

        LOGGER.info("Listed " + crawlers.size() + " crawler crawlers");

        return Response.ok(crawlers).build();
    }

    @POST
    @Path("/create-workspace")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkspace(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("key") String key,
            @QueryParam("name") String name
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        if (key == null) {
            return operationResults(false, "Parameter key is mandatory");
        }

        if (workspaceManager.getWorkspaceByKey(key) != null) {
            return operationResults(false, "Workspace '" + key + "' already exists.");
        }
        Workspace workspace = workspaceManager.create(key, name, true);
        return Response.ok(workspace).build();
    }

    @DELETE
    @Path("/delete-workspace")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteWorkspace(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("key") String key
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        if (key == null) {
            return operationResults(false, "Parameter key is mandatory");
        }

        workspaceManager.removeWorkspace(key);
        return operationResults(true, "Workspace '" + key + "' has been deleted.");
    }

    @DELETE
    @Path("/delete-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteToken(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("token") String token
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        if (token == null) {
            return operationResults(false, "Parameter token is mandatory");
        }

        Workspace workspace = authManager.getWorkspaceByToken(token);
        if (workspace == null) {
            return operationResults(false, "Invalid token");
        }
        workspace.getTokens().remove(token);
        workspaceManager.save(workspace);

        return operationResults(true, "Token in workspace '" + workspace.getKey() + "' has been removed.");
    }


    @PUT
    @Path("/generate-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateToken(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("workspace") String workspaceKey
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        if (workspaceKey == null) {
            return operationResults(false, "Parameter workspace is mandatory");
        }

        Workspace workspace = workspaceManager.getWorkspaceByKey(workspaceKey);
        if (workspace == null) {
            return operationResults(false, "Workspace '" + workspaceKey + "' not found");
        }

        String token = authManager.generateRandomAccessToken();

        workspace.getTokens().add(token);
        workspaceManager.save(workspace);

        return operationResults(true, "New token '" + token + "' for workspace '" + workspace.getKey() + "' has been generated.");
    }
    @PUT
    @Path("/start-node")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startNode(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("node") String node
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        if (crawlerWorkerNodesManager.getNode(node) == null) {
            return operationResults(false, "Node '" + node + "' does not exist");
        } else if (crawlerWorkerNodesManager.startNode(node)) {
            return operationResults(true, "Node '" + node + "' has been started");
        } else {
            return operationResults(false, "Error starting node '" + node + "'");
        }
    }

    @PUT
    @Path("/stop-node")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopNode(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("node") String node
    ) {
        if (!authManager.isAdminToken(authHeader)) {
            return noAuth();
        }

        if (crawlerWorkerNodesManager.getNode(node) == null) {
            return operationResults(false, "Node " + node + " does not exist");
        } else if (crawlerWorkerNodesManager.stopNode(node)) {
            return operationResults(true, "Node " + node + " has been stopped");
        } else {
            return operationResults(false, "Error stopping node '" + node + "'");
        }
    }
}