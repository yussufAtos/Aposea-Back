-- case categories
INSERT INTO case_category (name, description, display_priority, display_limit, trigger_ui_snapshot)
SELECT 'PF', 'Point Figé', 1, 30, true
WHERE NOT EXISTS ( SELECT name FROM case_category WHERE name = 'PF' );

INSERT INTO case_category (name, description, display_priority, display_limit, trigger_ui_snapshot)
SELECT 'SRMixte', 'Situation Réseau Mixte', 2, 0, false
WHERE NOT EXISTS ( SELECT name FROM case_category WHERE name = 'SRMixte' );

INSERT INTO case_category (name, description, display_priority, display_limit, trigger_ui_snapshot)
SELECT 'SRJ', 'Situation Réseau J', 3, 0, false
WHERE NOT EXISTS ( SELECT name FROM case_category WHERE name = 'SRJ' );

-- case types
INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'pf', 'PF', true, true, 'pf', 0, 0, '''/2_Situation_Reseau/PointsFiges/recollement/enrichi/recollement-auto-''yyyymmdd-HHMM''-enrichi'''
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'pf' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'srmixte', 'SRMixte', true, false, 'srmixte', 0, 0, '''/2_Situation_Reseau/SRMixte/SRMixte-''yyyymmdd_HHMM'
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'srmixte' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'srj-ij', 'SRJ', true, false, 'srj-ij', 0, 0, '''/2_Situation_Reseau/SRJ/ecct-situ-''yyyymmdd-HHMM'
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'srj-ij' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'srj-jm1', 'SRJ', true, false, 'srj-jm1', 0, 0, '''/2_Situation_Reseau/SRJ/ecct-situ-''yyyymmdd-HHMM'
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'srj-jm1' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment)
SELECT 'pf-hybride', 'PF', false, false, 'pf-hybride', 0, 0
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'pf-hybride' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'srmixte-hybride', 'SRMixte', false, false, 'srmixte-hybride', 0, 0, '''/2_Situation_Reseau/CGM_Hybrides/SRMixte/SRMixte_''yyyymmdd-HHMM''_IDCF_hybride'''
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'srmixte-hybride' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'srj-ij-hybride', 'SRJ', false, false, 'srj-ij-hybride', 0, 0, '''/2_Situation_Reseau/CGM_Hybrides/SRJ\SRJ_''yyyymmdd-HHMM''_IDCF_hybride'''
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'srj-ij-hybride' );

INSERT INTO case_type (name, case_category_id, enabled, opfab_enabled, card_tag, card_start_date_increment, card_end_date_increment, path_in_convergence)
SELECT 'srj-jm1-hybride', 'SRJ', false, false, 'srj-jm1-hybride', 0, 0, '''/2_Situation_Reseau/CGM_Hybrides/SRJ\SRJ_''yyyymmdd-HHMM''_D2CF_hybride'''
WHERE NOT EXISTS ( SELECT name FROM case_type WHERE name = 'srj-jm1-hybride' );

-- time ranges
INSERT INTO timerange_type (name, time_zone, card_tag, opfab_enabled, card_start_date_increment, card_end_date_increment)
SELECT 'Tout', 'Europe/Paris', 'Tout', true, 0, 0
WHERE NOT EXISTS ( SELECT name FROM timerange_type WHERE name = 'Tout' );

INSERT INTO timerange_type (name, time_zone, start_type, end_type, end_time_hour, start_time_minutes, card_tag, opfab_enabled, card_start_date_increment, card_end_date_increment)
SELECT 'TR', 'Europe/Paris', 'NOW', 'HOURRELATIVE', 3, 30, 'TR', true, 0, 0
WHERE NOT EXISTS ( SELECT name FROM timerange_type WHERE name = 'TR' );

INSERT INTO timerange_type (name, time_zone, start_type, end_type, end_time_hour, start_time_minutes, card_tag, opfab_enabled, card_start_date_increment, card_end_date_increment)
SELECT 'H+10', 'Europe/Paris', 'NOW', 'HOURRELATIVE', 10, 15, 'H+10', true, 0, 0
WHERE NOT EXISTS ( SELECT name FROM timerange_type WHERE name = 'H+10' );

INSERT INTO timerange_type (name, time_zone, start_type, end_type, end_time_day, alternate_if_less_hours_than, alternate_timerange, start_time_minutes, card_tag, opfab_enabled, card_start_date_increment, card_end_date_increment)
SELECT 'IJ', 'Europe/Paris', 'NOW', 'MIDNIGHT', 1, 10, 'H+10', 15, 'IJ', true, 0, 0
WHERE NOT EXISTS ( SELECT name FROM timerange_type WHERE name = 'IJ' );

INSERT INTO timerange_type (name, time_zone, start_type, end_type, start_time_day, end_time_day, card_tag, opfab_enabled, card_start_date_increment, card_end_date_increment)
SELECT 'J+1', 'Europe/Paris', 'MIDNIGHT', 'MIDNIGHT', 1, 2, 'J+1', true, 0, 0
WHERE NOT EXISTS ( SELECT name FROM timerange_type WHERE name = 'J+1' );

-- authorities
INSERT INTO authority (id, name)
SELECT nextval('AUTHORITIES_SEQ'), 'READ'
       WHERE NOT EXISTS ( SELECT name FROM authority WHERE name = 'READ' );

INSERT INTO authority (id, name)
SELECT nextval('AUTHORITIES_SEQ'), 'WRITE'
       WHERE NOT EXISTS ( SELECT name FROM authority WHERE name = 'WRITE' );

       INSERT INTO authority (id, name)
SELECT nextval('AUTHORITIES_SEQ'), 'ADMIN'
       WHERE NOT EXISTS ( SELECT name FROM authority WHERE name = 'ADMIN' );

-- authorities for opfab
INSERT INTO authority (id, name)
SELECT nextval('AUTHORITIES_SEQ'), 'ROLE_USER'
       WHERE NOT EXISTS ( SELECT name FROM authority WHERE name = 'ROLE_USER' );
INSERT INTO authority (id, name)
SELECT nextval('AUTHORITIES_SEQ'), 'ROLE_ADMIN'
       WHERE NOT EXISTS ( SELECT name FROM authority WHERE name = 'ROLE_ADMIN' );

-- admin user type and user
INSERT INTO usertypes (name, opfab_enabled)
SELECT 'admin', false
WHERE NOT EXISTS ( SELECT name FROM usertypes WHERE name = 'admin' );

INSERT INTO users (id, username, password, enabled, created_time, default_usertype_id, actual_usertype_id)
SELECT nextval('USERS_SEQ'), 'admin', '$2a$10$D4OLKI6yy68crm.3imC9X.P2xqKHs5TloWUcr6z5XdOqnTrAK84ri', true, now(), 'admin', 'admin'
       WHERE NOT EXISTS ( SELECT username FROM users WHERE username = 'admin' );

INSERT INTO usertypes_users (usertype_name, user_id)
SELECT (SELECT name from usertypes WHERE name='admin'), (SELECT id from users WHERE username='admin')
       WHERE NOT EXISTS ( SELECT usertype_name, user_id FROM usertypes_users WHERE user_id = (SELECT id from users WHERE username='admin')
       AND usertype_name = (SELECT name from usertypes WHERE name='admin'));

-- admin user authorities
INSERT INTO users_authorities (user_id, authority_id)
SELECT (SELECT id from users WHERE username='admin'), (SELECT id from authority WHERE name='READ')
       WHERE NOT EXISTS ( SELECT user_id, authority_id FROM users_authorities WHERE user_id = (SELECT id from users WHERE username='admin')
       AND authority_id = (SELECT id from authority WHERE name='READ'));

INSERT INTO users_authorities (user_id, authority_id)
SELECT (SELECT id from users WHERE username='admin'), (SELECT id from authority WHERE name='WRITE')
       WHERE NOT EXISTS ( SELECT user_id, authority_id FROM users_authorities WHERE user_id = (SELECT id from users WHERE username='admin')
       AND authority_id = (SELECT id from authority WHERE name='WRITE'));

INSERT INTO users_authorities (user_id, authority_id)
SELECT (SELECT id from users WHERE username='admin'), (SELECT id from authority WHERE name='ADMIN')
       WHERE NOT EXISTS ( SELECT user_id, authority_id FROM users_authorities WHERE user_id = (SELECT id from users WHERE username='admin')
       AND authority_id = (SELECT id from authority WHERE name='ADMIN'));

-- admin user authorities for opfab
INSERT INTO users_authorities (user_id, authority_id)
SELECT (SELECT id from users WHERE username='admin'), (SELECT id from authority WHERE name='ROLE_USER')
       WHERE NOT EXISTS ( SELECT user_id, authority_id FROM users_authorities WHERE user_id = (SELECT id from users WHERE username='admin')
       AND authority_id = (SELECT id from authority WHERE name='ROLE_USER'));

INSERT INTO users_authorities (user_id, authority_id)
SELECT (SELECT id from users WHERE username='admin'), (SELECT id from authority WHERE name='ROLE_ADMIN')
       WHERE NOT EXISTS ( SELECT user_id, authority_id FROM users_authorities WHERE user_id = (SELECT id from users WHERE username='admin')
       AND authority_id = (SELECT id from authority WHERE name='ROLE_ADMIN'));

