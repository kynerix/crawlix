#!/usr/bin/env bash
set -o errexit
set -o pipefail

pushd ../common/scripts
./build.sh
popd

pushd ../controller/scripts
./build.sh
popd

pushd ../crawler-node/scripts
./build.sh
popd

pushd ../service/scripts
./build.sh
popd

echo "########################################################################################################################"
echo All modules built successfully
echo "########################################################################################################################"

pushd ../controller/scripts
./build-image.sh
popd

pushd ../crawler-node/scripts
./build-image.sh
popd

pushd ../service/scripts
./build-image.sh
popd

echo "########################################################################################################################"
echo All images built successfully
echo "########################################################################################################################"