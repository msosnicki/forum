#!/bin/bash

echo "Stopping & cleaning after docker container with Postgres"
docker stop app_db
docker rm -f app_db