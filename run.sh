#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd ${DIR}

sbt assembly

bash start_db.sh

java -jar target/scala-2.12/forum-assembly-0.0.1-SNAPSHOT.jar

bash stop_db.sh