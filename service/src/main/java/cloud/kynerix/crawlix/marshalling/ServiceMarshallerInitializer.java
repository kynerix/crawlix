package cloud.kynerix.crawlix.marshalling;

import cloud.kynerix.crawlix.content.Content;
import cloud.kynerix.crawlix.crawler.Plugin;
import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.crawler.CrawlingJob;
import cloud.kynerix.crawlix.workspaces.Workspace;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = {WorkerNode.class, CrawlingJob.class, Plugin.class, Workspace.class, Content.class}, schemaPackageName = "crawlix")
public interface ServiceMarshallerInitializer extends SerializationContextInitializer {
}
