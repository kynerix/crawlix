package cloud.kynerix.crawlix.controller;

import cloud.kynerix.crawlix.crawler.CrawlJobsManager;
import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.crawler.CrawlerStatsManager;
import cloud.kynerix.crawlix.crawler.CrawlersManager;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class CrawlingJobsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsController.class.getName());

    @Inject
    CrawlersManager crawlersManager;

    @Inject
    CrawlJobsManager crawlJobsManager;

    @Inject
    WorkspaceManager workspaceManager;

    @Scheduled(every = "60s", delay = 1)
    void createJobs() {
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            try {
                int nJobsCreated = 0;
                LOGGER.debug("Creating seed jobs for workspace " + workspace.getKey());

                // TODO: Split crawlers ownership if there are multiple controllers to avoid conflicts
                for (Crawler crawler : crawlersManager.getAllCrawlers(workspace, Crawler.STATUS_ENABLED)) {
                    if (crawlJobsManager.createSeedJobIfNeeded(crawler) != null) {
                        nJobsCreated++;
                    }
                }

                LOGGER.debug("Jobs created: " + nJobsCreated);
            } catch (Exception e) {
                LOGGER.error("Error creating jobs for workspace " + workspace.getKey(), e);
            }
        }
    }
}
