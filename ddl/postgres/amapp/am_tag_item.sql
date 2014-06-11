--
-- Name: am_tag_item; Type: TABLE; Schema: amapp; Owner: -
--
CREATE TABLE am_tag_item (
    tag_template_id bigint NOT NULL,
    tag_item_id bigint NOT NULL,
    required character varying(1),
    display_order bigint,
    display_name character varying(200) NOT NULL,
    gui_handler character varying(200) NOT NULL,
    max_values bigint,
    code_type_name character varying(200),
    editable character varying(1),
    active_ind boolean NOT NULL,
    tag_item_uid character varying(300) NOT NULL,
    tag_item_attr character varying(300),
    tag_item_type character varying(200),
    view_in_grid smallint,
    tag_item_subtype character varying(200),
    view_in_child_grid smallint
);

--
-- Name: am_tag_item_pkey; Type: CONSTRAINT; Schema: amapp; Owner: -
--
ALTER TABLE ONLY am_tag_item
    ADD CONSTRAINT am_tag_item_pkey PRIMARY KEY (tag_template_id, tag_item_id);

--
-- Name: tf_trg_am_tag_item_id(); Type: FUNCTION; Schema: amapp; Owner: -
--
CREATE FUNCTION tf_trg_am_tag_item_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.TAG_ITEM_ID is null then
 select nextval('amapp.SEQ_AMAPP_DATA_ID') into NEW.TAG_ITEM_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_am_tag_item_id; Type: TRIGGER; Schema: amapp; Owner: -
--
CREATE TRIGGER trg_am_tag_item_id BEFORE INSERT ON am_tag_item FOR EACH ROW EXECUTE PROCEDURE tf_trg_am_tag_item_id();

--
-- Name: seq_amapp_data_id; Type: SEQUENCE; Schema: amapp; Owner: -
--
CREATE SEQUENCE seq_amapp_data_id
    START WITH 1995735
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

