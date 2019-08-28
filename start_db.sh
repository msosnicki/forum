#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

docker rm -f app_db

docker build . -f db.Dockerfile -t forumdb

docker run \
    -it \
    -d \
    -v ${DIR}/db:/docker-entrypoint-initdb.d \
    --env-file ${DIR}/db/postgres_env.list \
    -p 5432:5432 \
    --name app_db \
    forumdb