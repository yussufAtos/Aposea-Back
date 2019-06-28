#!/bin/bash
set -e

echo "CREATE USER $SPRING_DATASOURCE_USERNAME PASSWORD '$SPRING_DATASOURCE_PASSWORD';" >/tmp/sql.$$
echo "CREATE DATABASE apogeesea OWNER=$SPRING_DATASOURCE_USERNAME;" >>/tmp/sql.$$
echo "CREATE DATABASE apogeesea_test OWNER=$SPRING_DATASOURCE_USERNAME;" >>/tmp/sql.$$

psql -v ON_ERROR_STOP=1 --username "postgres" </tmp/sql.$$
rm /tmp/sql.$$
