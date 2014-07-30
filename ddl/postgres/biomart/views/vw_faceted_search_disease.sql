--
-- Name: vw_faceted_search_disease; Type: VIEW; Schema: biomart; Owner: biomart
--
CREATE VIEW vw_faceted_search_disease AS
 SELECT
	z.bio_assay_analysis_id,
	REPLACE (
		TRIM (LEADING '/' FROM solr_hierarchy),
		'//',
		'/'
	) AS solr_hierarchy
FROM
	(
		SELECT
			y.bio_assay_analysis_id,
			string_agg (y. id_path, '/') AS solr_hierarchy
		FROM
			(
				SELECT
					x.bio_assay_analysis_id,
					x.top_node,
					MAX (x. id_path) AS id_path
				FROM
					(
						SELECT DISTINCT
							bdd.bio_data_id AS bio_assay_analysis_id,
							substr(mp.id_path, 2, 11) AS top_node,
							mp.id_path
						FROM
							biomart.bio_data_disease bdd,
							biomart.bio_disease bd,
							(
								SELECT
									unique_id,
									mesh_name,
									child_number,
									parent_number,
									id_path
								FROM
									biomart.mesh_path
							) mc,
							(
								SELECT
									unique_id,
									mesh_name,
									child_number,
									parent_number,
									id_path
								FROM
									biomart.mesh_path
							) mp
						WHERE
							bdd.bio_disease_id = bd.bio_disease_id --and bdd.etl_source like 'TEST%'
						AND bd.mesh_code = mc.unique_id
						AND mc.child_number LIKE mp.child_number || '%'
					) x
				GROUP BY
					x.bio_assay_analysis_id,
					x.top_node
				ORDER BY
					x.bio_assay_analysis_id
			) y
		GROUP BY
			y.bio_assay_analysis_id
	) z
ORDER BY
	bio_assay_analysis_id;


