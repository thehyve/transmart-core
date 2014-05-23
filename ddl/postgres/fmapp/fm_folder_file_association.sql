--
-- Name: fm_folder_file_association; Type: TABLE; Schema: fmapp; Owner: -
--
CREATE TABLE fm_folder_file_association (
    folder_id bigint NOT NULL,
    file_id bigint NOT NULL
);

--
-- Name: pk_folder_file_assoc; Type: CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_folder_file_association
    ADD CONSTRAINT pk_folder_file_assoc PRIMARY KEY (folder_id, file_id);

--
-- Name: fk_fm_folder_fm_folder; Type: FK CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_folder_file_association
    ADD CONSTRAINT fk_fm_folder_fm_folder FOREIGN KEY (folder_id) REFERENCES fm_folder(folder_id);

--
-- Name: fk_folder_file_assoc_file; Type: FK CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_folder_file_association
    ADD CONSTRAINT fk_folder_file_assoc_file FOREIGN KEY (file_id) REFERENCES fm_file(file_id);

