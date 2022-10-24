# CrawliX - A Web Content Extraction platform

CrawliX is an **scalable crawling platform** that navigation and parsing of websites and applications, through the use of headless browsers, 
to **extract and store custom-defined content fragments and metrics**, for later retrieval, analysis and querying. 

All content, such text or html fragments, images or screenshots, is **indexed** and can be **queried** through 
a simple **REST** and **GraphQL** APIs, which makes it convenient for easy consumption by other services and applications. 
Content entries are subject to **retention** policies.

## Features:
The project is currently under active development, and aims to provide the following general capabilities in its first version:

* Container based architecture, but not limited to Kubernetes.
* Support for **multiple workspaces**, with full data isolation to enable multi-project and multi-tenancy capabilities.
* Simple **token based authentication** and authorization. 
* Several **simple to use REST API**.
  * Manage crawler plugins, supporting both synchronous and asynchronous executions.
  * Content queries and retrieval.
  * Administrative, to manage the crawling nodes and workspaces.
* A crawler **plugin library**, with common use cases.
* A lightweight Javascript **library** that to create the crawler plugins.
* Content **storage and indexing**, the parsed data retrieved in the browser gets stored in one or multiple persistent caches, ready for querying, with custom expiration policies.
* Link discovery and controlled deep site navigation.
* Scalability, multiple browser nodes can be deployed at once, and will coordinate among them to work over the crawling jobs queue.
* Simple installation and day-2 operations.

CrawliX is based on [Quarkus](https://quarkus.io/), [Infinispan](https://infinispan.org) and [Selenium](https://www.selenium.dev/documentation/).

-----

## Applications:

There are countless applications for data extraction from websites and web applications, such as:
* Content aggregation
* Competitive analysis
* Website health monitoring
* Application integrations
* Website monitoring
* AI models training


## Architecture ##
See [CrawliX Architecture Overview](docs/Architecture.md).

## Contributing to CrawliX ##
See the [developer's guide](docs/Developers.md).

## Deploying CrawliX ##
See [deploying CrawliX](deploy/Deploy.md).