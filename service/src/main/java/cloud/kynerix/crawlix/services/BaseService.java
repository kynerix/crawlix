package cloud.kynerix.crawlix.services;

import cloud.kynerix.crawlix.content.ContentManager;
import cloud.kynerix.crawlix.controller.WorkerNodesManager;
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

    Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName());

    @Inject
    PluginsManager pluginsManager;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    ContentManager contentManager;

    @Inject
    WorkerNodesManager workerNodesManager;

    @Inject
    CrawlingJobsManager crawlingJobsManager;

    Response operationResults(boolean success, String message) {
        Map<String, Object> results = new HashMap<>();
        results.put("success", success);
        if (message != null) {
            results.put("message", message);
        }
        LOGGER.debug("Operation results: " + success + " - msg: " + message);
        return Response.accepted(results).build();
    }

    Response noAuth() {
        LOGGER.error("Invalid auth token");
        return Response.status(Response.Status.FORBIDDEN).build();
    }


}
