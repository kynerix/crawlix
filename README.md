# CrawliX - A Web Content Extraction platform

CrawliX is a crawling platform that can be configured to navigate asynchronously through multiple web pages, parse their content selectively, and save the retrieved text fragments, images and screenshots in one or multiple key-value stores, which are automatically created and managed according to the configuration of each crawler's needs. 

All the data is indexed automatically, and can be queried and retrieved through a simple Rest API, which makes it ready for easy consumption by other services and applications. Each entry is subject to some predefined expiration and retention policies.

The project is under active development, and currently provides the following main capabilities:

* Support for multiple workspaces, with full data isolation to enable multi-project and multi-tenancy capabilities.
* Simple token based authentication and authorization. Integration with OIDC providers will shortly arrive.
* REST API to create and manage crawler plugins, supporting both synchronous and asynchronous executions.
* Simple lightweight Javascript library that simplifies the creation of the crawlers (plugins) while allowing to use all the browser's capabilities if needed.
* The data retrieved in the browser gets stored in one or multiple persistent caches, ready for querying, with custom expiration policies.
* Discovered URLs allow for automatic site deep navigation.
* Scalability, multiple browser nodes can be deployed at once, and will coordinate among them to work over the crawling jobs queue.

CrawliX is based on [Quarkus](https://quarkus.io/), [Infinispan](https://infinispan.org) and [Selenium](https://www.selenium.dev/documentation/).

-----

## Applications:

There are countless applications for data extraction from websites and web applications, such as:
* Content aggregation
* Competitive analysis
* Website health monitoring
* Application integrations
* Website monitoring

-----

## Architecture Overview

CrawliX is composed of the following main components:

### 1. Shared Data Store and Caching with Infinispan ###
Infinispan is a scalable data store and caching provider. CrawliX takes advantage of some of its advanced capabilities, 
such as dynamically creating caches, set expiration policies or querying data. 
Using it through its Quarkus extension provides an easy mapping to the necessary Java objects, through simple annotations.
  
The data store acts as the central point of coordination for both the controller and the crawler nodes, to exchange data, 
coordinate over queues of work and keep the parsing and extraction results, for later retrieval.

### 2. The CrawliX service and controller ###
This set of containers provides a Restful API to:

- Install new crawlers and manage them
- Manage and query the status of crawlers
- Retrieve the extracted content and data
- Execute once the plugin, without writing the results to the database, for testing and diagnose purposes.

There's also a controller component that creates the seed crawling jobs, as needed.

### 3. The crawler nodes ###
Each crawler node container has an embedded Firefox, that is initialized and controlled for every crawling job. Each node
has an inner loop that looks for pending crawling jobs, lock one of them and runs the headless Firefox. The necessary 
Javascript is injected and the execution results are either store as content for later query, or subsequent crawl jobs are created.

### 4. Other components ###

- A set of Javascript libraries for simple DOM manipulation
- A simple management console and monitoring capabilities [TBD]


-----

## Running CrawliX in development mode

### Pre-requisites

- Java 11+
- Podman, for building and executing images. Docker can be used as well, although scripts are not provided, yet.
- Firefox, in the PATH
- jq tool in the PATH

### **Step 1**. Start Infinispan
```
podman rm infinispan

podman run --name infinispan --network=host -p 11222:11222 -e USER=kynerix -e PASS=crawlix quay.io/infinispan/server:latest
```

### **Step 2**. Start the Controller Service in Quarkus dev mode
```
cd controller/scripts

./dev-mode.sh 
```

The crawliX controller will start in **port 8079** and **debug port 5005**


### **Step 3**. Start one crawler node in Quarkus dev mode
```
cd crawler-node/scripts

./dev-mode.sh 
```

The crawliX node will register itself and will start in **port 8078** and **debug port 5004**

### **Step 4**: Check everything is in place:

- Access the Infinispan console with user *kynerix* and password *crawlix* at http://localhost:11222/console/welcome


- Check the service:
```
curl -s -X  GET "http://localhost:8079/crawlix/list-plugins" --header "Authorization: 00-DEFAULT-TOKEN-00" | jq
```

- Check the node's health:

```
curl -s -X GET -H "Content-Type: application/json" http://localhost:8079/crawlix-admin/list-nodes | jq --tab
```

### **Step 5**: Start developing with Quarkus

You'll find some API usage examples at **/crawlix-service/scripts/run-tests.sh**
