package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class CrawlerStatsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerStatsManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    CrawlersManager crawlersManager;

    String getKey(String nodeKey, String crawlerKey) {
        return crawlerKey + "__" + nodeKey;
    }

    public CrawlerStats newEmptyStats(Crawler crawler, WorkerNode workerNode) {
        return newEmptyStats(crawler.getKey(), crawler.getWorkspaceKey(), workerNode.getKey());
    }

    Map<String, CrawlerStats> aggregateStatsByCrawler(Workspace workspace) {

        Map<String, CrawlerStats> crawlerStatsMap = new HashMap<>();
        CloseableIterator<CrawlerStats> it = infinispanSchema.getCrawlerStatsCache(workspace).values().iterator();
        try {
            while (it.hasNext()) {
                CrawlerStats stats = it.next();
                CrawlerStats crawlerStats = crawlerStatsMap.getOrDefault(stats.getCrawlerKey(), new CrawlerStats());
                crawlerStats.merge(stats);
                crawlerStatsMap.put(stats.getCrawlerKey(), crawlerStats);
            }
        } finally {
            it.close();
        }

        return crawlerStatsMap;
    }

    public CrawlerStats newEmptyStats(String crawlerKey, String workspaceKey, String nodeKey) {
        CrawlerStats stats = new CrawlerStats();
        stats.setCrawlerKey(crawlerKey);
        stats.setNodeKey(nodeKey);
        stats.setWorkspaceKey(workspaceKey);
        stats.setLastUpdate(new Date());
        return stats;
    }

    public void updateStats(CrawlerStats crawlerStats) {

        if (crawlerStats == null) return;

        Workspace workspace = workspaceManager.getWorkspaceByKey(crawlerStats.getWorkspaceKey());

        String key = getKey(crawlerStats.getNodeKey(), crawlerStats.getCrawlerKey());

        RemoteCache<String, CrawlerStats> cache = infinispanSchema.getCrawlerStatsCache(workspace);

        CrawlerStats nodeStats = cache.getOrDefault(key,
                newEmptyStats(crawlerStats.getCrawlerKey(), crawlerStats.getWorkspaceKey(), crawlerStats.getNodeKey()));

        nodeStats.merge(crawlerStats);

        // Update cache node stats
        cache.put(key, nodeStats);
    }


    public void refreshCrawlerStats(List<Crawler> crawlers) {
        Set<String> workspaces = crawlers.stream().map(Crawler::getWorkspaceKey).collect(Collectors.toSet());
        for (String workspaceKey : workspaces) {
            Workspace workspace = workspaceManager.getWorkspaceByKey(workspaceKey);

            Map<String, CrawlerStats> stats = aggregateStatsByCrawler(workspace);
            for (Crawler c : crawlers) {
                if (c.getWorkspaceKey().equals(workspaceKey)) {
                    // Update crawler statistics
                    c.setStats(stats.getOrDefault(c.getKey(), new CrawlerStats()));
                }
            }
        }
    }
}
