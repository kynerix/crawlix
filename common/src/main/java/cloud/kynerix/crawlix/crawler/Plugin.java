package cloud.kynerix.crawlix.crawler;

import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

public class Plugin {

    public final static String STATUS_ENABLED = "ENABLED";
    public final static String TEST_ONLY = "TEST-ONLY";
    public final static String STATUS_DISABLED = "DISABLED";

    private String key;
    private String status;
    private String defaultURL;
    private String script;
    private String scriptURL;

    private int loadPauseMs;
    private int browserWidth = 1280;
    private int browserHeight = 800;
    private int watchFrequencySeconds = 3600;

    private Date lastUpdate;

    private String contextScript;

    @ProtoField(number = 1)
    public String getKey() {
        return key;
    }

    @ProtoField(number = 2, defaultValue = STATUS_ENABLED)
    public String getStatus() {
        return status;
    }

    @ProtoField(number = 3)
    public String getDefaultURL() {
        return defaultURL;
    }

    @ProtoField(number = 4)
    public String getScript() {
        return script;
    }

    @ProtoField(number = 5, defaultValue = "1280")
    public int getBrowserWidth() {
        return browserWidth;
    }

    @ProtoField(number = 6, defaultValue = "800")
    public int getBrowserHeight() {
        return browserHeight;
    }

    @ProtoField(number = 7, defaultValue = "1000")
    public int getLoadPauseMs() {
        return loadPauseMs;
    }

    @ProtoField(number = 8, defaultValue = "3600")
    public int getWatchFrequencySeconds() {
        return watchFrequencySeconds;
    }

    @ProtoField(number = 9)
    public Date getLastUpdate() {
        return lastUpdate;
    }

    @ProtoField(number = 10)
    public String getScriptURL() {
        return scriptURL;
    }

    @ProtoField(number = 11)
    public String getContextScript() {
        return contextScript;
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

    public void setLoadPauseMs(int loadPauseMs) {
        this.loadPauseMs = loadPauseMs;
    }

    public void setWatchFrequencySeconds(int watchFrequencySeconds) {
        this.watchFrequencySeconds = watchFrequencySeconds;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    public boolean isValid() {
        return getKey() != null &&
                getKey().chars().allMatch((c) -> Character.isLetterOrDigit(c) || c == '-' || c == '_') &&
                getKey().length() >= 3;
    }

    @Override
    public String toString() {
        return "Plugin{" +
                "key='" + key + '\'' +
                ", status='" + status + '\'' +
                ", defaultURL='" + defaultURL + '\'' +
                ", script='" + script + '\'' +
                ", scriptURL='" + scriptURL + '\'' +
                ", loadPauseMs=" + loadPauseMs +
                ", browserWidth=" + browserWidth +
                ", browserHeight=" + browserHeight +
                ", watchFrequencySeconds=" + watchFrequencySeconds +
                ", lastUpdate=" + lastUpdate +
                ", contextScript='" + contextScript + '\'' +
                '}';
    }
}
