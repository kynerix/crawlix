package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.content.Content;

import java.util.List;

public class CrawlResults {

    private boolean success = false;
    private int httpCode = 0;

    private String crawlerKey;
    private String url;
    private long jobId;
    private String error;
    private String browserErrorCode;
    private String errorDetails;
    private String browserInfo;
    private List<String> crawlerLogs;
    private List<Content> content;
    private List<CrawlJob> crawlJobs;

    public CrawlResults() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCrawlerKey() {
        return crawlerKey;
    }

    public String getUrl() {
        return url;
    }

    public long getJobId() {
        return jobId;
    }

    public List<String> getCrawlerLogs() {
        return crawlerLogs;
    }

    public List<Content> getContent() {
        return content;
    }

    public List<CrawlJob> getCrawlingJobs() {
        return crawlJobs;
    }

    public String getError() {
        return error;
    }

    public String getBrowserErrorCode() {
        return browserErrorCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getBrowserInfo() {
        return browserInfo;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setCrawlerKey(String crawlerKey) {
        this.crawlerKey = crawlerKey;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public void setCrawlerLogs(List<String> crawlerLogs) {
        this.crawlerLogs = crawlerLogs;
    }

    public void setContent(List<Content> content) {
        this.content = content;
    }

    public void setCrawlingJobs(List<CrawlJob> crawlJobs) {
        this.crawlJobs = crawlJobs;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }

    public void setBrowserErrorCode(String browserErrorCode) {
        this.browserErrorCode = browserErrorCode;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
