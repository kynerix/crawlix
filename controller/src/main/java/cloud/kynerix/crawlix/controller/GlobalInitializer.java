package cloud.kynerix.crawlix.controller;

import cloud.kynerix.crawlix.admin.AuthManager;
import cloud.kynerix.crawlix.crawler.CrawlJobsManager;
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
import java.util.Optional;

@ApplicationScoped
public class GlobalInitializer {
    @ConfigProperty(name = "crawlix.init.workspaces.create.default", defaultValue = "false")
    boolean CREATE_DEFAULT_WORKSPACE;

    @ConfigProperty(name = "crawlix.init.workspaces.default.name", defaultValue = "Default Workspace")
    String DEFAULT_WORKSPACE_NAME;

    @ConfigProperty(name = "crawlix.init.workspaces.default.key", defaultValue = "default")
    String DEFAULT_WORKSPACE_KEY;

    @ConfigProperty(name = "crawlix.init.workspaces.default.token")
    Optional<String> DEFAULT_WORKSPACE_TOKEN;

    @ConfigProperty(name = "crawlix.init.admin.token")
    Optional<String> INIT_ADMIN_TOKEN;

    @ConfigProperty(name = "crawlix.init.admin.user")
    Optional<String> INIT_ADMIN_USER;

    @ConfigProperty(name = "crawlix.init.admin.password")
    Optional<String> INIT_ADMIN_PASSWORD;

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    AuthManager authManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlJobsManager.class.getName());

    void initWorkspaces() {
        if (CREATE_DEFAULT_WORKSPACE) {
            boolean generateWorkspaceToken = DEFAULT_WORKSPACE_TOKEN.isEmpty()
                    || DEFAULT_WORKSPACE_TOKEN.get().isBlank()
                    || DEFAULT_WORKSPACE_TOKEN.get().contains("RANDOM");
            LOGGER.warn("Creating default workspace: " + DEFAULT_WORKSPACE_KEY);
            Workspace workspace = workspaceManager.create(DEFAULT_WORKSPACE_KEY, DEFAULT_WORKSPACE_NAME, generateWorkspaceToken);
            if (!generateWorkspaceToken) {
                workspace.addToken(DEFAULT_WORKSPACE_TOKEN.get());
            }
            workspaceManager.save(workspace);
        } else {
            LOGGER.info("Skipping initial workspace creation");
        }
    }

    void initAdminUser() {
        if (!authManager.isAdminUserDefined()) {
            if (INIT_ADMIN_USER.isPresent() && INIT_ADMIN_PASSWORD.isPresent()) {
                LOGGER.info("Setting admin user and password to default values");
                authManager.changeAdminUser(INIT_ADMIN_USER.get(), INIT_ADMIN_PASSWORD.get());
            } else {
                LOGGER.info("No admin user is created");
            }
        }
    }

    void initAdminToken() {
        if (!authManager.isAdminTokenDefined()) {
            if (INIT_ADMIN_TOKEN.isPresent()) {
                LOGGER.info("Setting admin token to value provided");
                authManager.changeAdminToken(INIT_ADMIN_TOKEN.get());
            } else {
                LOGGER.info("Generating random admin token");
                authManager.changeAdminToken(authManager.generateRandomAccessToken());
            }
        }
    }

    public void onInit(@Observes StartupEvent ev) {
        infinispanSchema.initGlobalSchema();
        initWorkspaces();
        initAdminUser();
        initAdminToken();

        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            infinispanSchema.initWorkspaceSchema(workspace);
        }
    }
}
