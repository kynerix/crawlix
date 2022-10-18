package cloud.kynerix.crawlix.schema;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.crawler.CrawlingJob;
import cloud.kynerix.crawlix.crawler.Plugin;
import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.counter.api.*;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Scanner;

@ApplicationScoped
public class InfinispanSchema {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanSchema.class.getName());

    private static final String PERSISTENT_CACHE_TEMPLATE_XML = "/persistent_cache_template.xml";

    private static final String CACHE_WORKSPACES = "CXA_WORKSPACES";
    private static final String CACHE_NODES = "CXA_NODES";
    private static final String COUNTER_ID_JOBS = "CXA_ID_JOBS";
    private static final String COUNTER_ID_CONTENT = "CXA_ID_CONTENT";

    private static final String CACHE_PLUGINS = "CXW_{WORKSPACE}_PLUGINS";
    private static final String CACHE_JOBS = "CXW_{WORKSPACE}_JOBS";
    private static final String CACHE_CONTENT = "CXW_{WORKSPACE}_CONTENT";
    private static final String CACHE_VISITED = "CXW_{WORKSPACE}_VISITED";

    @Inject
    RemoteCacheManager remoteCacheManager;

    RemoteCache getCache(String cacheName) {
        return remoteCacheManager.getCache(cacheName);
    }

    StrongCounter getIdGenerator(String counterName) {
        return RemoteCounterManagerFactory.asCounterManager(remoteCacheManager).getStrongCounter(counterName);
    }

    RemoteCache initCache(String cacheName, int entriesInMemory) {
        RemoteCache cache = remoteCacheManager.getCache(cacheName);
        if (cache == null) {
            LOGGER.info("Cache " + cacheName + " not found. Installing it:");
            long startTime = System.currentTimeMillis();
            cache = remoteCacheManager.administration().getOrCreateCache(cacheName,
                    new XMLStringConfiguration(getCacheConfiguration(cacheName, entriesInMemory))
            );
            LOGGER.info("Installed " + cacheName + " in " + (System.currentTimeMillis() - startTime) + " ms");
        }

        return cache;
    }

    StrongCounter initCounter(String counterName) {
        CounterManager counterManager = RemoteCounterManagerFactory.asCounterManager(remoteCacheManager);
        if (!counterManager.isDefined(counterName)) {
            LOGGER.info("Creating Infinispan counter " + counterName);
            long startTime = System.currentTimeMillis();
            counterManager.defineCounter(
                    counterName,
                    CounterConfiguration.builder(CounterType.UNBOUNDED_STRONG).storage(Storage.PERSISTENT).build()
            );
            LOGGER.info("Installed " + counterName + " in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return counterManager.getStrongCounter(counterName);
    }

    String getCacheConfiguration(String cacheName, int entriesInMemory) {
        // Absurd trick to avoid dealing with byte[]
        String xml = new Scanner(
                this.getClass().getResourceAsStream(PERSISTENT_CACHE_TEMPLATE_XML), "UTF-8")
                .useDelimiter("\\A")
                .next()
                .replace("$CACHE_NAME", cacheName)
                .replace("$ENTRIES_IN_MEM", String.valueOf(entriesInMemory));

        LOGGER.info(xml);
        return xml;
    }

    String normalize(String name) {
        return name
                .toUpperCase()
                .replace("/", "-")
                .replace(" ", "")
                .trim();
    }

    //
    // GLOBAL CACHES
    //

    public RemoteCache<String, WorkerNode> getNodesCache() {
        return (RemoteCache<String, WorkerNode>) this.getCache(InfinispanSchema.CACHE_NODES);
    }

    public RemoteCache<String, Workspace> getWorkspacesCache() {
        return (RemoteCache<String, Workspace>) this.getCache(InfinispanSchema.CACHE_WORKSPACES);
    }


    //
    // PER WORKSPACE CACHES
    //

    String buildCacheName(Workspace workspace, String name) {
        return name.replace("{WORKSPACE}", normalize(workspace.getKey()));
    }

    public RemoteCache<String, Plugin> getPluginsCache(Workspace workspace) {
        return (RemoteCache<String, Plugin>) this.getCache(buildCacheName(workspace, InfinispanSchema.CACHE_PLUGINS));
    }

    public RemoteCache<Long, CrawlingJob> getJobsCache(Workspace workspace) {
        return (RemoteCache<Long, CrawlingJob>) this.getCache(buildCacheName(workspace, InfinispanSchema.CACHE_JOBS));

    }


    public RemoteCache<String, String> getVisitedURLCache(Workspace workspace) {
        return (RemoteCache<String, String>) this.getCache(buildCacheName(workspace, InfinispanSchema.CACHE_VISITED));
    }

    String getContentCacheName(Workspace workspace, String customContentCache) {
        return buildCacheName(workspace, InfinispanSchema.CACHE_CONTENT)
                + (customContentCache == null ? "" : "_" + normalize(customContentCache));
    }

    public RemoteCache<String, Content> getContentCache(Workspace workspace, String contentCache, boolean createIfNotExist) {
        String cacheName = getContentCacheName(workspace, contentCache);
        RemoteCache cache = (RemoteCache<String, Content>) this.getCache(cacheName);
        if (cache == null && createIfNotExist) {
            cache = initCache(cacheName, 10);
        }
        return cache;
    }

    //
    // COUNTERS
    //

    public long nextContentId() {
        return getIdGenerator(COUNTER_ID_CONTENT).sync().incrementAndGet();
    }

    public long nextJobId() {
        return getIdGenerator(COUNTER_ID_JOBS).sync().incrementAndGet();
    }

    //
    // OTHER
    //

    public QueryFactory getQueryFactory(RemoteCache remoteCache) {
        return Search.getQueryFactory(remoteCache);
    }

    //
    // Schema initialization
    //

    public void initGlobalSchema() {
        initCache(CACHE_NODES, 100);
        initCache(CACHE_WORKSPACES, 100);
        initCounter(COUNTER_ID_JOBS);
        initCounter(COUNTER_ID_CONTENT);
    }

    public void initWorkspaceSchema(Workspace workspace) {
        initCache(buildCacheName(workspace, InfinispanSchema.CACHE_PLUGINS), 1000);
        initCache(buildCacheName(workspace, InfinispanSchema.CACHE_JOBS), 1000);
        initCache(buildCacheName(workspace, InfinispanSchema.CACHE_VISITED), 1000);
        initCache(getContentCacheName(workspace, null), 10);
    }
}