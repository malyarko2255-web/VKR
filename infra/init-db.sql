-- Создание баз данных для сервисов advance-service
-- Скрипт выполняется при первом запуске PostgreSQL контейнера

\set ON_ERROR_STOP off

SELECT 'CREATE DATABASE advance_identity OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_identity')\gexec

SELECT 'CREATE DATABASE advance_core OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_core')\gexec

SELECT 'CREATE DATABASE advance_payment OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_payment')\gexec

SELECT 'CREATE DATABASE advance_scoring OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_scoring')\gexec

SELECT 'CREATE DATABASE advance_reference OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_reference')\gexec

SELECT 'CREATE DATABASE advance_notify OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_notify')\gexec

SELECT 'CREATE DATABASE advance_audit OWNER advance_user'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'advance_audit')\gexec

-- Выдача всех привилегий пользователю advance_user на каждую базу
\connect advance_identity
GRANT ALL PRIVILEGES ON DATABASE advance_identity TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;

\connect advance_core
GRANT ALL PRIVILEGES ON DATABASE advance_core TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;

\connect advance_payment
GRANT ALL PRIVILEGES ON DATABASE advance_payment TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;

\connect advance_scoring
GRANT ALL PRIVILEGES ON DATABASE advance_scoring TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;

\connect advance_reference
GRANT ALL PRIVILEGES ON DATABASE advance_reference TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;

\connect advance_notify
GRANT ALL PRIVILEGES ON DATABASE advance_notify TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;

\connect advance_audit
GRANT ALL PRIVILEGES ON DATABASE advance_audit TO advance_user;
GRANT ALL ON SCHEMA public TO advance_user;
