package cloud.kynerix.crawlix.controller;

import cloud.kynerix.crawlix.admin.AuthManager;
import cloud.kynerix.crawlix.crawler.CrawlingJobsManager;
import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class GlobalInitializer {
    @ConfigProperty(name = "crawlix.init.workspaces.create.default", defaultValue = "false")
    boolean CREATE_DEFAULT_WORKSPACE;

    @ConfigProperty(name = "crawlix.init.workspaces.default.name", defaultValue = "Default Workspace")
    String DEFAULT_WORKSPACE_NAME;

    @ConfigProperty(name = "crawlix.init.workspaces.default.key", defaultValue = "default")
    String DEFAULT_WORKSPACE_KEY;

    @ConfigProperty(name = "crawlix.init.workspaces.default.token")
    String DEFAULT_WORKSPACE_TOKEN;

    @ConfigProperty(name = "crawlix.init.admin.token")
    String INIT_ADMIN_TOKEN;

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    AuthManager authManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsManager.class.getName());

    void initWorkspaces() {
        if (CREATE_DEFAULT_WORKSPACE) {
            LOGGER.warn("Creating default workspace - Do not use this in production.");
            Workspace workspace = workspaceManager.create(DEFAULT_WORKSPACE_KEY, DEFAULT_WORKSPACE_NAME, INIT_ADMIN_TOKEN == null);
            if (DEFAULT_WORKSPACE_TOKEN != null) {
                workspace.addToken(DEFAULT_WORKSPACE_TOKEN);
            }
            workspaceManager.save(workspace);
        } else {
            LOGGER.info("Skipping initial workspace creation");
        }
    }

    void initAdminToken() {
        if (!authManager.isAdminTokenDefined()) {
            if (INIT_ADMIN_TOKEN != null) {
                LOGGER.info("Setting admin token to value provided");
                authManager.changeAdminToken(INIT_ADMIN_TOKEN);
            } else {
                LOGGER.info("Generating random admin token");
            }
        }
    }

    public void onInit(@Observes StartupEvent ev) throws Exception {
        infinispanSchema.initGlobalSchema();
        initWorkspaces();
        initAdminToken();

        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            infinispanSchema.initWorkspaceSchema(workspace);
        }
    }
}
