package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.crawler.CrawlingJob;
import cloud.kynerix.crawlix.crawler.Plugin;
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
import java.util.stream.Collectors;

@Path("/admin")
@ApplicationScoped
public class CrawlixAdminService extends BaseService {

    @Inject
    CrawlerNodesManager crawlerWorkerNodesManager;

    @GET
    @Path("/list-nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listNodes(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (!authManager.isAdmin(authHeader)) {
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
        if (!authManager.isAdmin(authHeader)) {
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
            @QueryParam("plugin") String plugin
    ) {
        if (!authManager.isAdmin(authHeader)) {
            return noAuth();
        }

        LOGGER.info("Retrieving all jobs for workspace: " + workspaceKey + " and plugin: " + plugin);

        List<CrawlingJob> jobs = new ArrayList<>();
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            if (workspaceKey == null || workspace.getKey().equals(workspaceKey)) {
                jobs.addAll(crawlingJobsManager.findAllJobs(workspace).stream().filter(
                        (CrawlingJob job) -> {
                            return plugin == null || plugin.equals(job.getPlugin());
                        }
                ).collect(Collectors.toList()));
            }
        }

        return Response.ok(jobs).build();
    }

    @GET
    @Path("/list-plugins")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listPlugins(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("workspace") String paramWorkspace
    ) {
        if (!authManager.isAdmin(authHeader)) {
            return noAuth();
        }

        List<Plugin> plugins;

        if (paramWorkspace == null) {
            plugins = pluginsManager.getAllPlugins();
        } else {
            Workspace workspace = workspaceManager.getWorkspaceByKey(paramWorkspace);
            plugins = pluginsManager.getAllPlugins(workspace);
        }

        LOGGER.info("Listed " + plugins.size() + " crawler plugins");

        return Response.ok(plugins).build();
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
        if (!authManager.isAdmin(authHeader)) {
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
        if (!authManager.isAdmin(authHeader)) {
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
        if (!authManager.isAdmin(authHeader)) {
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
        if (!authManager.isAdmin(authHeader)) {
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
        if (!authManager.isAdmin(authHeader)) {
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
        if (!authManager.isAdmin(authHeader)) {
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