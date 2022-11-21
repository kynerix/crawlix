package cloud.kynerix.crawlix.admin;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class AuthManager {

    private static final String ADMIN_TOKEN_KEY = "admin.token";

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    public String generateRandomAccessToken() {
        return UUID.randomUUID().toString();
    }

    public boolean isAdmin(String authHeader) {
        String adminToken = (String) infinispanSchema.getGlobalSettingsCache().get(ADMIN_TOKEN_KEY);
        return authHeader != null && authHeader.equals(adminToken);
    }

    public boolean canAccessWorkspace(String authHeader, Workspace workspace) {
        return workspace != null && (isAdmin(authHeader) || workspace.getTokens().contains(authHeader));
    }

    public void changeAdminToken(String newToken) {
        if (newToken == null || newToken.isBlank()) {
            infinispanSchema.getGlobalSettingsCache().remove(ADMIN_TOKEN_KEY);
        } else {
            infinispanSchema.getGlobalSettingsCache().put(ADMIN_TOKEN_KEY, newToken);
        }
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
