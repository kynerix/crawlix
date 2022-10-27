package cloud.kynerix.crawlix.crawler;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

@ProtoDoc("@Indexed")
public class VisitedURL {
    private String url;
    private String plugin;
    private Date date;

    @ProtoField(number = 1)
    public String getUrl() {
        return url;
    }

    @ProtoField(number = 2)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getPlugin() {
        return plugin;
    }

    @ProtoField(number = 3)
    public Date getDate() {
        return date;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "VisitedURL{" +
                "url='" + url + '\'' +
                ", plugin='" + plugin + '\'' +
                ", date=" + date +
                '}';
    }
}
