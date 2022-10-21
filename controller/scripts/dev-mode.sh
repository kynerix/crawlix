#!/usr/bin/env bash
pushd ../..
cd common/scripts
./build.sh
popd

pushd ..
echo "*** Starting CONTROLLER dev mode in PORT 8077 and DEBUG PORT 5003 ***"
./mvnw -Dquarkus.http.port=8077 \
       -Ddebug=5003 \
       -Dcrawlix.init.workspaces.create.default=true \
       -Dcrawlix.init.admin.token="00-DEFAULT-ADMIN-TOKEN-00" \
       -Dcrawlix.init.workspaces.default.token="00-DEFAULT-TOKEN-00" \
       clean compile quarkus:dev
popd
