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
    CrawlJobsManager crawlJobsManager;

    @Inject
    CrawlersManager crawlersManager;

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

    public CrawlResults runCrawlerExecution(Workspace workspace, CrawlJob crawlJob, Crawler crawler, boolean persistData) {

        CrawlResults results = null;

        String nextCrawlerStatus;

        // Execute job
        String url = crawlJob == null ? crawler.getDefaultURL() : crawlJob.getURL();

        if (crawlJobsManager.isURLVisited(workspace, url, crawler.getKey())) {
            LOGGER.debug("Already visited: " + url);
            nextCrawlerStatus = CrawlJob.STATUS_FINISHED_OK;
        } else {
            results = crawlerExecutor.runCrawler(workspace, crawler, crawlJob, persistData);

            if (results.isSuccess()) {
                LOGGER.debug("Crawl is successful. Deleting JOB");
                crawler.setLastUpdate(new Date());
                nextCrawlerStatus = CrawlJob.STATUS_FINISHED_OK;

                // Mark URL as visited
                crawlJobsManager.visitURL(workspace, crawler.getKey(), url);
            } else {
                nextCrawlerStatus = CrawlJob.STATUS_FINISHED_ERR;
                LOGGER.debug("Crawl is NOT successful");
                if (crawlJob != null) {
                    crawlJob.setLastError("HTTP Code: " + results.getHttpCode());
                    crawlJob.incFailures();
                    if (crawlJob.getConsecutiveFailures() > MAX_CONSECUTIVE_FAILURES) {
                        LOGGER.error("Job reached max failures " + MAX_CONSECUTIVE_FAILURES + " : " + crawlJob);
                        // Mark URL as visited
                        crawlJobsManager.visitURL(workspace, crawler.getKey(), url);
                    }
                }
            }
        }

        if (crawlJob != null) {
            crawlJob.setStatus(nextCrawlerStatus);
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
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }

            for (Workspace workspace : workspaceManager.getWorkspaces()) {
                LOGGER.debug("Checking workspace " + workspace.getKey());

                CrawlJob crawlJob = null;

                WorkerNode workerNode = getWorkerNode();
                try {
                    if (workerNode.isActive()) {
                        // TX1: Lock job
                        crawlJob = crawlJobsManager.tryLockCrawlingJob(
                                workspace,
                                workerNode,
                                crawlJobsManager.findPendingJobs(workspace)
                        );
                    }

                    if (crawlJob == null) {
                        LOGGER.debug("No job found for worker " + workerNode.getKey());
                    } else {
                        Crawler crawler = crawlersManager.getCrawler(workspace, crawlJob.getCrawlerKey());
                        if (crawler == null) {
                            LOGGER.warn("Crawler is null for job " + crawlJob);
                        } else if (!crawler.isActive()) {
                            LOGGER.warn("Crawler " + crawler.getKey() + " is inactive for job " + crawlJob);
                        } else {
                            crawlJob.setWorkerNode(workerNode.getKey());
                            crawlJob.setStatus(CrawlJob.STATUS_RUNNING);
                            crawlJob.setLastCrawlAttempt(new Date());

                            crawlJobsManager.save(workspace, crawlJob);
                            LOGGER.debug("Job locked " + crawlJob);

                            workerNode.setMessage("Running [ " + workspace.getKey() + " : " + crawler.getKey() + " : " + crawlJob.getId() + " ]");
                            save(workerNode);

                            CrawlResults results = runCrawlerExecution(workspace, crawlJob, crawler, true);
                            if (results.getHttpCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                                // Process not found
                                saveNotFound(workspace, results);
                            }

                            // Update job
                            crawlJobsManager.save(workspace, crawlJob);

                            // Update crawler
                            crawlersManager.save(workspace, crawler);

                            // Update node
                            workerNode.setMessage(null);
                            save(workerNode);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error trying to lock crawler in node " + workerNode.getKey(), e);
                    workerNode.setMessage("Error");
                    save(workerNode);
                }
            }
        } while (!stopCrawler);

        LOGGER.info("Crawler " + getWorkerNode() + " STOPPED!");
    }

    void saveNotFound(Workspace workspace, CrawlResults results) {
        Content content = new Content();
        content.setFoundTime(new Date());
        content.setType("404");
        content.setUrl(results.getUrl());
        content.setCrawlerKey(results.getCrawlerKey());
        content.setTitle("NOT FOUND: " + results.getUrl());
        contentManager.save(workspace, content);
    }
}