--
-- Name: de_variant_summary_detail_gene; Type: VIEW; Schema: deapp; Owner: -
--
CREATE VIEW de_variant_summary_detail_gene AS
 SELECT summary.variant_subject_summary_id,
    summary.chr,
    summary.pos,
    summary.dataset_id,
    summary.subject_id,
    summary.rs_id,
    summary.variant,
    summary.variant_format,
    summary.variant_type,
    summary.reference,
    summary.allele1,
    summary.allele2,
    summary.assay_id,
    detail.ref,
    detail.alt,
    detail.qual,
    detail.filter,
    detail.info,
    detail.format,
    detail.variant_value,
    genesymbol.text_value AS gene_name,
    geneid.text_value AS gene_id
   FROM (((de_variant_subject_summary summary
     JOIN de_variant_subject_detail detail ON ((((((detail.dataset_id)::text = (summary.dataset_id)::text) AND ((detail.chr)::text = (summary.chr)::text)) AND (detail.pos = summary.pos)) AND ((detail.rs_id)::text = (summary.rs_id)::text))))
     LEFT JOIN de_variant_population_data genesymbol ON ((((((genesymbol.dataset_id)::text = (summary.dataset_id)::text) AND ((genesymbol.chr)::text = (summary.chr)::text)) AND (genesymbol.pos = summary.pos)) AND ((genesymbol.info_name)::text = 'GS'::text))))
     LEFT JOIN de_variant_population_data geneid ON ((((((geneid.dataset_id)::text = (summary.dataset_id)::text) AND ((geneid.chr)::text = (summary.chr)::text)) AND (geneid.pos = summary.pos)) AND ((geneid.info_name)::text = 'GID'::text))));

