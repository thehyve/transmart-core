--
-- Name: node_curation; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE node_curation (
    node_type character varying(25),
    node_name character varying(250),
    display_name character varying(250),
    display_in_ui character(1),
    data_type character(1),
    global_flag character(1),
    study_id character varying(30),
    curator_name character varying(250),
    curation_date timestamp without time zone,
    active_flag character(1)
);

--
-- Name: node_curation_pk; Type: INDEX; Schema: tm_cz; Owner: -
--
CREATE UNIQUE INDEX node_curation_pk ON node_curation USING btree (node_type, node_name, study_id);

