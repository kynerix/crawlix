## Architecture Overview

CrawliX is composed of the following main components, each one supported through a container image that can be escalated to multiple nodes.

![CrawliX architecture overview](images/arch-overview.png)

### 1. Key-Value data store, caching, indexing and search with Infinispan ###
[Infinispan](https://infinispan.org) is a scalable data store and caching provider. CrawliX takes advantage of some of its advanced capabilities,
such as dynamically creating caches, set expiration policies or querying data.
Using it through its Quarkus extension provides an easy mapping to the necessary Java objects, through simple annotations.

The data store acts as the central point of coordination for both the controller and the crawler nodes, to exchange data,
coordinate over queues of work and keep the parsing and extraction results, for later retrieval.

### 2. The CrawliX service ###
This set of containers provides the following Restful APIs:

- Administration of crawlers */crawlix*. Configure crawlers, execute them and get their status.
- Platform administration at */admin*. Manage workspaces and security tokens.
- Content search at */content* and GraphQL. Retrieve content by query.

### 3. The crawler nodes ###
Each crawler node container has an **embedded Chromium browser**, that is initialized and controlled for every crawling job. Each node
has an inner loop that looks for pending crawling jobs, lock one of them and runs the headless browser. The necessary
Javascript is injected and the execution results are either store as content for later query, or subsequent crawl jobs are created.

### 4. The controller ###
This component takes care of setting up the system, and perform regular tasks:
- Create or update the Infinispan schema on startup
- Periodically, create the seed crawling jobs.

