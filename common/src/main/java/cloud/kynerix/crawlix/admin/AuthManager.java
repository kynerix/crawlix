package cloud.kynerix.crawlix.admin;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class AuthManager {

    private static final String ADMIN_TOKEN_KEY = "admin.token";
    private static final String ADMIN_USER_KEY = "admin.user";
    private static final String ADMIN_PASSWORD_KEY = "admin.password";

    private static final String TOKEN_TYPE_ADMIN = "ADMIN";

    @ConfigProperty(name = "crawlix.auth.token.lifespan.minutes", defaultValue = "30")
    long TOKEN_LIFESPAN_IN_MINUTES;

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    public String generateRandomAccessToken() {
        return UUID.randomUUID().toString();
    }

    public boolean isAdminToken(String token) {
        if (token == null) return false;

        // Check for temporal token
        String tempTokenType = infinispanSchema.getTokensCache().get(token);
        if (tempTokenType != null && tempTokenType.equals(TOKEN_TYPE_ADMIN)) {
            return true;
        }

        // Check for permanent admin token
        String adminToken = (String) infinispanSchema.getGlobalSettingsCache().get(ADMIN_TOKEN_KEY);

        if (token.equals(adminToken)) {
            return true;
        }
        return false;
    }

    public String authAdminUser(String user, String password) {
        String adminUser = infinispanSchema.getGlobalSettingsCache().get(ADMIN_USER_KEY);
        String adminPassword = infinispanSchema.getGlobalSettingsCache().get(ADMIN_PASSWORD_KEY);

        if (user != null && password != null && user.equals(adminUser) && password.equals(adminPassword)) {
            //return infinispanSchema.getGlobalSettingsCache().get(ADMIN_TOKEN_KEY);
            String newToken = generateRandomAccessToken();
            // Add a new token with a configured lifespan
            infinispanSchema.getTokensCache().put(newToken, TOKEN_TYPE_ADMIN, TOKEN_LIFESPAN_IN_MINUTES, TimeUnit.MINUTES);
            return newToken;
        } else {
            return null;
        }
    }

    public void changeAdminUser(String user, String password) {
        // TODO: HASH USER / PASSWORD
        infinispanSchema.getGlobalSettingsCache().put(ADMIN_USER_KEY, user);
        infinispanSchema.getGlobalSettingsCache().put(ADMIN_PASSWORD_KEY, password);
    }

    public boolean canAccessWorkspace(String authHeader, Workspace workspace) {
        return workspace != null && (isAdminToken(authHeader) || workspace.getTokens().contains(authHeader));
    }

    public void changeAdminToken(String newToken) {
        if (newToken == null || newToken.isBlank()) {
            infinispanSchema.getGlobalSettingsCache().remove(ADMIN_TOKEN_KEY);
        } else {
            infinispanSchema.getGlobalSettingsCache().put(ADMIN_TOKEN_KEY, newToken);
        }
    }

    public boolean isAdminUserDefined() {
        return infinispanSchema.getGlobalSettingsCache().get(ADMIN_USER_KEY) != null;
    }

    public boolean isAdminTokenDefined() {
        return infinispanSchema.getGlobalSettingsCache().get(ADMIN_TOKEN_KEY) != null;
    }

    public Workspace getWorkspaceByAuthHeader(String authHeader) {
        return getWorkspaceByToken(authHeader);
    }

    public Workspace getWorkspaceByToken(String token) {
        if (token != null) {
            for (Workspace w : workspaceManager.getWorkspaces()) {
                if (w.getTokens().contains(token)) {
                    return w;
                }
            }
        }
        return null;
    }
}
