package cloud.kynerix.crawlix.controller;

import cloud.kynerix.crawlix.crawler.CrawlJob;
import cloud.kynerix.crawlix.crawler.CrawlJobsManager;
import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.crawler.CrawlersManager;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

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
                List<Crawler> crawlers = crawlersManager.getAllCrawlers(workspace, Crawler.STATUS_ENABLED);

                long now = System.currentTimeMillis();

                for (Crawler crawler : crawlers) {
                    if (crawler.isActive() &&
                            (crawler.getLastUpdate() == null // No previous attempts
                                    || (crawler.getLastUpdate().getTime() < now - crawler.getWatchFrequencySeconds() * 1000L))
                    ) {
                        LOGGER.debug("Checking existing jobs for crawler " + crawler.getKey());
                        List<CrawlJob> crawlJobs = crawlJobsManager.findJobs(workspace, crawler.getKey());
                        if (crawlJobs.isEmpty()) {
                            LOGGER.debug("Queue is empty. Creating new seed job.");
                            // Create new CrawlJob
                            crawlJobsManager.cleanVisitedURLS(workspace, crawler.getKey());
                            crawlersManager.checkScriptURLForUpdate(workspace, crawler);

                            CrawlJob newCrawlJob = crawlJobsManager.newJob(workspace, crawler.getKey(), crawler.getDefaultURL(), null);
                            crawlJobsManager.save(workspace, newCrawlJob);
                            nJobsCreated++;
                        }
                    }
                }

                LOGGER.debug("Jobs created: " + nJobsCreated);
            } catch (Exception e) {
                LOGGER.error("Error creating jobs for workspace " + workspace.getKey(), e);
            }
        }
    }
}
