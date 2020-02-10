#!/usr/bin/env bash

here=$(dirname "${0}")
pushd "${here}/.."
TRANSMART_VERSION=$(gradle properties | grep '^version: ' - | awk '{print $2}')
popd

docker build --build-arg "TRANSMART_VERSION=${TRANSMART_VERSION}" -t "thehyve/transmart-api-server:${TRANSMART_VERSION}" "${here}/../docker/transmart-api-server" && \
docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD" && \
docker push "thehyve/transmart-api-server:${TRANSMART_VERSION}"
