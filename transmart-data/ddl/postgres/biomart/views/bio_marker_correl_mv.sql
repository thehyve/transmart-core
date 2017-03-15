--
-- Name: bio_marker_correl_mv; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_marker_correl_mv AS
 SELECT DISTINCT b.bio_marker_id,
    b.bio_marker_id AS asso_bio_marker_id,
    'GENE'::text AS correl_type,
    1 AS mv_id
   FROM bio_marker b
  WHERE ((b.bio_marker_type)::text = 'GENE'::text)
UNION
 SELECT DISTINCT b.bio_marker_id,
    b.bio_marker_id AS asso_bio_marker_id,
    'PROTEIN'::text AS correl_type,
    4 AS mv_id
   FROM bio_marker b
  WHERE ((b.bio_marker_type)::text = 'PROTEIN'::text)
UNION
 SELECT DISTINCT b.bio_marker_id,
    b.bio_marker_id AS asso_bio_marker_id,
    'MIRNA'::text AS correl_type,
    7 AS mv_id
   FROM bio_marker b
  WHERE ((b.bio_marker_type)::text = 'MIRNA'::text)
UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
    c.asso_bio_data_id AS asso_bio_marker_id,
    'PATHWAY GENE'::text AS correl_type,
    2 AS mv_id
   FROM bio_marker b,
    bio_data_correlation c,
    bio_data_correl_descr d
  WHERE ((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id) AND ((b.primary_source_code)::text <> 'ARIADNE'::text) AND ((d.correlation)::text = 'PATHWAY GENE'::text))
UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
    c.asso_bio_data_id AS asso_bio_marker_id,
    'HOMOLOGENE_GENE'::text AS correl_type,
    3 AS mv_id
   FROM bio_marker b,
    bio_data_correlation c,
    bio_data_correl_descr d
  WHERE ((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id) AND ((d.correlation)::text = 'HOMOLOGENE GENE'::text))
UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
    c.asso_bio_data_id AS asso_bio_marker_id,
    'PROTEIN TO GENE'::text AS correl_type,
    5 AS mv_id
   FROM bio_marker b,
    bio_data_correlation c,
    bio_data_correl_descr d
  WHERE ((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id) AND ((d.correlation)::text = 'PROTEIN TO GENE'::text))
UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
    c.asso_bio_data_id AS asso_bio_marker_id,
    'GENE TO PROTEIN'::text AS correl_type,
    6 AS mv_id
   FROM bio_marker b,
    bio_data_correlation c,
    bio_data_correl_descr d
  WHERE ((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id) AND ((d.correlation)::text = 'GENE TO PROTEIN'::text))
UNION
 SELECT DISTINCT c1.bio_data_id AS bio_marker_id,
    c2.asso_bio_data_id AS asso_bio_marker_id,
    'PATHWAY TO PROTEIN'::text AS correl_type,
    8 AS mv_id
   FROM (((bio_data_correlation c1
     JOIN bio_data_correlation c2 ON ((c1.asso_bio_data_id = c2.bio_data_id)))
     JOIN bio_data_correl_descr d1 ON ((c1.bio_data_correl_descr_id = d1.bio_data_correl_descr_id)))
     JOIN bio_data_correl_descr d2 ON ((c2.bio_data_correl_descr_id = d2.bio_data_correl_descr_id)))
  WHERE (((d1.correlation)::text = 'PATHWAY GENE'::text) AND ((d2.correlation)::text = 'GENE TO PROTEIN'::text))
UNION
 SELECT DISTINCT b.bio_marker_id,
    b.bio_marker_id AS asso_bio_marker_id,
    'METABOLITE'::text AS correl_type,
    9 AS mv_id
   FROM bio_marker b
  WHERE ((b.bio_marker_type)::text = 'METABOLITE'::text)
UNION
 SELECT DISTINCT b.bio_marker_id,
    b.bio_marker_id AS asso_bio_marker_id,
    'TRANSCRIPT'::text AS correl_type,
    10 AS mv_id
   FROM bio_marker b
  WHERE ((b.bio_marker_type)::text = 'TRANSCRIPT'::text)
UNION
 SELECT DISTINCT c.bio_data_id AS bio_marker_id,
    c.asso_bio_data_id AS asso_bio_marker_id,
    'GENE TO TRANSCRIPT'::text AS correl_type,
    11 AS mv_id
   FROM bio_marker b,
    bio_data_correlation c,
    bio_data_correl_descr d
  WHERE ((b.bio_marker_id = c.bio_data_id) AND (c.bio_data_correl_descr_id = d.bio_data_correl_descr_id) AND ((d.correlation)::text = 'GENE TO TRANSCRIPT'::text));

