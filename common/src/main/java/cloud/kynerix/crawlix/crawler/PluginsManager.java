package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

@ApplicationScoped
public class PluginsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginsManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    public Plugin getPlugin(Workspace workspace, String nodeKey) {
        return infinispanSchema.getPluginsCache(workspace).get(nodeKey);
    }

    public void save(Workspace workspace, Plugin plugin) {
        plugin.setWorkspace(workspace.getKey());
        infinispanSchema.getPluginsCache(workspace).put(plugin.getKey(), plugin);
    }

    public void delete(Workspace workspace, String plugin) {
        infinispanSchema.getPluginsCache(workspace).remove(plugin);
    }

    public int size(Workspace workspace) {
        return infinispanSchema.getPluginsCache(workspace).size();
    }

    public List<Plugin> getAllPlugins() {
        List<Plugin> allPlugins = new ArrayList<>();
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            allPlugins.addAll(getAllPlugins(workspace));
        }
        return allPlugins;
    }

    public List<Plugin> getAllPlugins(Workspace workspace) {
        return new ArrayList<>(infinispanSchema.getPluginsCache(workspace).values());
    }

    public List<Plugin> getAllPlugins(Workspace workspace, String status) {
        List<Plugin> filtered = getAllPlugins(workspace);
        if (status != null) {
            filtered.removeIf(c -> !status.equalsIgnoreCase(c.getStatus()));
        }
        return filtered;
    }

    public void checkScriptURLForUpdate(Workspace workspace, Plugin plugin) {
        if (plugin == null) return;
        if (plugin.getScriptURL() == null) return;

        // Try to load script from URL and update the script body if needed
        try {
            String scriptBody = new Scanner(new URL(plugin.getScriptURL()).openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();
            if (scriptBody != null && !scriptBody.trim().isEmpty()) {
                LOGGER.debug("Updating plugin script from " + plugin.getScriptURL());
                scriptBody = "// Updated from " + plugin.getScriptURL() + " at " + new Date() + "\n\r" + scriptBody;
                plugin.setScript(scriptBody);
                save(workspace, plugin);
            }
        } catch (IOException e) {
            LOGGER.error("Error loading script URL: " + plugin.getScriptURL(), e);
        }
    }
}
