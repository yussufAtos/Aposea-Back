#! /bin/bash

PG_ACCESS_FILE=$PGDATA/pg_hba.conf
PG_CONF_FILE=$PGDATA/postgresql.conf

# Adjust PostgreSQL access configuration so that remote connections to the
# database from containers are possible
echo "Customizing $PG_ACCESS_FILE for Apogee-SEA"
cat >>$PG_ACCESS_FILE <<PG_ACCESS_FILE_CONF
host  all all 172.18.0.1/16   md5
PG_ACCESS_FILE_CONF

# Adjust PostgreSQL configuration:
#  . logging
#  . autovacuum
echo "Customizing $PGDATA/postgresql.conf for Apogee-SEA"
cat >>$PG_CONF_FILE <<PG_CONFIGURATION_END
# Activate logging in "pg_log" folder
log_destination = stderr
logging_collector = on  			# Enable capturing of stderr and csvlog
#log_directory = '/var/lib/postgresql/pg_log'	# directory where log files are written
log_filename = 'postgresql-%a.log'		# log file name pattern (one file per day)
log_file_mode = 0644				# creation mode for log files
log_truncate_on_rotation = on
log_rotation_age = 1d
log_rotation_size = 0
log_line_prefix = '< %m >'
log_timezone = 'Europe/Paris'
#log_connections = on

# Autovacuum configuration
autovacuum_work_mem = 128MB
autovacuum_vacuum_cost_delay = 10ms
autovacuum_vacuum_cost_limit = 1000

# Date format
datestyle = 'iso, dmy'
timezone = 'Europe/Paris'

# Locale
lc_messages = 'fr_FR.UTF-8'                   # locale for system error message
lc_monetary = 'fr_FR.UTF-8'                   # locale for monetary formatting
lc_numeric = 'fr_FR.UTF-8'                    # locale for number formatting
lc_time = 'fr_FR.UTF-8'                               # locale for time formatting
default_text_search_config = 'pg_catalog.french'

PG_CONFIGURATION_END
echo "log_directory='$PG_LOG_DIR'" >>$PG_CONF_FILE
