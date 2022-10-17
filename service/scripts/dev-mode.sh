#!/usr/bin/env bash
pushd ../..
cd common/scripts
./build.sh
popd
pushd ..
echo "*** Starting dev mode in PORT 8079 ***"
./mvnw \
  -Dquarkus.http.port=8079 \
  -Dcrawlix.workspaces.create.default=true \
  clean compile quarkus:dev
popd
