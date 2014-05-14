--
-- Name: validate_hd_data_view; Type: VIEW; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE VIEW validate_hd_data_view (lvl, data_type, message) AS SELECT 
  valid.lvl,
  valid.data_type,
  valid.message
 FROM validate_hd_data valid
;
