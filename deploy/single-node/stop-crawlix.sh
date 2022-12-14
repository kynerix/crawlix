#!/usr/bin/env bash
set -o errexit
set -o pipefail

if [[ "${TRACE-0}" == "1" ]]; then
    set -o xtrace
fi

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

echo "- Stopping crawlix services"
podman rm -i -f crawlix-services

for i in `seq 1 ${NUM_CRAWLERS}`;
do
    PORT=$(($CRAWLER_PORT+1-i))
    echo "- Stopping crawler $i on port $PORT"
    podman rm -i -f "crawlix-crawler-$i"
done

echo "- Stopping controller"
podman rm -i -f crawlix-controller

echo "- Stopping Infinispan"
podman rm -i -f infinispan


echo "- All CrawliX containers STOPPED"

echo ""
echo "-----------------------------------------------------------------------------------------------------------------------------------------------------"
podman ps
echo "-----------------------------------------------------------------------------------------------------------------------------------------------------"
