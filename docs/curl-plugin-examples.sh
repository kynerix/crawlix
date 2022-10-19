#!/usr/bin/env bash

AUTH_TOKEN="00-DEFAULT-TOKEN-00"
CURL_ARGS="-w \n -s -X "

set -o xtrace

# Install multiple plugins
curl -s -X POST http://localhost:8079/crawlix/install-plugins \
      --header "Content-Type: application/json" \
      --header "Authorization: $AUTH_TOKEN" \
      --data-binary @- <<EOF
[
   {   "key": "test-crawler-1", "defaultURL":  "http://localhost:8079/tests/test-1.html" },
   {   "key": "test-crawler-2", "defaultURL":  "http://localhost:8079/tests/test-2.html" }
]
EOF

# List installed plugins
curl -s -X GET "http://localhost:8079/crawlix/list-plugins" --header "Authorization: $AUTH_TOKEN" | jq

# Change plugin script
curl -s -X POST http://localhost:8079/crawlix/install-script?key=test-crawler-1 \
      --header 'Content-Type: text/plain' \
      --header "Authorization: $AUTH_TOKEN" \
      --data-binary @- <<EOF
// Simple test #1 plugin
crawlix
  .begin()

	  // Content 1
	  .key().parse("[id='my-id-1']", "id").assertEquals("my-id-1")
	  .body().parse("[id='my-id-1']")      .assertContains("find me by id 1")
    .addContent()

    // Content 2
	  .field("key") .parse("[id='my-id-2']", "id").assertEquals("my-id-2")
	  .field("body").parse("[id='my-id-2']")      .assertContains("find me by id 2")
    .addContent()

    .assertContentCount(2)

    // Links
    .findLinks().filterLinks(null, "id")
    .assert( crawlix.linkCount() == 3)

  .end()
EOF

# Update plugin script
curl -s -X POST http://localhost:8079/crawlix/install-script?key=test-crawler-2 \
      --header 'Content-Type: text/plain' \
      --header "Authorization: $AUTH_TOKEN" \
      --data-binary @- <<EOF
// Simple test #2 plugin
crawlix
  .begin()
	.title().parseMultiple("h2")
	.summary().parseMultiple("p.small")
	.body().parseMultiple("p.body")
	.addContents()
  .assertMinContentCount(3)
  .end()
EOF

# Run crawler 1 and store results
curl -s -X GET "http://localhost:8079/crawlix/execute?plugin=test-crawler-1&store-results=true" \
        --header 'Content-Type: application/json' \
        --header "Authorization: $AUTH_TOKEN" | jq --tab

# Run crawler 2 and store results
curl -s -X GET "http://localhost:8079/crawlix/execute?plugin=test-crawler-2&store-results=true" \
        --header 'Content-Type: application/json' \
        --header "Authorization: $AUTH_TOKEN" | jq --tab

# List all default content for this workspace without filters
curl -s -GET http://localhost:8079/crawlix-content/search?max-results=3 --header "Content-Type: application/json" --header "Authorization: $AUTH_TOKEN" | jq

# List filter with query : See https://infinispan.org/docs/stable/titles/query/query.html#ickle-query-language for syntax
QUERY="(key='my-id-1' OR key='my-id-2')"
curl -s -G GET http://localhost:8079/crawlix-content/search --data-urlencode "filter=$QUERY" --header "Content-Type: application/json" --header "Authorization: $AUTH_TOKEN" | jq

