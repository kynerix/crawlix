package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

@ApplicationScoped
public class CrawlersManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlersManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    public Crawler getCrawler(Workspace workspace, String crawlerKey) {
        return infinispanSchema.getCrawlersCache(workspace).get(crawlerKey);
    }

    public Workspace getWorkspace(Crawler crawler) {
        if (crawler == null) return null;
        return workspaceManager.getWorkspaceByKey(crawler.getWorkspaceKey());
    }

    public void save(Crawler crawler) {
        save(getWorkspace(crawler), crawler);
    }

    public void save(Workspace workspace, Crawler crawler) {
        crawler.setWorkspaceKey(workspace.getKey());

        infinispanSchema.getCrawlersCache(workspace).put(crawler.getKey(), crawler);
    }

    public void delete(Workspace workspace, String crawlerKey) {
        infinispanSchema.getCrawlersCache(workspace).remove(crawlerKey);
    }

    public int size(Workspace workspace) {
        return infinispanSchema.getCrawlersCache(workspace).size();
    }

    public List<Crawler> getAllCrawlers() {
        List<Crawler> allCrawlers = new ArrayList<>();
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            allCrawlers.addAll(getAllCrawlers(workspace));
        }
        return allCrawlers;
    }

    public List<Crawler> getAllCrawlers(Workspace workspace) {
        return new ArrayList<>(infinispanSchema.getCrawlersCache(workspace).values());
    }

    public List<Crawler> getAllCrawlers(Workspace workspace, String status) {
        List<Crawler> filtered = getAllCrawlers(workspace);
        if (status != null) {
            filtered.removeIf(c -> !status.equalsIgnoreCase(c.getStatus()));
        }
        return filtered;
    }


    public void updateScriptFromSource(Crawler crawler) {
        if (crawler == null) return;
        if (crawler.getScriptURL() == null) return;

        // Try to load script from URL and update the script body if needed
        try {
            String scriptBody = new Scanner(new URL(crawler.getScriptURL()).openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();
            if (scriptBody != null && !scriptBody.trim().isEmpty()) {
                LOGGER.debug("Updating crawler script from " + crawler.getScriptURL());
                scriptBody = "// Updated from " + crawler.getScriptURL() + " at " + new Date() + "\n\r" + scriptBody;
                crawler.setScript(scriptBody);
            }
        } catch (IOException e) {
            LOGGER.error("Error loading script URL: " + crawler.getScriptURL(), e);
            if (crawler.getScript() == null || crawler.getScript().isBlank()) {
                // Disable crawler if there's not a previous version of the script
                crawler.setStatus(Crawler.STATUS_DISABLED);
            }
        }
    }
}
