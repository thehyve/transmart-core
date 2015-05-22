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
-- Name: cz_form_layout_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_form_layout
    ADD CONSTRAINT cz_form_layout_pk PRIMARY KEY (form_layout_id);

--
-- Name: tf_trg_cz_form_layout_id(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_form_layout_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.FORM_LAYOUT_ID is null then
 select nextval('tm_cz.SEQ_CZ_FORM_LAYOUT_ID') into NEW.FORM_LAYOUT_ID ;
 end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_cz_form_layout_id; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_form_layout_id BEFORE INSERT ON cz_form_layout FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_form_layout_id();

--
-- Name: seq_cz_form_layout_id; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_cz_form_layout_id
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

