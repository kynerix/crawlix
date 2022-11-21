package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.admin.AuthManager;
import cloud.kynerix.crawlix.content.ContentManager;
import cloud.kynerix.crawlix.nodes.CrawlerNodesManager;
import cloud.kynerix.crawlix.crawler.CrawlingJobsManager;
import cloud.kynerix.crawlix.crawler.PluginsManager;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseService {

    final Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName());

    @Inject
    PluginsManager pluginsManager;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    ContentManager contentManager;

    @Inject
    CrawlerNodesManager workerNodesManager;

    @Inject
    CrawlingJobsManager crawlingJobsManager;

    @Inject
    AuthManager authManager;

    private Map<String, Object> getResponseMap(boolean success, String message) {
        LOGGER.debug("Operation results: " + success + " - msg: " + message);
        Map<String, Object> results = new HashMap<>();
        results.put("success", success);
        if (message != null) {
            results.put("message", message);
        }
        return results;
    }

    Response operationResults(boolean success, String message) {
        return Response.accepted( getResponseMap(success, message) ).build();
    }

    Response noAuth() {
        return Response.accepted( getResponseMap(false, "Forbidden. Invalid authorization token.") ).status(Response.Status.FORBIDDEN).build();
    }
}
