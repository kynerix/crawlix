package cloud.kynerix.crawlix.crawler;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;


@ProtoDoc("@Indexed")
public class CrawlerStats {

    private String crawlerKey;
    private String workspaceKey;
    private String nodeKey;

    private int contentCount;
    private int jobsCount;
    private int pagesCount;
    private int successCount;
    private int errorCount;
    private int notFoundCount;

    private String browserErrorCodes;
    private Date lastUpdate;

    public CrawlerStats() {
    }

    @ProtoField(number = 1, required = true)
    public String getCrawlerKey() {
        return crawlerKey;
    }

    @ProtoField(number = 2, required = true)
    public String getWorkspaceKey() {
        return workspaceKey;
    }

    @ProtoField(number = 3, required = true)
    public String getNodeKey() {
        return nodeKey;
    }

    @ProtoField(number = 4, required = true, defaultValue = "0")
    public int getContentCount() {
        return contentCount;
    }

    @ProtoField(number = 5, required = true, defaultValue = "0")
    public int getJobsCount() {
        return jobsCount;
    }

    @ProtoField(number = 6, required = true, defaultValue = "0")
    public int getPagesCount() {
        return pagesCount;
    }

    @ProtoField(number = 7, required = true, defaultValue = "0")
    public int getSuccessCount() {
        return successCount;
    }

    @ProtoField(number = 8, required = true, defaultValue = "0")
    public int getErrorCount() {
        return errorCount;
    }

    @ProtoField(number = 9, required = true, defaultValue = "0")
    public int getNotFoundCount() {
        return notFoundCount;
    }

    @ProtoField(number = 10, required = false)
    public String getBrowserErrorCodes() {
        return browserErrorCodes;
    }

    @ProtoField(number = 11, required = false)
    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setCrawlerKey(String crawlerKey) {
        this.crawlerKey = crawlerKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public void setContentCount(int contentCount) {
        this.contentCount = contentCount;
    }

    public void setJobsCount(int jobsCount) {
        this.jobsCount = jobsCount;
    }

    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public void setNotFoundCount(int notFoundCount) {
        this.notFoundCount = notFoundCount;
    }

    public void setBrowserErrorCodes(String browserErrorCodes) {
        this.browserErrorCodes = browserErrorCodes;
    }

    public void setWorkspaceKey(String workspaceKey) {
        this.workspaceKey = workspaceKey;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void merge(CrawlerStats stats) {
        this.jobsCount += stats.jobsCount;
        this.pagesCount += stats.pagesCount;
        this.successCount += stats.successCount;
        this.errorCount += stats.errorCount;

        if (stats.browserErrorCodes != null && !this.browserErrorCodes.contains(stats.browserErrorCodes)) {
            if (this.browserErrorCodes == null) {
                this.browserErrorCodes = "";
            }
            this.browserErrorCodes += stats.getBrowserErrorCodes().toUpperCase();
        }
    }
}
