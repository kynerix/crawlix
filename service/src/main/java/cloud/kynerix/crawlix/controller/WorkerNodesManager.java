package cloud.kynerix.crawlix.controller;

import cloud.kynerix.crawlix.crawler.WorkerNode;
import cloud.kynerix.crawlix.schema.InfinispanSchema;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class WorkerNodesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerNodesManager.class.getName());

    @Inject
    InfinispanSchema infinispanSchema;

    private WorkerNodeClient getWorkerNodeService(WorkerNode crawlerWorkerNode) throws URISyntaxException {
        URI apiUri = new URI(crawlerWorkerNode.getPublicURI());
        return RestClientBuilder.newBuilder()
                .baseUri(apiUri)
                .readTimeout(1, TimeUnit.HOURS)
                .connectTimeout(1, TimeUnit.MINUTES)
                .build(WorkerNodeClient.class);
    }

    public WorkerNode getNode(String nodeKey) {
        return infinispanSchema.getNodesCache().get(nodeKey);
    }

    public List<WorkerNode> getAllNodes() {
        return infinispanSchema.getNodesCache().values().stream().collect(Collectors.toList());
    }

    public List<WorkerNode> getAllActiveNodes() {
        return infinispanSchema.getNodesCache().values().stream()
                .filter(n -> n.isActive())
                .collect(Collectors.toList());
    }

    public WorkerNode getRandomNode() {
        // If localhost is registered, give it priority (for development)
        WorkerNode localNode = getNode(WorkerNode.LOCALHOST);
        if (localNode != null) {
            return localNode;
        }
        List<WorkerNode> nodes = getAllActiveNodes();
        if (!nodes.isEmpty()) {
            return nodes.get(new Random().nextInt(nodes.size()));
        } else {
            return null;
        }
    }

    public Response startNode(String node) {
        WorkerNode crawlerWorkerNode = getNode(node);
        try {
            if (crawlerWorkerNode != null) {
                LOGGER.info("Starting node " + node);
                WorkerNodeClient crawlerService = getWorkerNodeService(crawlerWorkerNode);
                return crawlerService.startNode();
            }
        } catch (Exception e) {
            LOGGER.error("Error starting node", e);
        }
        return Response.ok(Boolean.FALSE).build();
    }

    public Response stopNode(String node) {
        WorkerNode crawlerWorkerNode = getNode(node);
        try {
            if (crawlerWorkerNode != null) {
                LOGGER.info("Stopping  node " + node);
                return getWorkerNodeService(crawlerWorkerNode).stopNode();
            }
        } catch (Exception e) {
            LOGGER.error("Error starting node", e);
        }
        return null;
    }

    public Response getJavascript(String node) {
        WorkerNode crawlerWorkerNode = getNode(node);
        try {
            if (crawlerWorkerNode != null) {
                LOGGER.info("Retrieving javascript" + node);
                return getWorkerNodeService(crawlerWorkerNode).javascript();
            }
        } catch (Exception e) {
            LOGGER.error("Error starting node", e);
        }
        return null;
    }

    public Response executeRemoteCrawler(String node, String workspaceKey, String pluginKey, boolean storeResults) throws Exception {
        WorkerNode crawlerWorkerNode = getNode(node);
        if (crawlerWorkerNode != null) {
            LOGGER.info("Running remote crawler " + pluginKey + " in node " + node);
            return getWorkerNodeService(crawlerWorkerNode).execute(workspaceKey, pluginKey, storeResults);
        } else {
            return  null;
        }
    }
}
