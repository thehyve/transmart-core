--
-- Type: VIEW; Owner: DEAPP; Name: DE_VARIANT_SUMMARY_DETAIL_GENE
--
CREATE OR REPLACE FORCE VIEW "DEAPP"."DE_VARIANT_SUMMARY_DETAIL_GENE" ("VARIANT_SUBJECT_SUMMARY_ID", "CHR", "POS", "DATASET_ID", "SUBJECT_ID", "RS_ID", "VARIANT", "VARIANT_FORMAT", "VARIANT_TYPE", "REFERENCE", "ALLELE1", "ALLELE2", "ASSAY_ID", "REF", "ALT", "QUAL", "FILTER", "INFO", "FORMAT", "VARIANT_VALUE", "GENE_NAME", "GENE_ID") AS 
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
 FROM deapp.de_variant_subject_summary summary
 JOIN deapp.de_variant_subject_detail detail ON detail.dataset_id = summary.dataset_id AND detail.chr = summary.chr AND detail.pos = summary.pos AND detail.rs_id = summary.rs_id
 LEFT JOIN deapp.de_variant_population_data genesymbol ON genesymbol.dataset_id = summary.dataset_id AND genesymbol.chr = summary.chr AND genesymbol.pos = summary.pos AND genesymbol.info_name = 'GS'
 LEFT JOIN deapp.de_variant_population_data geneid ON geneid.dataset_id = summary.dataset_id AND geneid.chr = summary.chr AND geneid.pos = summary.pos AND geneid.info_name = 'GID';

