package cloud.kynerix.crawlix.content;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Date;

@ProtoDoc("@Indexed")
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
    private String crawlerKey;

    public Content() {
    }

    @ProtoField(number = 1)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public Long getId() {
        return id;
    }

    @ProtoField(number = 2)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getKey() {
        return key;
    }

    @ProtoField(number = 3)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getType() {
        return type;
    }

    @ProtoField(number = 4)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getTitle() {
        return title;
    }

    @ProtoField(number = 5)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getSummary() {
        return summary;
    }

    @ProtoField(number = 6)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getBody() {
        return body;
    }

    @ProtoField(number = 7)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getAuthor() {
        return author;
    }

    @ProtoField(number = 8)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getUrl() {
        return url;
    }

    @ProtoField(number = 9)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public Date getFoundTime() {
        return foundTime;
    }

    @ProtoField(number = 10)
    @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
    public String getCrawlerKey() {
        return crawlerKey;
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

    public void setCrawlerKey(String crawlerKey) {
        this.crawlerKey = crawlerKey;
    }

    public void setType(String type) {
        this.type = type;
    }
}
