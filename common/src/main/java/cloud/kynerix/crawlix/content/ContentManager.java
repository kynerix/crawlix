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

@ApplicationScoped
public class ContentManager {

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

            LOGGER.debug("Saving content with key " + content.getKey());
            infinispanSchema.getContentCache(workspace, true).put(content.getKey(), content);
        }
    }

    public List<Content> search(Workspace workspace, String optionalCacheName, String filter) throws Exception {
        return search(workspace, optionalCacheName, filter, 0, DEFAULT_MAX_RESULTS);
    }

    public List<Content> search(Workspace workspace, String optionalCacheName, String queryFilter, int startOffset, int maxResults) {
        RemoteCache<String, Content> cache = infinispanSchema.getContentCache(workspace, false);
        if (cache == null) {
            return Collections.emptyList();
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