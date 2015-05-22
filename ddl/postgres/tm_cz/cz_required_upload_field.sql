set search_path = tm_cz, pg_catalog;
--
-- Name: cz_required_upload_field; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_required_upload_field (
    required_upload_field_id bigint NOT NULL,
    type character varying(50),
    field character varying(50)
);

--
-- Name: cz_required_upload_field_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_required_upload_field
    ADD CONSTRAINT cz_required_upload_field_pk PRIMARY KEY (required_upload_field_id);

--
-- Name: tf_trg_cz_req_upload_field_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_req_upload_field_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.REQUIRED_UPLOAD_FIELD_ID is null then
 select nextval('tm_cz.SEQ_REQUIRED_UPLOAD_FIELD_ID') into NEW.REQUIRED_UPLOAD_FIELD_ID ;
 end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_req_upload_field_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_req_upload_field_id BEFORE INSERT ON cz_required_upload_field FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_req_upload_field_id();

--
-- Name: seq_required_upload_field_id; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_required_upload_field_id
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

