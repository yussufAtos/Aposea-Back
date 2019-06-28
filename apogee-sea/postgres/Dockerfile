FROM postgres:9.4

RUN localedef -i fr_FR -c -f UTF-8 -A /usr/share/locale/locale.alias fr_FR.UTF-8
ENV LANG fr_FR.UTF-8
ENV PG_LOG_DIR /var/lib/postgresql/pg_log

COPY init-user-db.sh customize-config.sh /docker-entrypoint-initdb.d/
COPY backupdb.sh /usr/bin/

# Create group and user apogee
RUN addgroup --gid 2000 apogee \
	&& adduser --system --uid 2000 --gecos 'User apogee' --ingroup apogee apogee \
	&& mkdir /var/lib/postgresql/backup /var/lib/postgresql/pg_log \
	&& chown -R apogee:apogee /var/lib/postgresql