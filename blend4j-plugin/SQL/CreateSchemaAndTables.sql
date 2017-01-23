-- Schema: galaxy

-- DROP SCHEMA galaxy;

CREATE SCHEMA galaxy
  AUTHORIZATION galaxy;

GRANT ALL ON SCHEMA galaxy TO galaxy;
GRANT ALL ON SCHEMA galaxy TO biomart_user;

-- Table: galaxy.status_of_export_job

-- DROP TABLE galaxy.status_of_export_job;

CREATE TABLE galaxy.status_of_export_job
(
  job_status character varying(200) NOT NULL,
  last_export_name character varying(200) NOT NULL,
  last_export_time timestamp(6) without time zone NOT NULL,
  job_name_id character varying(200) NOT NULL,
  id bigint NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE galaxy.status_of_export_job
  OWNER TO galaxy;
GRANT ALL ON TABLE galaxy.status_of_export_job TO galaxy;
GRANT ALL ON TABLE galaxy.status_of_export_job TO biomart_user;


-- Table: galaxy.USERS_DETAILS_FOR_EXPORT_GAL

-- DROP TABLE galaxy.users_details_for_export_gal;

CREATE TABLE galaxy.users_details_for_export_gal
(
  id bigint NOT NULL,
  galaxy_key character varying(100),
  mail_address character varying(200),
  username character varying(200)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE galaxy.users_details_for_export_gal
  OWNER TO galaxy;
GRANT ALL ON TABLE galaxy.users_details_for_export_gal TO galaxy;
GRANT ALL ON TABLE galaxy.users_details_for_export_gal TO biomart_user;


-- Sequence: galaxy.hibernate_id

-- DROP SEQUENCE galaxy.hibernate_id;

CREATE SEQUENCE galaxy.hibernate_id
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE galaxy.hibernate_id
  OWNER TO galaxy;
  
  -- Sequence: galaxy.status_of_export_job_seq

-- DROP SEQUENCE galaxy.status_of_export_job_seq;

CREATE SEQUENCE galaxy.status_of_export_job_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE galaxy.status_of_export_job_seq
  OWNER TO galaxy;