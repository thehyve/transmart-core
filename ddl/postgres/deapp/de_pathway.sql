--
-- Name: de_pathway; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_pathway (
    name character varying(300),
    description character varying(510),
    id bigint NOT NULL,
    type character varying(100),
    source character varying(100),
    externalid character varying(100),
    pathway_uid character varying(200),
    user_id bigint
);

--
-- Name: de_pathway_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_pathway
    ADD CONSTRAINT de_pathway_pkey PRIMARY KEY (id);

