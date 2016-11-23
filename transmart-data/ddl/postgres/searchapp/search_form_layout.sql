--
-- Name: search_form_layout; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_form_layout (
    form_layout_id bigint NOT NULL,
    form_key character varying(50),
    form_column character varying(50),
    display_name character varying(50),
    data_type character varying(50),
    sequence bigint,
    display character(1)
);

--
-- Name: search_form_layout_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_form_layout
    ADD CONSTRAINT search_form_layout_pk PRIMARY KEY (form_layout_id);

--
-- Name: tf_trg_search_form_layout_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_form_layout_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.FORM_LAYOUT_ID is null then
 select nextval('searchapp.SEQ_SEARCH_FORM_LAYOUT_ID') into NEW.FORM_LAYOUT_ID ;
 end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_search_form_layout_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_form_layout_id BEFORE INSERT ON search_form_layout FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_form_layout_id();

--
-- Name: seq_search_form_layout_id; Type: SEQUENCE; Schema: searchapp; Owner: -
--
CREATE SEQUENCE seq_search_form_layout_id
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

