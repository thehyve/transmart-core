--
-- Name: cz_job_master; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_job_master (
    job_id bigint NOT NULL,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    active character varying(1),
    time_elapsed_secs double precision DEFAULT 0,
    build_id bigint,
    session_id bigint,
    database_name character varying(50),
    job_status character varying(50),
    job_name character varying(500)
);

--
-- Name: cz_job_master_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_job_master
    ADD CONSTRAINT cz_job_master_pk PRIMARY KEY (job_id);

--
-- Name: tf_trg_cz_job_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_job_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
      if NEW.JOB_ID is null then
        select nextval('tm_cz.SEQ_CZ_JOB_MASTER') into NEW.JOB_ID ;       
      end if;       
  RETURN NEW;
end;
$$;

--
-- Name: trg_cz_job_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_job_id BEFORE INSERT ON cz_job_master FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_job_id();

--
-- Name: seq_cz_job_master; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_cz_job_master
    START WITH 18288
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 2;

