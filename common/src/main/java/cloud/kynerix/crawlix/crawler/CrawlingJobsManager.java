package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.infinispan.client.hotrod.MetadataValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CrawlingJobsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlingJobsManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    public CrawlingJob getJob(Workspace workspace, long id) {
        return infinispanSchema.getJobsCache(workspace).get(id);
    }

    public CrawlingJob newJob(Workspace workspace, String pluginKey, String url) {
        CrawlingJob newCrawlingJob = new CrawlingJob();
        newCrawlingJob.setId(infinispanSchema.nextJobId());
        newCrawlingJob.setUrl(url);
        newCrawlingJob.setWorkspace(workspace.getKey());
        newCrawlingJob.setConsecutiveFailures(0);
        newCrawlingJob.setPlugin(pluginKey);
        newCrawlingJob.setStatus(CrawlingJob.STATUS_WAITING);
        LOGGER.debug("Crawling Job created " + newCrawlingJob);
        return newCrawlingJob;
    }

    public void save(Workspace workspace, CrawlingJob crawlingJob) {
        if (crawlingJob != null) {
            if (crawlingJob.getId() == null) {
                crawlingJob.setId(infinispanSchema.nextJobId());
            }
            infinispanSchema.getJobsCache(workspace).put(crawlingJob.getId(), crawlingJob);
        }
    }

    public void delete(Workspace workspace, long id) {
        infinispanSchema.getJobsCache(workspace).remove(id);
    }

    public void deletePluginJobs(Workspace workspace, String plugin) {
        List<CrawlingJob> crawlingJobs = findJobs(workspace, plugin);
        for (CrawlingJob j : crawlingJobs) {
            delete(workspace, j.getId());
        }
    }

    public List<CrawlingJob> findPendingJobs(Workspace workspace) {
        List jobs = infinispanSchema.getQueryFactory(infinispanSchema.getJobsCache(workspace))
                .create("FROM crawlix.CrawlingJob j " +
                        "WHERE status=:status " +
                        "ORDER BY j.lastCrawlAttempt, j.plugin DESC")
                .setParameter("status", CrawlingJob.STATUS_WAITING)
                .execute()
                .list();

        return jobs;
    }

    public CrawlingJob tryLockCrawlingJob(Workspace workspace, WorkerNode node, CrawlingJob job) {
        if (node == null) return null;

        LOGGER.info("Node '" + node.getKey() + "' trying to lock : " + job.getId());
        MetadataValue<CrawlingJob> crawlerStatusMetadata = infinispanSchema.getJobsCache(workspace).getWithMetadata(job.getId());

        job.setWorkerNode(node.getKey());
        job.setStatus(CrawlingJob.STATUS_RUNNING);
        job.setLastCrawlAttempt(new Date());

        boolean success = infinispanSchema.getJobsCache(workspace).replaceWithVersion(
                job.getId(),
                job,
                crawlerStatusMetadata.getVersion()
        );

        if (success) {
            // Update message
            LOGGER.info("Locked " + job.getId());
            return job;
        } else {
            LOGGER.info("Failed acquiring lock of " + job.getId());
            return null;
        }
    }

    public CrawlingJob tryLockCrawlingJob(Workspace workspace, WorkerNode node, List<CrawlingJob> jobs) {
        for (CrawlingJob j : jobs) {
            if (tryLockCrawlingJob(workspace, node, j) != null) {
                return j;
            }
        }
        return null;
    }

    public List<CrawlingJob> findJobs(Workspace workspace, String pluginKey) {
        if (pluginKey == null) return findAllJobs(workspace);

        List jobs = infinispanSchema.getQueryFactory(infinispanSchema.getJobsCache(workspace))
                .create("FROM crawlix.CrawlingJob j " +
                        "WHERE plugin=:plugin " +
                        "ORDER BY j.lastCrawlAttempt DESC")
                .setParameter("plugin", pluginKey)
                .execute()
                .list();

        return jobs;
    }

    public List<CrawlingJob> findAllJobs(Workspace workspace) {
        return infinispanSchema.getJobsCache(workspace).values().stream().collect(Collectors.toList());
    }

    public void visitURL(Workspace workspace, String pluginKey, String url) {
        infinispanSchema.getVisitedURLCache(workspace).put(pluginKey + "_" + url, "");
    }

    public boolean isURLVisited(Workspace workspace, String pluginKey, String url) {
        return infinispanSchema.getVisitedURLCache(workspace).containsKey(pluginKey + "_" + url);
    }

    public void cleanVisitedURLS(Workspace workspace, String pluginKey) {
        infinispanSchema.getVisitedURLCache(workspace).clear();
    }
}
