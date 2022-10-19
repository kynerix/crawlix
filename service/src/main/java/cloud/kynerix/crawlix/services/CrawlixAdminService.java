package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.controller.WorkerNodesManager;
import cloud.kynerix.crawlix.crawler.CrawlingJob;
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

@Path("/crawlix-admin")
@ApplicationScoped
public class CrawlixAdminService extends BaseService {

    @Inject
    WorkerNodesManager crawlerWorkerNodesManager;

    @GET
    @Path("/list-nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listNodes(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (!workspaceManager.isAdmin(authHeader)) {
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
        if (!workspaceManager.isAdmin(authHeader)) {
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
        if (!workspaceManager.isAdmin(authHeader)) {
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
    @Path("/create-workspace")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createWorkspace(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("key") String key,
            @QueryParam("name") String name
    ) {
        if (!workspaceManager.isAdmin(authHeader)) {
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
        if (!workspaceManager.isAdmin(authHeader)) {
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
        if (!workspaceManager.isAdmin(authHeader)) {
            return noAuth();
        }

        if (token == null) {
            return operationResults(false, "Parameter token is mandatory");
        }

        Workspace workspace = workspaceManager.getWorkspaceByToken(token);
        if (workspace == null) {
            return operationResults(false, "Invalid token");
        }
        workspace.getTokens().remove(token);
        workspaceManager.save(workspace);

        return operationResults(true, "Token in workspace '" + workspace.getKey() + "' has been removed.");
    }


    @POST
    @Path("/generate-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateToken(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("workspace") String workspaceKey
    ) {
        if (!workspaceManager.isAdmin(authHeader)) {
            return noAuth();
        }

        if (workspaceKey == null) {
            return operationResults(false, "Parameter workspace is mandatory");
        }

        Workspace workspace = workspaceManager.getWorkspaceByKey(workspaceKey);
        if (workspace == null) {
            return operationResults(false, "Workspace '" + workspaceKey + "' not found");
        }

        String token = workspaceManager.generateRandomAccessToken();

        workspace.getTokens().add(token);
        workspaceManager.save(workspace);

        return operationResults(true, "New token '" + token + "' for workspace '" + workspace.getKey() + "' has been generated.");
    }

    @GET
    @Path("/start-node")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startNode(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("node") String node
    ) {
        if (!workspaceManager.isAdmin(authHeader)) {
            return noAuth();
        }

        if (crawlerWorkerNodesManager.getNode(node) == null) {
            return operationResults(false, "Node " + node + " does not exist");
        }

        crawlerWorkerNodesManager.startNode(node);
        return operationResults(true, "Node " + node + " has been started");
    }

    @GET
    @Path("/stop-node")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopNode(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("node") String node
    ) {
        if (!workspaceManager.isAdmin(authHeader)) {
            return noAuth();
        }

        if (crawlerWorkerNodesManager.getNode(node) == null) {
            return operationResults(false, "Node " + node + " does not exist");
        }

        crawlerWorkerNodesManager.startNode(node);
        return operationResults(true, "Node " + node + " has been stopped");
    }
}