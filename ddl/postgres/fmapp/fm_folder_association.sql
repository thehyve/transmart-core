--
-- Name: fm_folder_association; Type: TABLE; Schema: fmapp; Owner: -
--
CREATE TABLE fm_folder_association (
    folder_id bigint NOT NULL,
    object_uid character varying(300) NOT NULL,
    object_type character varying(100) NOT NULL
);
--
-- Type: REF_CONSTRAINT; Name: fk_fm_folder_assoc_fm_folder; Schema: fmapp; Owner: -
--
ALTER TABLE fm_folder_association
    ADD CONSTRAINT fk_fm_folder_assoc_fm_folder FOREIGN KEY (folder_id) REFERENCES fm_folder(folder_id);

--
-- Name: pk_folder_assoc; Type: CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_folder_association
    ADD CONSTRAINT pk_folder_assoc PRIMARY KEY (folder_id, object_uid);
