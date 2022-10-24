# CrawliX deployment instructions

CrawliX is designed to run in containers. There are multiple options to deploy and run the different components, for example:
- Running containers, with Podman or Docker.
- Using Docker compose.
- Deploying into any Kubernetes cluster.

## Single node architecture with Podman

This is the simplest style of deployment, with just one node (physical or VM) with multiple containers running in different ports, 
in the host network.

***Pre-requisites***: 
- Linux Fedora, CentOS or RHEL. Other distributions will require different install instructions to meet the pre-requisites.
- 1 physical box or VM, with the following RAM requirements: 
  - 1 GB + 1 GB per crawling node. 
  - Min 4 GB, 8+ GB recommended.

### 1. Install dependencies
```
sudo dnf -y install java-devel jq git nc podman
```
### 2. Clone repository

```
git clone https://github.com/kynerix/crawlix.git
```
### 3. Build all images
```
cd crawlix/deploy/

./build-all.sh
```

### 4. Configure deployment
Configuration is simply a set of variables, being the most important the number of crawler nodes to spin up.
Defaults minimal settings are provided in [config-minimal.sh](single-node/config-minimal.sh).

If you need a different configuration, just create your own copy, and pass it as a parameter to the start and stop scripts.

### 5. Start all containers
```
cd single-node

./start-crawlix.sh [ optional, your own configuration ]
```

### 6. Stop all containers
```
./stop-crawlix.sh [ optional, your own configuration ]
```

### Not provided

#### Security and external HTTPS routing
This deployment instructions do not provide any routing mechanism to the exposed services via HTTPS. The service can only 
be accessed from localhost. That can be easily achieved with HAProxy, Apache or similar.

#### Certificates generation or installation
No mechanism is provided for this. Feel free to bring your own.