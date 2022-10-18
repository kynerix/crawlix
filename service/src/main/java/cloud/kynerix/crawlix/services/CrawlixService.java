package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.crawler.Plugin;
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
    @Path("/install-plugin")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response installPlugin(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            Plugin plugin) throws Exception {

        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        if (plugin.isValid()) {
            pluginsManager.save(workspace, plugin);
            return operationResults(true, "Plugin " + plugin.getKey() + " updated");
        } else {
            LOGGER.error("Invalid plugin definition: " + plugin);
            return operationResults(false, "Invalid plugin definition. Please check the field key is set with the right format.");
        }
    }

    @POST
    @Path("/install-plugins")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response installPlugins(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            List<Plugin> pluginList) {

        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        if (pluginList == null || pluginList.isEmpty()) {
            return operationResults(false, "List of plugins must be provided for bulk installation");
        }

        pluginList.forEach(plugin -> {
            try {
                if (plugin.isValid()) {
                    pluginsManager.save(workspace, plugin);
                } else {
                    LOGGER.error("Invalid plugin definition: " + plugin);
                }
            } catch (Exception e) {
                LOGGER.error("Error updating plugin", e);
            }
        });

        return operationResults(true, pluginList.size() + " plugin(s) updated");
    }

    @POST
    @Path("/install-script")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installScript(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("key") String pluginKey,
            String script) {

        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        Plugin plugin = pluginsManager.getPlugin(workspace, pluginKey);
        if (plugin == null) {
            return operationResults(false, "Plugin " + pluginKey + " not found");
        }

        plugin.setScript(script);
        pluginsManager.save(workspace, plugin);

        return operationResults(true, "Plugin " + pluginKey + " script updated");
    }

    @GET
    @Path("/get-script")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getScript(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("key") String pluginKey) {

        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        Plugin plugin = pluginsManager.getPlugin(workspace, pluginKey);
        if (plugin == null) {
            return Response.serverError().build();
        }

        return Response.accepted(plugin.getScript()).build();
    }


    @DELETE
    @Path("/delete-plugin")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePlugin(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("plugin") String key) {

        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        pluginsManager.delete(workspace, key);
        crawlingJobsManager.deletePluginJobs(workspace, key);

        return operationResults(true, "Crawler " + key + " deleted. Total number of crawlers currently installed is " + pluginsManager.size(workspace));
    }

    @GET
    @Path("/list-plugins")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listPlugins(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        List<Plugin> plugins = pluginsManager.getAllPlugins(workspace);

        LOGGER.info("Listed " + plugins.size() + " crawler plugins");

        return Response.ok(plugins).build();
    }

    @GET
    @Path("/enable-plugin")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startPlugin(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("plugin") String crawler
    ) {
        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        LOGGER.info("Starting plugin " + crawler);
        Plugin plugin = pluginsManager.getPlugin(workspace, crawler);
        if (plugin != null) {
            plugin.setStatus(Plugin.STATUS_ENABLED);
            pluginsManager.save(workspace, plugin);
            return operationResults(true, "Plugin " + plugin + " is ENABLED");
        } else {
            return operationResults(false, "Plugin " + plugin + " not found");
        }
    }

    @GET
    @Path("/disable-plugin")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopPlugin(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("plugin") String crawler) {
        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        LOGGER.info("Stopping plugin " + crawler);
        Plugin plugin = pluginsManager.getPlugin(workspace, crawler);
        if (plugin != null) {
            plugin.setStatus(Plugin.STATUS_DISABLED);
            pluginsManager.save(workspace, plugin);
            return operationResults(true, "Plugin " + plugin + " is DISABLED");
        } else {
            return operationResults(false, "Plugin " + plugin + " not found");
        }
    }

    @GET
    @Path("/execute")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response execute(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader,
            @QueryParam("plugin") String pluginKey,
            @QueryParam("store-results") @DefaultValue("false") boolean storeResults) throws Exception {

        Workspace workspace = workspaceManager.getWorkspaceByAuthHeader(authHeader);
        if (workspace == null) return noAuth();

        LOGGER.info("Running plugin " + pluginKey + " once");

        Plugin plugin = pluginsManager.getPlugin(workspace, pluginKey);
        if (plugin == null) {
            return operationResults(false, "Plugin " + pluginKey + " not found");
        }

        // Execute remotely and return response
        WorkerNode node = workerNodesManager.getRandomNode();
        if (node == null || !node.isActive()) {
            return operationResults(false, "Can't lock a node to execute crawler");
        } else {
            LOGGER.error("Executing in worker node " + node.getKey());
            try {
                Response response = workerNodesManager.executeRemoteCrawler(node.getKey(), workspace.getKey(), pluginKey, storeResults);
                if (response == null) {
                    return operationResults(false, "Unknown error");
                } else {
                    return response;
                }
            } catch (Exception e) {
                LOGGER.error("Error executing plugin " + pluginKey + " in node " + node, e);
                return operationResults(false, "Error executing plugin " + pluginKey + " - Exception: " + e.getClass().getName());
            }
        }
    }
}