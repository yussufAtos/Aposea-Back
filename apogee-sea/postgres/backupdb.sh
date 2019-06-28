#!/bin/sh

# Mode rouleau sur les sauvegardes
PG_DUMP_OUT_DIR=/var/lib/postgresql/backup
RETENTION=18
find $PG_DUMP_OUT_DIR -mtime +$RETENTION -exec rm {} \;

# Sauvegarde proprement dite
PG_HOST=localhost
PG_PORT=5432
PG_USER=$SPRING_DATASOURCE_USERNAME
PG_DB=apogeesea
export PGPASSWORD=$SPRING_DATASOURCE_PASSWORD

NOW=$(date +"%Y%m%d-%H%M%S")
echo ""
echo "-> Dumping PostgreSQL "$PG_DB" database"
OUT_FILE=$PG_DUMP_OUT_DIR"/dump-postgreSQL-"$PG_DB"-"$NOW".dump"
pg_dump -h $PG_HOST -p $PG_PORT -U $PG_USER -Fc $PG_DB > $OUT_FILE

echo "   PostgreSQL DB saved in "$OUT_FILE
