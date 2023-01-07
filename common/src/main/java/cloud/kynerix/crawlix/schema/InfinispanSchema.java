package cloud.kynerix.crawlix.schema;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.crawler.CrawlJob;
import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.crawler.VisitedURL;
import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@ApplicationScoped
public class InfinispanSchema {

    @ConfigProperty(name = "crawlix.lifespan.content.days", defaultValue = "10")
    int LIFESPAN_CONTENT_DAYS;

    @ConfigProperty(name = "crawlix.lifespan.jobs.days", defaultValue = "5")
    int LIFESPAN_JOBS_DAYS;

    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanSchema.class.getName());

    private static final String PERSISTENT_CACHE_TEMPLATE_XML = "/persistent_cache_template.xml";
    private static final String INDEXED_CACHE_TEMPLATE_XML = "/indexed_cache_template.xml";

    private static final String CACHE_WORKSPACES = "CXA_WORKSPACES";
    private static final String CACHE_NODES = "CXA_NODES";
    private static final String CACHE_SETTINGS = "CXA_SETTINGS";
    private static final String CACHE_TOKENS = "CXA_TOKENS";
    private static final String COUNTER_ID_JOBS = "CXA_ID_JOBS";
    private static final String COUNTER_ID_CONTENT = "CXA_ID_CONTENT";

    private static final String CACHE_CRAWLERS = "CXW_{WORKSPACE}_CRAWLERS";
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

    RemoteCache initCache(String cacheName, String entity, int entriesInMemory, int lifespanInDays) {
        RemoteCache cache = remoteCacheManager.getCache(cacheName);
        if (cache == null) {
            LOGGER.info("Cache " + cacheName + " not found. Installing it:");
            cache = remoteCacheManager.administration().getOrCreateCache(cacheName,
                    new XMLStringConfiguration(getCacheConfiguration(cacheName, entity, entriesInMemory, lifespanInDays))
            );
            LOGGER.info("Installed " + cacheName);
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

    String getCacheConfiguration(String cacheName, String indexedEntity, int entriesInMemory, long lifespanInDays) {
        // Absurd trick to avoid dealing with byte[]
        String template = indexedEntity == null ? PERSISTENT_CACHE_TEMPLATE_XML : INDEXED_CACHE_TEMPLATE_XML;
        long lifespanInMs = -1;
        if (lifespanInDays > 0) {
            lifespanInMs = lifespanInDays * 24 * 60 * 60 * 1000L;
        }

        String xml = new Scanner(
                this.getClass().getResourceAsStream(template), StandardCharsets.UTF_8)
                .useDelimiter("\\A")
                .next()
                .replace("$CACHE_NAME", cacheName)
                .replace("$ENTRIES_IN_MEM", String.valueOf(entriesInMemory))
                .replace("$ENTITY", String.valueOf(indexedEntity))
                .replace("$LIFESPAN_DAYS", lifespanInDays == -1 ? "Disabled" : lifespanInDays + " days")
                .replace("$LIFESPAN", String.valueOf(lifespanInMs));

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

    public RemoteCache<String, String> getGlobalSettingsCache() {
        return (RemoteCache<String, String>) this.getCache(InfinispanSchema.CACHE_SETTINGS);
    }

    public RemoteCache<String, String> getTokensCache() {
        return (RemoteCache<String, String>) this.getCache(InfinispanSchema.CACHE_TOKENS);
    }

    //
    // PER WORKSPACE CACHES
    //

    String buildCacheName(Workspace workspace, String name) {
        return name.replace("{WORKSPACE}", normalize(workspace.getKey()));
    }

    public RemoteCache<String, Crawler> getCrawlersCache(Workspace workspace) {
        return (RemoteCache<String, Crawler>) this.getCache(buildCacheName(workspace, InfinispanSchema.CACHE_CRAWLERS));
    }

    public RemoteCache<Long, CrawlJob> getJobsCache(Workspace workspace) {
        return (RemoteCache<Long, CrawlJob>) this.getCache(buildCacheName(workspace, InfinispanSchema.CACHE_JOBS));

    }


    public RemoteCache<String, VisitedURL> getVisitedURLCache(Workspace workspace) {
        return (RemoteCache<String, VisitedURL>) this.getCache(buildCacheName(workspace, InfinispanSchema.CACHE_VISITED));
    }

    String getContentCacheName(Workspace workspace) {
        return buildCacheName(workspace, InfinispanSchema.CACHE_CONTENT);
    }

    public RemoteCache<String, Content> getContentCache(Workspace workspace, boolean createIfNotExist) {
        String cacheName = getContentCacheName(workspace);
        RemoteCache cache = (RemoteCache<String, Content>) this.getCache(cacheName);
        if (cache == null && createIfNotExist) {
            cache = initCache(cacheName, "crawlix.Content", 10, LIFESPAN_CONTENT_DAYS);
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
        initCache(CACHE_NODES, null, 100, -1);
        initCache(CACHE_WORKSPACES, null, 100, -1);
        initCache(CACHE_SETTINGS, null, 100, -1);
        initCache(CACHE_TOKENS, null, 100, -1);
        initCounter(COUNTER_ID_JOBS);
        initCounter(COUNTER_ID_CONTENT);
    }

    public void initWorkspaceSchema(Workspace workspace) {
        initCache(buildCacheName(workspace, InfinispanSchema.CACHE_CRAWLERS), null, 1000, -1);
        initCache(buildCacheName(workspace, InfinispanSchema.CACHE_JOBS), "crawlix.CrawlJob", 1000, LIFESPAN_JOBS_DAYS);
        initCache(buildCacheName(workspace, InfinispanSchema.CACHE_VISITED), null, 1000, -1);
        initCache(getContentCacheName(workspace), "crawlix.Content", 10, LIFESPAN_CONTENT_DAYS);
    }
}
