package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.content.ContentManager;
import cloud.kynerix.crawlix.crawler.selenium.SeleniumCrawlerExecutor;
import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import cloud.kynerix.crawlix.workspaces.WorkspaceManager;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Random;

@ApplicationScoped
public class CrawlerNodeManager {

    @ConfigProperty(name = "crawler.node.key")
    String NODE_KEY;

    @ConfigProperty(name = "crawler.node.uri")
    String PUBLIC_URI;

    @ConfigProperty(name = "crawler.check.interval.sec", defaultValue = "60")
    long CHECK_INTERVAL_SEC;

    @ConfigProperty(name = "crawler.autostart", defaultValue = "true")
    boolean AUTOSTART;

    @ConfigProperty(name = "crawler.max.consecutive.failures", defaultValue = "3")
    int MAX_CONSECUTIVE_FAILURES;

    private boolean stopCrawler = false;

    @Inject
    CrawlingJobsManager crawlingJobsManager;

    @Inject
    PluginsManager pluginsManager;

    @Inject
    InfinispanSchema infinispanSchema;

    @Inject
    SeleniumCrawlerExecutor crawlerExecutor;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    ContentManager contentManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerNodeManager.class.getName());

    public CrawlerNodeManager() {
    }

    public void init() {
        try {
            WorkerNode node = getWorkerNode();
            LOGGER.info("Starting node '" + node.getKey() + "' at URL '" + PUBLIC_URI + "'");
            if (AUTOSTART) {
                node.setStatus(WorkerNode.STATUS_READY);
                node.setLastStartTime(new Date());
            } else {
                node.setStatus(WorkerNode.STATUS_STOPPED);
            }
            node.setPublicURI(PUBLIC_URI);
            save(node);
        } catch (Exception e) {
            LOGGER.error("Error init node status for " + getWorkerNode(), e);
        }

        if (AUTOSTART) {
            LOGGER.info("AUTOSTART = true :: Starting crawler loop");
            start();
        } else {
            LOGGER.info("AUTOSTART = false :: Crawler node must be manually started");
        }
    }

    public void stop() {
        WorkerNode node = getWorkerNode();
        LOGGER.info("Stopping node " + node);
        node.setStatus(WorkerNode.STATUS_STOPPED);
        node.setMessage(null);
        save(node);
        stopCrawler = true;
    }

    public void start() {
        WorkerNode crawlerWorkerNode = getWorkerNode();
        crawlerWorkerNode.setLastStartTime(new Date());
        crawlerWorkerNode.setStatus(WorkerNode.STATUS_READY);
        save(crawlerWorkerNode);

        stopCrawler = false;
        new Thread(
                this::runLoop,
                "CrawliX worker node " + crawlerWorkerNode.getKey()
        ).start();
    }

    WorkerNode getWorkerNode() {
        WorkerNode crawlerWorkerNode = infinispanSchema.getNodesCache().get(NODE_KEY);
        if (crawlerWorkerNode == null) {
            crawlerWorkerNode = new WorkerNode();
            crawlerWorkerNode.setKey(NODE_KEY);
            save(crawlerWorkerNode);
        }
        return crawlerWorkerNode;
    }

    void save(WorkerNode node) {
        LOGGER.info("Updating node status " + node);
        infinispanSchema.getNodesCache().put(NODE_KEY, node);
    }

    public CrawlingResults runCrawlerExecution(Workspace workspace, CrawlingJob crawlingJob, Plugin plugin, boolean persistData) {

        CrawlingResults results = null;

        String nextCrawlerStatus;

        // Execute job
        String url = crawlingJob == null ? plugin.getDefaultURL() : crawlingJob.getURL();

        if (crawlingJobsManager.isURLVisited(workspace, url, plugin.getKey())) {
            LOGGER.debug("Already visited: " + url);
            nextCrawlerStatus = CrawlingJob.STATUS_FINISHED_OK;
        } else {
            results = crawlerExecutor.runCrawler(workspace, plugin, crawlingJob, persistData);

            if (results.isSuccess()) {
                LOGGER.debug("Crawl is successful. Deleting JOB");
                plugin.setLastUpdate(new Date());
                nextCrawlerStatus = CrawlingJob.STATUS_FINISHED_OK;

                // Mark URL as visited
                crawlingJobsManager.visitURL(workspace, plugin.getKey(), url);
            } else {
                nextCrawlerStatus = CrawlingJob.STATUS_FINISHED_ERR;
                LOGGER.debug("Crawl is NOT successful");
                if (crawlingJob != null) {
                    crawlingJob.setLastError("HTTP Code: " + results.getHttpCode());
                    crawlingJob.incFailures();
                    if (crawlingJob.getConsecutiveFailures() > MAX_CONSECUTIVE_FAILURES) {
                        LOGGER.error("Job reached max failures " + MAX_CONSECUTIVE_FAILURES + " : " + crawlingJob);
                        // Mark URL as visited
                        crawlingJobsManager.visitURL(workspace, plugin.getKey(), url);
                    }
                }
            }
        }

        if (crawlingJob != null) {
            crawlingJob.setStatus(nextCrawlerStatus);
        }

        return results;
    }

    public String getInjectedJS() {
        return crawlerExecutor.getJavascriptLibraryContent();
    }

    public void onInit(@Observes StartupEvent ev) {
        init();
    }

    void onStop(@Observes ShutdownEvent ev) {
        stop();
    }

    //
    //
    //

    void runLoop() {
        do {
            try {
                long time = CHECK_INTERVAL_SEC * 1000L + new Random().nextInt(1000);
                LOGGER.debug("Waiting time before retrying: " + time + "ms");
                Thread.currentThread().sleep(time);
            } catch (InterruptedException e) {
            }

            for (Workspace workspace : workspaceManager.getWorkspaces()) {
                LOGGER.debug("Checking workspace " + workspace.getKey());

                CrawlingJob crawlingJob = null;

                WorkerNode workerNode = getWorkerNode();

                try {
                    if (getWorkerNode().isActive()) {
                        // TX1: Lock job
                        crawlingJob = crawlingJobsManager.tryLockCrawlingJob(
                                workspace,
                                workerNode,
                                crawlingJobsManager.findPendingJobs(workspace)
                        );
                    }

                    if (crawlingJob == null) {
                        LOGGER.debug("No job found for worker " + workerNode.getKey());
                    } else {
                        Plugin plugin = pluginsManager.getPlugin(workspace, crawlingJob.getPlugin());
                        if (plugin == null) {
                            LOGGER.warn("Plugin is null for job " + crawlingJob);
                        } else {
                            crawlingJob.setWorkerNode(workerNode.getKey());
                            crawlingJob.setStatus(CrawlingJob.STATUS_RUNNING);
                            crawlingJob.setLastCrawlAttempt(new Date());
                            crawlingJobsManager.save(workspace, crawlingJob);
                            LOGGER.debug("Job locked " + crawlingJob);

                            CrawlingResults results = runCrawlerExecution(workspace, crawlingJob, plugin, true);
                            if (results.getHttpCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                                // Process not found
                                saveNotFound(workspace, results);
                            }

                            // Update job
                            crawlingJobsManager.save(workspace, crawlingJob);

                            // Update plugin
                            pluginsManager.save(workspace, plugin);
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("Error trying to lock crawler in node " + workerNode.getKey(), e);
                }
            }
        } while (!stopCrawler);

        LOGGER.info("Crawler " + getWorkerNode() + " STOPPED!");
    }

    void saveNotFound(Workspace workspace, CrawlingResults results) {
        Content content = new Content();
        content.setFoundTime(new Date());
        content.setType("404");
        content.setUrl(results.getUrl());
        content.setPlugin(results.getPlugin());
        content.setTitle("NOT FOUND: " + results.getUrl());
        contentManager.save(workspace, content);
    }
}