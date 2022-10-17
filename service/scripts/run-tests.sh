#!/usr/bin/env bash
AUTH_TOKEN="00-DEFAULT-TOKEN-00"
CURL_ARGS="-w \n -s -X "

curl $CURL_ARGS POST http://localhost:8079/crawlix/install-plugins \
      --header "Content-Type: application/json" \
      --header "Authorization: $AUTH_TOKEN" \
      --data-binary @- <<EOF
[
   {   "key": "test-crawler-1", "defaultURL":  "http://localhost:8079/tests/test-1.html" },
   {   "key": "test-crawler-2", "defaultURL":  "http://localhost:8079/tests/test-2.html" }
]
EOF

echo ""
echo "Installed plugins:"
curl $CURL_ARGS GET "http://localhost:8079/crawlix/list-plugins" --header "Authorization: $AUTH_TOKEN" | jq

echo "------------------------------------------"

curl $CURL_ARGS POST http://localhost:8079/crawlix/install-script?key=test-crawler-1 \
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

curl $CURL_ARGS POST http://localhost:8079/crawlix/install-script?key=test-crawler-2 \
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

# ----------------------------------------------------------------------------------------------------------------------

echo ""
echo ""
echo "** EXECUTING"
curl $CURL_ARGS GET "http://localhost:8079/crawlix/execute?plugin=test-crawler-1&store-results=true" \
        --header 'Content-Type: application/json' \
        --header "Authorization: $AUTH_TOKEN" | jq --tab
echo ""
echo ""
curl $CURL_ARGS GET "http://localhost:8079/crawlix/execute?plugin=test-crawler-2&store-results=true" \
        --header 'Content-Type: application/json' \
        --header "Authorization: $AUTH_TOKEN" | jq --tab
echo ""
echo ""
