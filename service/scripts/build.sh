#!/usr/bin/env bash
set -e
pushd ..
#quarkus build
./mvnw package
popd
