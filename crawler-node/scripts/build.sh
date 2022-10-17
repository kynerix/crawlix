#!/usr/bin/env bash
set -
pushd ../..
cd common/scripts
./build.sh
popd
pushd ..
./mvnw clean package -DskipTests
popd
