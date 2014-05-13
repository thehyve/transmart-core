--
-- Name: cz_dw_version; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_dw_version (
    dw_version_id bigint NOT NULL,
    version_name character varying(200),
    release_date timestamp without time zone,
    create_date timestamp without time zone,
    created_by bigint,
    is_current character(1)
);

--
-- Type: SEQUENCE; Owner: TM_CZ; Name: SEQ_CZ_DW_VERSION_ID
--
CREATE SEQUENCE seq_cz_dw_version_id
  NO MINVALUE
  NO MAXVALUE
  INCREMENT BY 1
  START WITH 41
  CACHE 2
;

--
-- Name: tf_trg_cz_dw_version_id; Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_dw_version_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.DW_VERSION_ID is null then
 select nextval('tm_cz.SEQ_CZ_DW_VERSION_ID') into NEW.DW_VERSION_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_dw_version_id(); Type: TRIGGER; Schema: tm_cz; Owner: -
--
  CREATE TRIGGER trg_cz_dw_version_id BEFORE INSERT ON cz_dw_version FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_dw_version_id();


--
-- Name: cz_dw_version_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_dw_version
    ADD CONSTRAINT cz_dw_version_pk PRIMARY KEY (dw_version_id);
