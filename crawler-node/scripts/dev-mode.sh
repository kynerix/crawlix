#!/usr/bin/env bash
pushd ../..
cd common/scripts
./build.sh
popd

pushd ..
echo "*** Starting CRAWLER dev mode in PORT 8077 and DEBUG PORT 5003 ***"
# Add -Dcrawler.firefox.args="--browser" to open a Firefox window instead of a headless browser
./mvnw -Dquarkus.http.port=8077 \
       -Ddebug=5003 \
       -Dcrawler.node.uri="http://localhost:8077"\
       -Dcrawler.javascript.lib.url="http://localhost:8077"\
       -Dcrawler.node.key="localhost" \
       -Dcrawler.autostart=false \
       clean compile quarkus:dev
popd
