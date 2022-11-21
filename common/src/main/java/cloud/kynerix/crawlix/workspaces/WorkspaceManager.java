package cloud.kynerix.crawlix.workspaces;

import cloud.kynerix.crawlix.admin.AuthManager;
import cloud.kynerix.crawlix.crawler.CrawlingJobsManager;
import cloud.kynerix.crawlix.schema.InfinispanSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WorkspaceManager {

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    AuthManager authManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsManager.class.getName());

    public void removeWorkspace(String key) {
        if (key != null) {
            infinispanSchema.getWorkspacesCache().remove(key);
        }
    }

    public Workspace getWorkspaceByKey(String key) {
        if( key == null ) return null;
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
            workspace.addToken(authManager.generateRandomAccessToken());
        }
        save(workspace);
        return workspace;
    }

    public void save(Workspace workspace) {
        infinispanSchema.getWorkspacesCache().put(workspace.getKey(), workspace);
        LOGGER.info("Updated " + workspace);
    }
}
