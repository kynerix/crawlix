# CrawliX - Crawling and Content Extraction platform

CrawliX is an **scalable crawling platform** that allows automated parsing of websites by headless browsers, 
to **extract and store custom-defined content fragments and metrics**, for later analysis and querying. 

All content, such text or html fragments, images or screenshots, is **indexed** and can be **queried** through 
a simple **REST** API, which makes it convenient for easy consumption by other services and applications. 
Content entries can be subject to **retention** policies.

## Features:
The project is currently in **prototype** phase and is under active development, and aims to provide the following general capabilities in its first version:

* A container based architecture - but not limited to Kubernetes deployments.
* Support for **multiple workspaces**, with full data isolation to enable multi-project and multi-tenancy capabilities.
* Token based authentication and authorization. 
* Simple to use **REST API**.
  * Manage crawler plugins, supporting both synchronous and asynchronous executions.
  * Content queries and retrieval.
  * Manage the crawling nodes and workspaces.
* Management and monitoring console UI.
* A simple crawler [plugin library](https://github.com/kynerix/crawlix-lib), with examples and common use cases.
* A lightweight Javascript library that to create the crawler plugins.
* Content storage and indexing.
* Link discovery and controlled deep site navigation.
* Simple scalability model - multiple browser nodes containers can be deployed, and will coordinate among them to work over the crawling jobs queue.

CrawliX is based on [Quarkus](https://quarkus.io/), [Infinispan](https://infinispan.org) and [Selenium](https://www.selenium.dev/documentation/).

## Applications:

There are countless applications for data extraction from websites and web applications, such as:
* Content aggregation
* Competitive analysis
* Website health monitoring
* Application integrations
* Gather data for AI models training


## Architecture ##
See [CrawliX Architecture Overview](docs/Architecture.md).

## Contributing to CrawliX ##
See the [developer's guide](docs/Developers.md).

## Deploying CrawliX ##
See [deploying CrawliX](deploy/Deploy.md).