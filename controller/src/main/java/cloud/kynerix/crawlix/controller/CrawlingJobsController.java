package cloud.kynerix.crawlix.controller;

import cloud.kynerix.crawlix.crawler.CrawlingJob;
import cloud.kynerix.crawlix.crawler.CrawlingJobsManager;
import cloud.kynerix.crawlix.crawler.Plugin;
import cloud.kynerix.crawlix.crawler.PluginsManager;
import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;

public class CrawlingJobsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsController.class.getName());

    @Inject
    PluginsManager pluginsManager;

    @Inject
    CrawlingJobsManager crawlingJobsManager;

    @Inject
    WorkspaceManager workspaceManager;

    @Scheduled(every = "60s", delay = 1)
    void createJobs() {
        for (Workspace workspace : workspaceManager.getWorkspaces()) {
            try {
                int nJobsCreated = 0;
                LOGGER.debug("Creating seed jobs for workspace " + workspace.getKey());

                // TODO: Split plugins ownership if there are multiple controllers to avoid conflicts
                List<Plugin> plugins = pluginsManager.getAllPlugins(workspace, Plugin.STATUS_ENABLED);

                long now = System.currentTimeMillis();

                for (Plugin plugin : plugins) {
                    if (plugin.isActive() &&
                            (plugin.getLastUpdate() == null // No previous attempts
                                    || (plugin.getLastUpdate().getTime() < now - plugin.getWatchFrequencySeconds() * 1000L))
                    ) {
                        LOGGER.debug("Checking existing jobs for plugin " + plugin.getKey());
                        List<CrawlingJob> crawlingJobs = crawlingJobsManager.findJobs(workspace, plugin.getKey());
                        if (crawlingJobs.isEmpty()) {
                            LOGGER.debug("Queue is empty. Creating new job.");
                            // Create new CrawlingJob
                            CrawlingJob newCrawlingJob = crawlingJobsManager.newJob(workspace, plugin.getKey(), plugin.getDefaultURL());
                            crawlingJobsManager.save(workspace, newCrawlingJob);
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
