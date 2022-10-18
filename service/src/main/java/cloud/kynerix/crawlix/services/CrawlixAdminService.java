package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.controller.WorkerNodesManager;
import cloud.kynerix.crawlix.crawler.CrawlingJob;
import cloud.kynerix.crawlix.workspaces.Workspace;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
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
    public Response nodes() {
        LOGGER.info("Retrieving all node status");

        return Response.ok(
                crawlerWorkerNodesManager.getAllNodes()
        ).build();
    }

    @GET
    @Path("/list-jobs")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response jobs(
            @QueryParam("workspace") String workspaceKey,
            @QueryParam("plugin") String plugin
    ) {
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
    @Path("/start-node")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startNode(@QueryParam("node") String node
    ) {
        LOGGER.info("Starting node " + node);
        if (crawlerWorkerNodesManager.getNode(node) == null) {
            LOGGER.error("Node " + node + " does not exist");
            return Response.ok(Boolean.FALSE).build();
        } else {
            crawlerWorkerNodesManager.startNode(node);
            return Response.ok(Boolean.TRUE).build();
        }
    }

    @GET
    @Path("/stop-node")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopNode(@QueryParam("node") String node
    ) {
        LOGGER.info("Stopping node " + node);
        if (crawlerWorkerNodesManager.getNode(node) == null) {
            LOGGER.error("Node " + node + " does not exist");
            return Response.ok(Boolean.FALSE).build();
        } else {
            crawlerWorkerNodesManager.stopNode(node);
            return Response.ok(Boolean.TRUE).build();
        }
    }
}