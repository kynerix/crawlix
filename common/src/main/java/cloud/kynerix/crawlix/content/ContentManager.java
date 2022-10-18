package cloud.kynerix.crawlix.content;

import cloud.kynerix.crawlix.schema.InfinispanSchema;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.query.dsl.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ContentManager {

    @ConfigProperty(name = "crawlix.content.default.lifespan.hours", defaultValue = "48")
    private long DEFAULT_CONTENT_LIFESPAN;

    @ConfigProperty(name = "crawlix.content.default.max.results", defaultValue = "1000")
    private int DEFAULT_MAX_RESULTS;

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
            infinispanSchema.getContentCache(workspace, content.getStore(), true).put(content.getKey(), content, DEFAULT_CONTENT_LIFESPAN, TimeUnit.HOURS);
        }
    }

    public List<Content> search(Workspace workspace, String optionalCacheName, String filter) throws Exception  {
        return search(workspace, optionalCacheName, filter, 0, DEFAULT_MAX_RESULTS);
    }

    public List<Content> search(Workspace workspace, String optionalCacheName, String queryFilter, int startOffset, int maxResults) throws Exception {
        RemoteCache<String, Content> cache = infinispanSchema.getContentCache(workspace, optionalCacheName, false);
        if (cache == null) {
            return Collections.EMPTY_LIST;
        }

        String query = "FROM crawlix.Content";
        if (queryFilter != null && !queryFilter.isBlank()) {
            query += " WHERE " + queryFilter;
        }

        LOGGER.debug("Executing query: " + query);

        QueryResult queryResult = infinispanSchema.getQueryFactory(cache).create(query).startOffset(startOffset).maxResults(maxResults).execute();
        List<Content> results = queryResult.list();
        LOGGER.debug(results.size() + " results found of " + queryResult.hitCount());
        return results;
    }
}