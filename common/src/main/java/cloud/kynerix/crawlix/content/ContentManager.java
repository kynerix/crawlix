package cloud.kynerix.crawlix.content;


import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ContentManager {

    @ConfigProperty(name = "crawlix.content.default.lifespan.hours", defaultValue = "48")
    private long DEFAULT_CONTENT_LIFESPAN;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    public void save(Workspace workspace, Content content) {
        if (content != null) {
            if (content.getId() == null) {
                content.setId(infinispanSchema.nextContentId());
            }
            if (content.getKey() == null) {
                content.setKey("CXID_" + content.getId());
            }

            LOGGER.debug("Saving content in store :" + content.getStore() + " with key " + content.getKey());
            infinispanSchema.getContentCache(workspace, content.getStore()).put(content.getKey(), content, DEFAULT_CONTENT_LIFESPAN, TimeUnit.HOURS);
        }
    }
}