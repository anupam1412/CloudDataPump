#!/usr/bin/env bash

echo "Running entrypoint.sh"
#Point to Nexus via gcptools DNS for dynamic pip install of DAGs

airflow initdb
airflow scheduler &
airflow webserver