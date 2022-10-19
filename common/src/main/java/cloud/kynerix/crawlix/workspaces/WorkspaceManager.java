package cloud.kynerix.crawlix.workspaces;

import cloud.kynerix.crawlix.crawler.CrawlingJobsManager;
import cloud.kynerix.crawlix.schema.InfinispanSchema;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WorkspaceManager {

    @Inject
    InfinispanSchema infinispanSchema;

    @ConfigProperty(name = "crawlix.workspaces.create.default", defaultValue = "false")
    boolean CREATE_DEFAULT_WORKSPACE;

    @ConfigProperty(name = "crawlix.workspaces.default.name", defaultValue = "Default Workspace")
    String DEFAULT_WORKSPACE_NAME;

    @ConfigProperty(name = "crawlix.workspaces.default.key", defaultValue = "default")
    String DEFAULT_WORKSPACE_KEY;

    @ConfigProperty(name = "crawlix.workspaces.default.token", defaultValue = "00-DEFAULT-TOKEN-00")
    String DEFAULT_WORKSPACE_TOKEN;

    @ConfigProperty(name = "crawlix.admin.default.token", defaultValue = "00-DEFAULT-ADMIN-TOKEN-00")
    String DEFAULT_ADMIN_TOKEN;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsManager.class.getName());

    public boolean isAdmin(String authHeader) {
        return authHeader != null && authHeader.equals(DEFAULT_ADMIN_TOKEN);
    }

    public Workspace getWorkspaceByAuthHeader(String authHeader) {
        return getWorkspaceByToken(authHeader);
    }

    public Workspace getWorkspaceByToken(String token) {
        if (token != null) {
            for (Workspace w : getWorkspaces()) {
                if (w.getTokens().contains(token)) {
                    return w;
                }
            }
        }
        return null;
    }

    public void removeWorkspace(String key) {
        if (key != null) {
            infinispanSchema.getWorkspacesCache().remove(key);
        }
    }

    public Workspace getWorkspaceByKey(String key) {
        return infinispanSchema.getWorkspacesCache().get(key);
    }

    public List<Workspace> getWorkspaces() {
        return new ArrayList<>(infinispanSchema.getWorkspacesCache().values());
    }

    public Workspace create(String key, String name, boolean createToken) {
        Workspace workspace = new Workspace();
        workspace.setKey(key);
        workspace.setName(name);
        if (createToken) {
            workspace.addToken(generateRandomAccessToken());
        }
        save(workspace);
        return workspace;
    }

    public void save(Workspace workspace) {
        infinispanSchema.getWorkspacesCache().put(workspace.getKey(), workspace);
        LOGGER.info("Updated " + workspace);
    }

    public String generateRandomAccessToken() {
        return UUID.randomUUID().toString();
    }

    public void init() {
        if (CREATE_DEFAULT_WORKSPACE) {
            LOGGER.warn("Creating default workspace - Do not use this in production.");
            Workspace workspace = create(DEFAULT_WORKSPACE_KEY, DEFAULT_WORKSPACE_NAME, false);
            workspace.addToken(DEFAULT_WORKSPACE_TOKEN);
            save(workspace);
        } else {
            LOGGER.info("Skipping initial workspace creation");
        }

        if (DEFAULT_ADMIN_TOKEN != null && DEFAULT_ADMIN_TOKEN.contains("-DEFAULT-")) {
            LOGGER.warn("Default Admin token - Please, change it for production.");
        }
    }
}
