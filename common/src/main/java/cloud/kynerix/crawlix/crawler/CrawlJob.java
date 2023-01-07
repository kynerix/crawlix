package cloud.kynerix.crawlix.crawler;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

@ProtoDoc("@Indexed")
public class CrawlJob {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FINISHED_OK = "OK";
    public static final String STATUS_FINISHED_ERR = "ERROR";

    public static final String ACTION_PARSE = "PARSE";
    public static final String ACTION_CHECK = "CHECK";

    private Long id;
    private String action;
    private String crawlerKey;
    private String workspace;
    private String status;
    private String URL;
    private String context;
    private String parentURL;
    private String workerNode;
    private String lastError;
    private Date lastCrawlAttempt = null;
    private Date lastSuccessCrawl = null;
    private int consecutiveFailures = 0;

    public CrawlJob() {
    }

    @ProtoField(number = 1, required = true)
    public Long getId() {
        return id;
    }

    @ProtoField(number = 2)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getStatus() {
        return status;
    }

    @ProtoField(number = 3, required = true,name = "URL")
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getURL() {
        return URL;
    }

    @ProtoField(number = 4)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getCrawlerKey() {
        return crawlerKey;
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
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public Date getLastCrawlAttempt() {
        return lastCrawlAttempt;
    }

    @ProtoField(number = 9)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public Date getLastSuccessCrawl() {
        return lastSuccessCrawl;
    }

    @ProtoField(number = 10, required = true, defaultValue = "0")
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    @ProtoField(number = 11)
    public String getParentURL() {
        return parentURL;
    }

    @ProtoField(number = 12, defaultValue = ACTION_PARSE)
    public String getAction() {
        return action;
    }

    @ProtoField(number = 13)
    public String getContext() {
        return context;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCrawlerKey(String crawlerKey) {
        this.crawlerKey = crawlerKey;
    }

    public void setURL(String URL) {
        this.URL = URL;
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

    public void setParentURL(String parentURL) {
        this.parentURL = parentURL;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void incFailures() {
        this.consecutiveFailures++;
    }

    @Override
    public String toString() {
        return "CrawlJob{" +
                "id=" + id +
                ", action='" + action + '\'' +
                ", crawlerId='" + crawlerKey + '\'' +
                ", workspace='" + workspace + '\'' +
                ", status='" + status + '\'' +
                ", URL='" + URL + '\'' +
                ", context='" + context + '\'' +
                ", parentURL='" + parentURL + '\'' +
                ", workerNode='" + workerNode + '\'' +
                ", lastError='" + lastError + '\'' +
                ", lastCrawlAttempt=" + lastCrawlAttempt +
                ", lastSuccessCrawl=" + lastSuccessCrawl +
                ", consecutiveFailures=" + consecutiveFailures +
                '}';
    }
}
