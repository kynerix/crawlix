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
import java.util.OptionalLong;
import java.util.stream.Collectors;

@ApplicationScoped
public class CrawlJobsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlJobsManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    CrawlersManager crawlersManager;

    public CrawlJob getJob(Workspace workspace, long id) {
        return infinispanSchema.getJobsCache(workspace).get(id);
    }

    public CrawlJob newJob(Workspace workspace, String crawlerKey, String url, String parentURL) {
        CrawlJob newCrawlJob = new CrawlJob();
        newCrawlJob.setId(infinispanSchema.nextJobId());
        newCrawlJob.setURL(url);
        newCrawlJob.setWorkspace(workspace.getKey());
        newCrawlJob.setConsecutiveFailures(0);
        newCrawlJob.setCrawlerKey(crawlerKey);
        newCrawlJob.setStatus(CrawlJob.STATUS_WAITING);
        newCrawlJob.setParentURL(parentURL);

        LOGGER.debug("Crawling Job created " + newCrawlJob);
        return newCrawlJob;
    }

    public void save(Workspace workspace, CrawlJob crawlJob) {
        if (crawlJob != null) {
            if (crawlJob.getId() == null) {
                crawlJob.setId(infinispanSchema.nextJobId());
            }
            infinispanSchema.getJobsCache(workspace).put(crawlJob.getId(), crawlJob);
        }
    }

    public void delete(Workspace workspace, long id) {
        infinispanSchema.getJobsCache(workspace).remove(id);
    }

    public void deleteJobs(Workspace workspace, String crawlerKey) {
        List<CrawlJob> crawlJobs = findJobs(workspace, crawlerKey);
        for (CrawlJob j : crawlJobs) {
            delete(workspace, j.getId());
        }
    }

    public List<CrawlJob> findPendingJobs(Workspace workspace) {
        List jobs = infinispanSchema.getQueryFactory(infinispanSchema.getJobsCache(workspace))
                .create("FROM crawlix.CrawlJob j " +
                        "WHERE status=:status " +
                        "ORDER BY j.lastCrawlAttempt, j.crawlerKey DESC")
                .setParameter("status", CrawlJob.STATUS_WAITING)
                .execute()
                .list();

        return jobs;
    }

    public CrawlJob tryLockCrawlingJob(Workspace workspace, WorkerNode node, CrawlJob job) {
        if (node == null) return null;

        LOGGER.info("Node '" + node.getKey() + "' trying to lock : " + job.getId());
        MetadataValue<CrawlJob> crawlerStatusMetadata = infinispanSchema.getJobsCache(workspace).getWithMetadata(job.getId());

        job.setWorkerNode(node.getKey());
        job.setStatus(CrawlJob.STATUS_RUNNING);
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

    public CrawlJob tryLockCrawlingJob(Workspace workspace, WorkerNode node, List<CrawlJob> jobs) {
        for (CrawlJob j : jobs) {
            if (tryLockCrawlingJob(workspace, node, j) != null) {
                return j;
            }
        }
        return null;
    }

    public List<CrawlJob> findJobs(Workspace workspace, String crawlerKey) {
        if (crawlerKey == null) return findAllJobs(workspace);

        List jobs = infinispanSchema.getQueryFactory(infinispanSchema.getJobsCache(workspace))
                .create("FROM crawlix.CrawlJob j " +
                        "WHERE crawlerKey=:crawlerKey " +
                        "ORDER BY j.lastCrawlAttempt DESC")
                .setParameter("crawlerKey", crawlerKey)
                .execute()
                .list();

        return jobs;
    }

    public List<CrawlJob> findAllJobs(Workspace workspace) {
        return infinispanSchema.getJobsCache(workspace).values().stream().collect(Collectors.toList());
    }

    public void visitURL(Workspace workspace, String crawlerKey, String url) {
        VisitedURL visitedURL = new VisitedURL();
        visitedURL.setUrl(url);
        visitedURL.setCrawlerKey(crawlerKey);
        visitedURL.setDate(new Date());
        infinispanSchema.getVisitedURLCache(workspace).put(crawlerKey + "|" + url, visitedURL);
    }

    public boolean existsJob(Workspace workspace, String crawlerKey, String url) {
        OptionalLong nResults = infinispanSchema.getQueryFactory(infinispanSchema.getJobsCache(workspace))
                .create("FROM crawlix.CrawlJob j " +
                        "WHERE crawlerKey=:crawlerKey AND URL=:url " +
                        "ORDER BY j.lastCrawlAttempt DESC")
                .setParameter("crawlerKey", crawlerKey)
                .setParameter("url", url)
                .execute().hitCount();
        return !nResults.isEmpty() && nResults.getAsLong() > 0;
    }

    public boolean isURLVisited(Workspace workspace, String crawlerKey, String url) {
        return infinispanSchema.getVisitedURLCache(workspace).containsKey(crawlerKey + "|" + url);
    }

    void cleanVisitedURLS(Workspace workspace, String crawlerKey) {
        infinispanSchema.getQueryFactory(infinispanSchema.getVisitedURLCache(workspace)).create(
                        "DELETE from crawlix.VisitedURL v where v.crawlerKey = :crawlerKey")
                .setParameter("crawlerKey", crawlerKey)
                .executeStatement();
    }

    public CrawlJob createSeedJobIfNeeded(Crawler crawler) {

        if (crawler == null) return null;

        try {
            Workspace workspace = crawlersManager.getWorkspace(crawler);

            if (!crawler.isActive()) return null;
            if (crawler.getLastStart() == null) return null;

            if ((crawler.getLastStart().getTime() > (System.currentTimeMillis() - crawler.getWatchFrequencySeconds() * 1000L)
                    || !findJobs(workspace, crawler.getKey()).isEmpty())) {
                return null;
            }

            // Create new seed CrawlJob
            cleanVisitedURLS(workspace, crawler.getKey());

            // Re-update script from source, in case is needed
            crawlersManager.updateScriptFromSource(crawler);
            crawler.setLastStart(new Date());

            crawlersManager.save(workspace, crawler);

            CrawlJob newCrawlJob = newJob(workspace, crawler.getKey(), crawler.getDefaultURL(), null);
            save(workspace, newCrawlJob);
            return newCrawlJob;
        } catch (Exception e) {
            LOGGER.error("Error in controller: crawler " + crawler.getKey(), e);
            return null;
        }
    }
}
