package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PluginsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginsManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    public Plugin getPlugin(Workspace workspace, String nodeKey) {
        return infinispanSchema.getPluginsCache(workspace).get(nodeKey);
    }

    public void save(Workspace workspace, Plugin plugin) {
        infinispanSchema.getPluginsCache(workspace).put(plugin.getKey(), plugin);
    }

    public void delete(Workspace workspace, String plugin) {
        infinispanSchema.getPluginsCache(workspace).remove(plugin);
    }

    public int size(Workspace workspace) {
        return infinispanSchema.getPluginsCache(workspace).size();
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
}
