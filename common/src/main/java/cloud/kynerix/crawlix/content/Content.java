package cloud.kynerix.crawlix.content;

import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

public class Content {

    private Long id;
    private String key;
    private String type;
    private String title;
    private String summary;
    private String body;
    private String author;
    private String url;
    private Date foundTime;
    private String plugin;
    private String store;

    public Content() {
    }

    @ProtoField(number = 1)
    public Long getId() {
        return id;
    }

    @ProtoField(number = 2)
    public String getKey() {
        return key;
    }

    @ProtoField(number = 3)
    public String getType() {
        return type;
    }

    @ProtoField(number = 4)
    public String getTitle() {
        return title;
    }

    @ProtoField(number = 5)
    public String getSummary() {
        return summary;
    }

    @ProtoField(number = 6)
    public String getBody() {
        return body;
    }

    @ProtoField(number = 7)
    public String getAuthor() {
        return author;
    }

    @ProtoField(number = 8)
    public String getUrl() {
        return url;
    }

    @ProtoField(number = 9)
    public Date getFoundTime() {
        return foundTime;
    }

    @ProtoField(number = 10)
    public String getPlugin() {
        return plugin;
    }

    @ProtoField(number = 11)
    public String getStore() {
        return store;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setFoundTime(Date foundTime) {
        this.foundTime = foundTime;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStore(String store) {
        this.store = store;
    }
}
