--
-- Name: am_tag_association; Type: TABLE; Schema: amapp; Owner: -
--
CREATE TABLE am_tag_association (
    subject_uid character varying(300) NOT NULL,
    object_uid character varying(300) NOT NULL,
    object_type character varying(50),
    tag_item_id bigint
);

--
-- Name: am_tag_association_pkey; Type: CONSTRAINT; Schema: amapp; Owner: -
--
ALTER TABLE ONLY am_tag_association
    ADD CONSTRAINT am_tag_association_pkey PRIMARY KEY (subject_uid, object_uid);

