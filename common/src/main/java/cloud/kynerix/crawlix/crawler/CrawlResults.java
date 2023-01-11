package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.content.Content;

import java.util.Date;
import java.util.List;

public class CrawlResults {

    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String ALREADY_VISITED = "ALREADY_VISITED";

    private String outcome;

    private String url;

    private Date date;
    private String crawlerKey;
    private String browserErrorCode;
    private String error;

    private String errorDetails;
    private String browserInfo;
    private List<String> crawlerLogs;
    private List<Content> content;
    private List<CrawlJob> crawlJobs;

    public CrawlResults() {
        super();
    }

    public String getUrl() {
        return url;
    }

    public Date getDate() {
        return date;
    }

    public String getCrawlerKey() {
        return crawlerKey;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getBrowserErrorCode() {
        return browserErrorCode;
    }

    public String getError() {
        return error;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getBrowserInfo() {
        return browserInfo;
    }

    public List<String> getCrawlerLogs() {
        return crawlerLogs;
    }

    public List<Content> getContent() {
        return content;
    }

    public List<CrawlJob> getCrawlJobs() {
        return crawlJobs;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }

    public void setCrawlerLogs(List<String> crawlerLogs) {
        this.crawlerLogs = crawlerLogs;
    }

    public void setContent(List<Content> content) {
        this.content = content;
    }

    public void setCrawlJobs(List<CrawlJob> crawlJobs) {
        this.crawlJobs = crawlJobs;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setCrawlerKey(String crawlerKey) {
        this.crawlerKey = crawlerKey;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public void setBrowserErrorCode(String browserErrorCode) {
        this.browserErrorCode = browserErrorCode;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isSuccessful() {
        return SUCCESS.equals(this.outcome);
    }

    @Override
    public String toString() {
        return "CrawlResults{" +
                "outcome='" + outcome + '\'' +
                ", url='" + url + '\'' +
                ", date=" + date +
                ", crawlerKey='" + crawlerKey + '\'' +
                ", browserErrorCode='" + browserErrorCode + '\'' +
                ", error='" + error + '\'' +
                ", errorDetails='" + errorDetails + '\'' +
                ", browserInfo='" + browserInfo + '\'' +
                ", content=" + (content == null ? "0" : content.size()) +
                ", crawlJobs=" + (crawlJobs == null ? "0" : crawlJobs.size()) +
                '}';
    }
}
