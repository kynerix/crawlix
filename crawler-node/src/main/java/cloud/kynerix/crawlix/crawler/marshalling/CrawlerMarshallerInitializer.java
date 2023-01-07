package cloud.kynerix.crawlix.crawler.marshalling;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.crawler.CrawlJob;
import cloud.kynerix.crawlix.crawler.Crawler;
import cloud.kynerix.crawlix.crawler.VisitedURL;
import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = {WorkerNode.class, CrawlJob.class, Crawler.class, Workspace.class, Content.class, VisitedURL.class}, schemaPackageName = "crawlix")
public interface CrawlerMarshallerInitializer extends SerializationContextInitializer {
}
