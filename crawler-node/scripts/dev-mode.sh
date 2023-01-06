#!/usr/bin/env bash
pushd ../..
cd common/scripts
./build.sh
popd

pushd ..
echo "*** Starting CRAWLER dev mode in PORT 8077 and DEBUG PORT 5003 ***"
./mvnw -Dquarkus.http.port=8077 \
       -Ddebug=5003 \
       -Dcrawler.node.uri="http://localhost:8077"\
       -Dcrawler.javascript.lib.url="http://localhost:8077"\
       -Dcrawler.node.key="localhost" \
       -Dcrawler.autostart=false \
       -Dcrawler.browser.headless=true \
       clean compile quarkus:dev
popd
