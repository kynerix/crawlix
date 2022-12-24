#!/usr/bin/env bash

# Number of crawler nodes
NUM_CRAWLERS=2

# Please, change this to your needs
INFINISPAN_USER=kynerix
INFINISPAN_PASS=crawlix
INFINISPAN_PORT=11222

CONTROLLER_PORT=8078
SERVICE_PORT=8079
CRAWLER_PORT=8077

ADMIN_USER=kynerix
ADMIN_PASS=crawlix

# Leave commented to generate a random token (recommended)
ADMIN_TOKEN="00-DEFAULT-ADMIN-TOKEN-00"

echo "**********************************************************************************"
echo "DEFAULT VALUES - DO NOT USE IN PRODUCTION"
echo "**********************************************************************************"
