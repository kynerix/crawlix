#!/usr/bin/env bash
pushd ../..
cd common/scripts
./build.sh
popd

pushd ..
echo "*** Starting CRAWLER dev mode in PORT 8078 and DEBUG PORT 5004 ***"
# Add -Dcrawler.firefox.args="--browser" to open a Firefox window instead of a headless browser
./mvnw -Dquarkus.http.port=8078 \
       -Ddebug=5004 \
       -Dcrawler.node.uri="http://localhost:8078"\
       -Dcrawler.node.key="localhost" \
       -Dcrawler.autostart=false \
       clean compile quarkus:dev
popd
