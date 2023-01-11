package cloud.kynerix.crawlix.marshalling;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.crawler.*;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = {WorkerNode.class, CrawlJob.class, Crawler.class, Workspace.class, Content.class, VisitedURL.class, CrawlerStats.class}, schemaPackageName = "crawlix")
public interface ServiceMarshallerInitializer extends SerializationContextInitializer {
}
