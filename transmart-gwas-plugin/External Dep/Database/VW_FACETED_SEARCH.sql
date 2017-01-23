--------------------------------------------------------
--  File created - Thursday-October-25-2012   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for View VW_FACETED_SEARCH
--------------------------------------------------------


  CREATE OR REPLACE FORCE VIEW "BIOMART"."VW_FACETED_SEARCH" AS 
select ba.bio_assay_analysis_id as ANALYSIS_ID
,be.bio_experiment_id as STUDY
,be.bio_experiment_id as STUDY_ID
,ba.analysis_type as ANALYSES
,ba.bio_assay_data_type as DATA_TYPE 
,bplat.platform_accession as PLATFORM
,bplat.platform_description as PLATFORM_DESCRIPTION
,bplat.platform_vendor as PLATFORM_VENDOR
,baap.platform_name as PLATFORM_NAME
,'OBS:' || bpobs.obs_code as OBSERVATION
,be.title as STUDY_TITLE
,be.description as STUDY_DESCRIPTION
,be.design as STUDY_DESIGN
,be.primary_investigator as STUDY_PRIMARY_INV
,be.contact_field as STUDY_CONTACT_FIELD
,be.overall_design as STUDY_OVERALL_DESIGN
,be.institution as STUDY_INSTITUTION
,be.accession as STUDY_ACCESSION
,be.country as STUDY_COUNTRY
,be.biomarker_type as STUDY_BIOMARKER_TYPE
,be.target as STUDY_TARGET
,be.access_type as STUDY_ACCESS_TYPE
,ba.analysis_name as ANALYSIS_NAME
,ba.short_description as ANALYSIS_DESCRIPTION_S
,ba.long_description as ANALYSIS_DESCRIPTION_L
,ba.analysis_type as ANALYSIS_TYPE
,ba.analyst_name AS ANALYSIS_ANALYST_NAME
,ba.analysis_method_cd as ANALYSIS_METHOD
,ba.bio_assay_data_type as ANALYSIS_DATA_TYPE
,ba.qa_criteria as ANALYSIS_QA_CRITERIA
,bae.model_name as MODEL_NAME
,bae.model_desc as MODEL_DESCRIPTION
,bae.research_unit as RESEARCH_UNIT
,row_number() over (order by ba.bio_assay_analysis_id) as FACET_ID
from bio_assay_analysis ba
Join bio_experiment be 
on ba.etl_id = be.accession
left outer join bio_assay_analysis_ext bae
on bae.bio_assay_analysis_id = ba.bio_assay_analysis_id
left outer join bio_data_platform bdplat
on ba.bio_assay_analysis_id = bdplat.bio_data_id
left outer join bio_assay_platform bplat
on bdplat.bio_assay_platform_id = bplat.bio_assay_platform_id
left outer join bio_data_observation bdpobs
on ba.bio_assay_analysis_id = bdpobs.bio_data_id
left outer join bio_observation bpobs
on bdpobs.bio_observation_id = bpobs.bio_observation_id
left outer join bio_asy_analysis_pltfm baap
on baap.bio_asy_analysis_pltfm_id = ba.bio_asy_analysis_pltfm_id
WHERE lower(be.bio_experiment_type) = 'experiment'; 