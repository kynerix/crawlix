package cloud.kynerix.crawlix.crawler;

import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

public class CrawlingJob {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_RUNNING = "RUNNING";

    private Long id;
    private String plugin;
    private String workspace;
    private String status;
    private String url;
    private String workerNode;
    private String lastError;
    private Date lastCrawlAttempt = null;
    private Date lastSuccessCrawl = null;
    private int consecutiveFailures = 0;

    public CrawlingJob() {
    }

    @ProtoField(number = 1, required = true)
    public Long getId() {
        return id;
    }

    @ProtoField(number = 2, required = false)
    public String getStatus() {
        return status;
    }

    @ProtoField(number = 3, required = true)
    public String getUrl() {
        return url;
    }

    @ProtoField(number = 4)
    public String getPlugin() {
        return plugin;
    }

    @ProtoField(number = 5)
    public String getWorkspace() {
        return workspace;
    }

    @ProtoField(number = 6)
    public String getWorkerNode() {
        return workerNode;
    }

    @ProtoField(number = 7)
    public String getLastError() {
        return lastError;
    }

    @ProtoField(number = 8)
    public Date getLastCrawlAttempt() {
        return lastCrawlAttempt;
    }

    @ProtoField(number = 9)
    public Date getLastSuccessCrawl() {
        return lastSuccessCrawl;
    }

    @ProtoField(number = 10, required = true, defaultValue = "0")
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setWorkerNode(String workerNode) {
        this.workerNode = workerNode;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setLastCrawlAttempt(Date lastCrawlAttempt) {
        this.lastCrawlAttempt = lastCrawlAttempt;
    }

    public void setLastSuccessCrawl(Date lastSuccessCrawl) {
        this.lastSuccessCrawl = lastSuccessCrawl;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public String toString() {
        return "CrawlingJob{" +
                "id=" + id +
                ", plugin='" + plugin + '\'' +
                ", url='" + url + '\'' +
                ", workerNode='" + workerNode + '\'' +
                ", lastError='" + lastError + '\'' +
                ", lastCrawlAttempt=" + lastCrawlAttempt +
                ", lastSuccessCrawl=" + lastSuccessCrawl +
                ", consecutiveFailures=" + consecutiveFailures +
                '}';
    }
}
