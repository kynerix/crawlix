#!/usr/bin/env bash
pushd ../..
cd common/scripts
./build.sh
popd

pushd ..
echo "*** Starting CONTROLLER dev mode in PORT 8078 and DEBUG PORT 5004 ***"
./mvnw -Dquarkus.http.port=8078 \
       -Ddebug=5004 \
       -Dcrawlix.init.workspaces.create.default=true \
       -Dcrawlix.init.admin.token="00-DEFAULT-ADMIN-TOKEN-00" \
       -Dcrawlix.init.admin.user="admin" \
       -Dcrawlix.init.admin.password="crawlix" \
       -Dcrawlix.init.workspaces.default.token="00-DEFAULT-TOKEN-00" \
       clean compile quarkus:dev
popd
