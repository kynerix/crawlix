#!/usr/bin/env bash
set -o errexit
set -o pipefail

if [[ "${TRACE-0}" == "1" ]]; then
    set -o xtrace
fi

# Function to wait for container availability
wait_for_container() {
  host="$1"
  port="$2"

  until  nc -z $host $port; do
    >&2 echo "   - Container unavailable yet at $host : $port - retrying"
    sleep 2
  done
}

generate_admin_token() {
  ADMIN_TOKEN=`cat /proc/sys/kernel/random/uuid | sed 's/[-]//g' | head -c 20; echo;`
}

# First optional argument is the configuration script
if [ -z "$1" ]
  then
    # Default config
    CONFIG_FILE=config-minimal.sh
else
    CONFIG_FILE=$1
fi

echo "- Loading configuration from: $CONFIG_FILE"

. ${CONFIG_FILE}

if [ -z "$ADMIN_TOKEN" ]
then
    echo "- Generate admin token"
    generate_admin_token
else
    echo "- Admin token has been provided"
fi

echo "- Starting Infinispan container..."

podman rm -i -f infinispan > /dev/null 2>&1

podman run -d --name infinispan --network=host -e USER=$INFINISPAN_USER -e PASS=$INFINISPAN_PASS quay.io/infinispan/server:latest &

wait_for_container "127.0.0.1" $INFINISPAN_PORT

echo "- Infinispan is ready in port $INFINISPAN_PORT"
echo "- Starting Controller..."

podman rm -i -f crawlix-controller > /dev/null 2>&1

podman run -d --name crawlix-controller --network=host \
      -e QUARKUS_INFINISPAN_CLIENT_SERVER_LIST="127.0.0.1:$INFINISPAN_PORT" \
      -e QUARKUS_HTTP_PORT="$CONTROLLER_PORT" \
      -e QUARKUS_INFINISPAN_CLIENT_AUTH_USERNAME="$INFINISPAN_USER" \
      -e QUARKUS_INFINISPAN_CLIENT_AUTH_PASSWORD="$INFINISPAN_PASS" \
      -e CRAWLIX_INIT_WORKSPACES_CREATE_DEFAULT="true" \
      -e CRAWLIX_INIT_ADMIN_TOKEN="$ADMIN_TOKEN" \
      -e CRAWLIX_INIT_ADMIN_USER="$ADMIN_USER" \
      -e CRAWLIX_INIT_ADMIN_PASSWORD="$ADMIN_PASS" \
      kynerix/crawlix-controller

wait_for_container "127.0.0.1" $CONTROLLER_PORT

echo "- CONTROLLER is ready in $CONTROLLER_PORT"
echo "- Starting Service container..."

podman rm -i -f crawlix-service > /dev/null 2>&1

podman run -d --name crawlix-service --network=host \
      -e QUARKUS_INFINISPAN_CLIENT_SERVER_LIST="127.0.0.1:$INFINISPAN_PORT" \
      -e QUARKUS_HTTP_PORT="$SERVICE_PORT" \
      -e QUARKUS_INFINISPAN_CLIENT_AUTH_USERNAME="$INFINISPAN_USER" \
      -e QUARKUS_INFINISPAN_CLIENT_AUTH_PASSWORD="$INFINISPAN_PASS" \
      kynerix/crawlix-service

wait_for_container 127.0.0.1 $SERVICE_PORT

echo "- SERVICE is ready in $SERVICE_PORT"

for i in `seq 1 ${NUM_CRAWLERS}`;
    do
    PORT=$(($CRAWLER_PORT+1-i))

    echo "- Starting crawler $i on port $PORT"
    podman rm -i -f "crawlix-crawler-$i" > /dev/null 2>&1
    podman run -d --name "crawlix-crawler-${i}" --network=host \
        --shm-size 2gb \
        --memory 1gb \
        -e QUARKUS_INFINISPAN_CLIENT_SERVER_LIST="127.0.0.1:$INFINISPAN_PORT" \
        -e QUARKUS_HTTP_PORT="$SERVICE_PORT" \
        -e QUARKUS_INFINISPAN_CLIENT_AUTH_USERNAME="$INFINISPAN_USER" \
        -e QUARKUS_INFINISPAN_CLIENT_AUTH_PASSWORD="$INFINISPAN_PASS" \
        -e CRAWLER_NODE_URI="http://127.0.0.1:$PORT" \
        -e CRAWLER_JAVASCRIPT_LIB_URI="http://127.0.0.1:$PORT" \
        -e CRAWLER_NODE_KEY="crawler${i}" \
        -e CRAWLER_AUTOSTART="true" \
        -e QUARKUS_HTTP_PORT=$PORT \
        kynerix/crawlix-crawler

        wait_for_container 127.0.0.1  $PORT

      echo "- CRAWLER NODE $i ready in $PORT"

  done

echo ""
echo "-----------------------------------------------------------------------------------------------------------------------------------------------------"
podman ps
echo "-----------------------------------------------------------------------------------------------------------------------------------------------------"
echo "- Service    : http://127.0.0.1:$SERVICE_PORT"
echo "- Console    : http://127.0.0.1:$SERVICE_PORT/console/login.html"
echo "- Infinispan : http://127.0.0.1:$INFINISPAN_PORT"
echo ""
echo "- Infinispan user:  $INFINISPAN_USER"
echo "- Infinispan pass:  $INFINISPAN_PASS"
echo ""
echo "- Admin auth token is: $ADMIN_TOKEN"
echo "- Admin user: $ADMIN_USER"
echo "- Admin pass: $ADMIN_PASS"

echo "-----------------------------------------------------------------------------------------------------------------------------------------------------"

#echo "Status of crawlers:"
#echo ""
#curl -s -X GET "http://127.0.0.1:$SERVICE_PORT/admin/list-nodes" --header "Authorization: $ADMIN_TOKEN" | jq
#echo ""
