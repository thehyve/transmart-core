--
-- Name: cz_form_layout; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_form_layout (
    form_layout_id bigint NOT NULL,
    form_key character varying(50),
    form_column character varying(50),
    display_name character varying(50),
    data_type character varying(50),
    sequence bigint,
    display character(1)
);

--
-- Type: SEQUENCE; Owner: TM_CZ; Name: SEQ_FORM_LAYOUT_ID
--
CREATE SEQUENCE  seq_form_layout_id
  NO MINVALUE
  NO MAXVALUE
  INCREMENT BY 1
  START WITH 41
  CACHE 20
;

--
-- Name: tf_trg_cz_form_layout_id; Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_form_layout_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.FORM_LAYOUT_ID is null then
 select nextval('tm_cz.SEQ_FORM_LAYOUT_ID') into NEW.FORM_LAYOUT_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_form_layout_id(); Type: TRIGGER; Schema: tm_cz; Owner: -
--
  CREATE TRIGGER trg_cz_form_layout_id BEFORE INSERT ON cz_form_layout FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_form_layout_id();


--
-- Name: cz_form_layout_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_form_layout
    ADD CONSTRAINT cz_form_layout_pk PRIMARY KEY (form_layout_id);
