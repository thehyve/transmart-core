--
-- Name: lt_chromosomal_region_gpl_id_fkey; Type: FK CONSTRAINT; Schema: tm_lz; Owner: -
--
ALTER TABLE ONLY lt_chromosomal_region
    ADD CONSTRAINT lt_chromosomal_region_gpl_id_fkey FOREIGN KEY (gpl_id) REFERENCES deapp.de_gpl_info(platform);

