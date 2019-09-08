#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd ${DIR}

docker rm -f app_db

echo "Starting docker container with Postgres"

docker run \
    -it \
    -d \
    -v ${DIR}/db:/docker-entrypoint-initdb.d \
    --env-file ${DIR}/db/postgres_env.list \
    -p 5432:5432 \
    --name app_db \
    postgres:alpine