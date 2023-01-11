package cloud.kynerix.crawlix.crawler;

import cloud.kynerix.crawlix.utils.WatchFrequencyParser;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

public class Crawler {

    public final static String STATUS_ENABLED = "ENABLED";
    public final static String STATUS_DISABLED = "DISABLED";

    private String key;
    private String workspaceKey;
    private String status;
    private String defaultURL;
    private String script;
    private String scriptURL;

    private int loadPauseMs;
    private int browserWidth = 1280;
    private int browserHeight = 800;
    private String watchFrequency;
    private int watchFrequencySeconds;

    private String contextScript;

    private Date lastStart;

    private transient CrawlerStats stats;

    @ProtoField(number = 1)
    public String getKey() {
        return key;
    }

    @ProtoField(number = 2)
    public String getWorkspaceKey() {
        return workspaceKey;
    }

    @ProtoField(number = 3, defaultValue = STATUS_ENABLED)
    public String getStatus() {
        return status;
    }

    @ProtoField(number = 4)
    public String getDefaultURL() {
        return defaultURL;
    }

    @ProtoField(number = 5)
    public String getScript() {
        return script;
    }

    @ProtoField(number = 6, defaultValue = "1280")
    public int getBrowserWidth() {
        return browserWidth;
    }

    @ProtoField(number = 7, defaultValue = "800")
    public int getBrowserHeight() {
        return browserHeight;
    }

    @ProtoField(number = 8, defaultValue = "1000")
    public int getLoadPauseMs() {
        return loadPauseMs;
    }

    @ProtoField(number = 9, defaultValue = "3600")
    public int getWatchFrequencySeconds() {
        return watchFrequencySeconds;
    }

    @ProtoField(number = 10, defaultValue = "1d")
    public String getWatchFrequency() {
        return watchFrequency;
    }

    @ProtoField(number = 11)
    public String getScriptURL() {
        return scriptURL;
    }

    @ProtoField(number = 12)
    public String getContextScript() {
        return contextScript;
    }

    @ProtoField(number = 13)
    public Date getLastStart() {
        return lastStart;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDefaultURL(String defaultURL) {
        this.defaultURL = defaultURL;
    }

    public void setBrowserWidth(int browserWidth) {
        this.browserWidth = browserWidth;
    }

    public void setBrowserHeight(int browserHeight) {
        this.browserHeight = browserHeight;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public void setWatchFrequencySeconds(int watchFrequencySeconds) {
        this.watchFrequencySeconds = watchFrequencySeconds;
    }

    public void setWatchFrequency(String watchFrequency) {
        this.watchFrequency = watchFrequency;
    }

    public void setLoadPauseMs(int loadPauseMs) {
        this.loadPauseMs = loadPauseMs;
    }

    public boolean isActive() {
        return STATUS_ENABLED.equals(this.status);
    }

    public void setScriptURL(String scriptURL) {
        this.scriptURL = scriptURL;
    }

    public void setContextScript(String contextScript) {
        this.contextScript = contextScript;
    }

    public void setWorkspaceKey(String workspace) {
        this.workspaceKey = workspace;
    }

    public void setLastStart(Date lastStart) {
        this.lastStart = lastStart;
    }

    public boolean isValid() {
        setWatchFrequencySeconds(
                WatchFrequencyParser.parse(this.watchFrequency, 60 * 60)
        );
        return getKey() != null &&
                getKey().chars().allMatch((c) -> Character.isLetterOrDigit(c) || c == '-' || c == '_') &&
                getKey().length() >= 3;
    }

    public CrawlerStats getStats() {
        return stats;
    }

    public void setStats(CrawlerStats stats) {
        this.stats = stats;
    }

    @Override
    public String toString() {
        return "Crawler{" +
                "key='" + key + '\'' +
                ", workspaceKey='" + workspaceKey + '\'' +
                ", status='" + status + '\'' +
                ", defaultURL='" + defaultURL + '\'' +
                ", script='" + script + '\'' +
                ", scriptURL='" + scriptURL + '\'' +
                ", loadPauseMs=" + loadPauseMs +
                ", browserWidth=" + browserWidth +
                ", browserHeight=" + browserHeight +
                ", watchFrequency='" + watchFrequency + '\'' +
                ", watchFrequencySeconds=" + watchFrequencySeconds +
                ", contextScript='" + contextScript + '\'' +
                ", lastStart=" + lastStart +
                '}';
    }
}
