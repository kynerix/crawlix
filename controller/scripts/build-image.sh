#!/usr/bin/env bash
pushd ..
podman build -f src/main/docker/Dockerfile.jvm -t kynerix/crawlix-controller .
popd
