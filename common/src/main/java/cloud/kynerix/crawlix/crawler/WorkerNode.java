package cloud.kynerix.crawlix.crawler;

import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

public class WorkerNode {

    public static final String LOCALHOST = "localhost";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_STOPPED = "STOPPED";

    private String key;
    private String publicURI;
    private String status;
    private String message;
    private Date lastStartTime = null;

    public boolean isActive() {
        return key.equalsIgnoreCase("localhost") || WorkerNode.STATUS_READY.equalsIgnoreCase(status);
    }

    @ProtoField(number = 1)
    public String getKey() {
        return key;
    }

    @ProtoField(number = 2)
    public String getPublicURI() {
        return publicURI;
    }

    @ProtoField(number = 3)
    public String getStatus() {
        return status;
    }

    @ProtoField(number = 4)
    public String getMessage() {
        return message;
    }

    @ProtoField(number = 5)
    public Date getLastStartTime() {
        return lastStartTime;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPublicURI(String publicURI) {
        this.publicURI = publicURI;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLastStartTime(Date lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    @Override
    public String toString() {
        return "WorkerNode{" +
                "key='" + key + '\'' +
                ", publicURI='" + publicURI + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", lastInit=" + lastStartTime +
                '}';
    }
}
