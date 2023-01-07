## Running CrawliX in development mode

### Pre-requisites

- Java 11+
- Podman, for building and executing images. 
  - Docker may be used as well, scripts not provided yet.
- Chromium, in the PATH 
  - default location is: */usr/bin/chromium-browser*
- Chromium driver in the path 
  - default location is: */usr/local/bin/chromedriver*
- jq tool in the PATH

### **Step 1**. Start the Infinispan container
```
podman rm infinispan

podman run --name infinispan --network=host -e USER=kynerix -e PASS=crawlix quay.io/infinispan/server:latest
```
The Infinispan console will become available at http://localhost:11222/console/welcome

### **Step 2**. Start the Controller Service in Quarkus dev mode
```
cd controller/scripts

./dev-mode.sh 
```
The crawliX controller will start in **port 8078** and **debug port 5004**

### **Step 3**. Start the REST service and console in Quarkus dev mode
```
cd service/scripts

./dev-mode.sh 
```
It will start in **port 8079** and **debug port 5005**

### **Step 4**. Start one crawler node in Quarkus dev mode
```
cd crawler-node/scripts

./dev-mode.sh 
```

The crawliX node will register itself and will start in **port 8077** and **debug port 5003**

### **Step 5**: Check everything is in place:

- Access the Infinispan console with user *kynerix* and password *crawlix* at http://localhost:11222/console/welcome
- Access to the CrawliX console at http://localhost:8079/console/login.html with user *admin* / *crawlix*
- Check the /admin service:
```
curl -s -X  GET "http://localhost:8079/admin/list-workspaces" --header "Authorization: 00-DEFAULT-ADMIN-TOKEN-00" | jq
```
- Check the /crawlix service:
```
curl -s -X  GET "http://localhost:8079/crawlix/default/crawlers" --header "Authorization: 00-DEFAULT-TOKEN-00" | jq
```

- Check the crawler node's health:

```
curl -s -X GET -H "Content-Type: application/json" http://localhost:8077/crawler-node/status | jq --tab
```

- Check the Swagger UI : http://localhost:8079/q/swagger-ui/

### **Step 6**: Start developing with Quarkus

