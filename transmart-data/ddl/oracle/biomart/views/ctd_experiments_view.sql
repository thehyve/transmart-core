--
-- Type: VIEW; Owner: BIOMART; Name: CTD_EXPERIMENTS_VIEW
--
  CREATE OR REPLACE FORCE VIEW "BIOMART"."CTD_EXPERIMENTS_VIEW" ("ID", "REF_ARTICLE_PROTOCOL_ID", "DRUG_INHIBITOR_COMMON_NAME", "DRUG_INHIBITOR_DOSE", "DRUG_INHIBITOR_TIME_PERIOD", "DRUG_INHIBITOR_ROUTE_OF_ADMIN", "DRUG_INHIBITOR_TRTMT_REGIME", "COMPARATOR_NAME", "COMPARATOR_DOSE", "COMPARATOR_TIME_PERIOD", "COMPARATOR_ROUTE_OF_ADMIN", "TREATMENT_REGIME", "PLACEBO", "EXPERIMENT_DESCRIPTION") AS 
  SELECT   ROWNUM AS ID,
           v."REF_ARTICLE_PROTOCOL_ID",
           v."DRUG_INHIBITOR_COMMON_NAME",
           v."DRUG_INHIBITOR_DOSE",
           v."DRUG_INHIBITOR_TIME_PERIOD",
           v."DRUG_INHIBITOR_ROUTE_OF_ADMIN",
           v."DRUG_INHIBITOR_TRTMT_REGIME",
           v."COMPARATOR_NAME",
           v."COMPARATOR_DOSE",
           v."COMPARATOR_TIME_PERIOD",
           v."COMPARATOR_ROUTE_OF_ADMIN",
           v."TREATMENT_REGIME",
           v."PLACEBO",
           v."EXPERIMENT_DESCRIPTION"
    FROM   (  SELECT   DISTINCT REF_ARTICLE_PROTOCOL_ID,
                                DRUG_INHIBITOR_COMMON_NAME,
                                DRUG_INHIBITOR_TIME_PERIOD,
                                DRUG_INHIBITOR_DOSE,
                                DRUG_INHIBITOR_ROUTE_OF_ADMIN,
                                DRUG_INHIBITOR_TRTMT_REGIME,
                                COMPARATOR_NAME,
                                COMPARATOR_DOSE,
                                COMPARATOR_TIME_PERIOD,
                                COMPARATOR_ROUTE_OF_ADMIN,
                                TREATMENT_REGIME,
                                PLACEBO,
                                EXPERIMENT_DESCRIPTION
                FROM   ctd_full
            ORDER BY   REF_ARTICLE_PROTOCOL_ID,
                       DRUG_INHIBITOR_COMMON_NAME,
                       DRUG_INHIBITOR_TRTMT_REGIME) v


;
 
