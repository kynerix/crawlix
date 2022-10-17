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

    @ConfigProperty(name = "crawlix.workspaces.default.name", defaultValue = "Default Workspace")
    private String DEFAULT_WORKSPACE_NAME;

    @ConfigProperty(name = "crawlix.workspaces.default.key", defaultValue = "default")
    private String DEFAULT_WORKSPACE_KEY;

    @ConfigProperty(name = "crawlix.workspaces.default.token", defaultValue = "00-DEFAULT-TOKEN-00")
    private String DEFAULT_WORKSPACE_TOKEN;

    @ConfigProperty(name = "crawlix.workspaces.default.prefix", defaultValue = "DEFAULT")
    private String DEFAULT_WORKSPACE_PREFIX;

    @ConfigProperty(name = "crawlix.workspaces.create.default", defaultValue = "false")
    private boolean CREATE_DEFAULT_WORKSPACE;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsManager.class.getName());

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

    public Workspace getWorkspaceByKey(String key) {
        return infinispanSchema.getWorkspacesCache().get(key);
    }

    public List<Workspace> getWorkspaces() {
        return new ArrayList<>(infinispanSchema.getWorkspacesCache().values());
    }

    public void updateWorkspace(Workspace workspace) {
        infinispanSchema.getWorkspacesCache().put(workspace.getKey(), workspace);
        LOGGER.info("Updated " + workspace);
    }

    public String generateRandomAccessToken() {
        return UUID.randomUUID().toString();
    }

    public void init() {
        if (CREATE_DEFAULT_WORKSPACE) {
            LOGGER.info("Creating default workspace");
            Workspace workspace = new Workspace();
            workspace.setKey(DEFAULT_WORKSPACE_KEY);
            workspace.setName(DEFAULT_WORKSPACE_NAME);
            workspace.addToken(DEFAULT_WORKSPACE_TOKEN);
            updateWorkspace(workspace);
        } else {
            LOGGER.info("Skipping initial workspace creation");
        }
    }
}
