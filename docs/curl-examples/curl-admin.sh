#!/usr/bin/env bash

AUTH_TOKEN="00-DEFAULT-ADMIN-TOKEN-00"
CURL_ARGS="-w \n -s -X "

set -o xtrace

# List crawling nodes
curl -s -X GET "http://localhost:8079/admin/list-nodes" --header "Authorization: $AUTH_TOKEN" | jq

# Stop crawling node
#curl -s -X GET "http://localhost:8079/admin/stop-node?node=localhost" --header "Authorization: $AUTH_TOKEN" | jq

# Start crawling node
#curl -s -X GET "http://localhost:8079/admin/start-node?node=localhost" --header "Authorization: $AUTH_TOKEN" | jq

# List existing workspaces
curl -s -X GET "http://localhost:8079/admin/list-workspaces" --header "Authorization: $AUTH_TOKEN" | jq

# Create workspace
curl -s -X GET "http://localhost:8079/admin/create-workspace?key=my-workspace&name=My%20Workspace" --header "Authorization: $AUTH_TOKEN" | jq

# Generate a token
curl -s -X GET "http://localhost:8079/admin/generate-token?workspace=my-workspace" --header "Authorization: $AUTH_TOKEN" | jq

# Generate token in workspace
curl -s -X POST "http://localhost:8079/admin/generate-token?workspace=my-workspace" --header "Authorization: $AUTH_TOKEN" | jq

# Delete a workspace
curl -s -X DELETE "http://localhost:8079/admin/delete-workspace?key=my-workspace" --header "Authorization: $AUTH_TOKEN" | jq

# Delete token
#curl -s -X DELETE "http://localhost:8079/admin/delete-token?token=3e8818c8-4b1d-434f-89f9-e1cb879a3ec5" --header "Authorization: $AUTH_TOKEN" | jq
