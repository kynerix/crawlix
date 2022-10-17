package cloud.kynerix.crawlix.workspaces;

import org.infinispan.protostream.annotations.ProtoField;

import java.util.ArrayList;
import java.util.List;

public class Workspace {
    private String key;
    private List<String> tokens = new ArrayList<>();
    private String name;

    @ProtoField(number = 1, required = true)
    public String getKey() {
        return key;
    }

    @ProtoField(number = 2)
    public List<String> getTokens() {
        return tokens;
    }

    @ProtoField(number = 3, required = true)
    public String getName() {
        return name;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public void addToken(String token) {
        this.tokens.add(token);
    }

    public void removeToken(String token) {
        this.tokens.remove(token);
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "key='" + key + '\'' +
                ", tokens=" + tokens +
                ", name='" + name + '\'' +
                '}';
    }
}
