package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.content.Content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawlingResults {

    private Map<String, Object> parserResults = new HashMap<>();

    private boolean success = false;
    private int httpCode = 0;

    private String plugin;
    private String url;
    private long jobId;
    private List<String> pluginLogs;
    private List<Content> content;
    private List<CrawlingJob> crawlingJobs;
    private String error;

    public CrawlingResults() {
    }

    public Map<String, Object> getParserResults() {
        return parserResults;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPlugin() {
        return plugin;
    }

    public String getUrl() {
        return url;
    }

    public long getJobId() {
        return jobId;
    }

    public List<String> getPluginLogs() {
        return pluginLogs;
    }

    public List<Content> getContent() {
        return content;
    }

    public List<CrawlingJob> getCrawlingJobs() {
        return crawlingJobs;
    }

    public String getError() {
        return error;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public void setPluginLogs(List<String> pluginLogs) {
        this.pluginLogs = pluginLogs;
    }

    public void setContent(List<Content> content) {
        this.content = content;
    }

    public void setCrawlingJobs(List<CrawlingJob> crawlingJobs) {
        this.crawlingJobs = crawlingJobs;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }
}
