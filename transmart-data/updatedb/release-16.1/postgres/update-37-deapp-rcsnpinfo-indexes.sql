--
-- add indexes for deapp.de_rc_snp_info
--

set search_path = deapp, pg_catalog;

--
-- Name: de_rc_snp_info_chrom_pos_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_rc_snp_info_chrom_pos_idx ON de_rc_snp_info USING btree (chrom, pos);

ALTER INDEX de_rc_snp_info_chrom_pos_idx SET TABLESPACE indx;

--
-- Name: de_rc_snp_info_entrez_id_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_rc_snp_info_entrez_id_idx ON de_rc_snp_info USING btree (entrez_id);

ALTER INDEX de_rc_snp_info_entrez_id_idx SET TABLESPACE indx;

--
-- Name: de_rc_snp_info_rs_id_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_rc_snp_info_rs_id_idx ON de_rc_snp_info USING btree (rs_id);

ALTER INDEX de_rc_snp_info_rs_id_idx SET TABLESPACE indx;
