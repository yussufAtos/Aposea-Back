RDBMS docker container for Apogee-SEA (PostgreSQL)

Two customization scripts are used

# customize-config.sh
*This script is called only at database creation time*

It handles:
  * pg_hba.conf customization: one line is added to allow another docker container to call PostgreSQL in PostgreSQL container.
  * postgresql.conf customization: logging is activated
For more details, see script content.

# init-user-db.sh
*This script is called only at database creation time*

It handles: 
  * user creation (name 'apogee')
  * standard database creation (apogeesea)
  * test database creation (apogeesea_test)

