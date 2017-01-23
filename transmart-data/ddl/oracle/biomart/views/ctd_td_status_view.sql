--
-- Type: VIEW; Owner: BIOMART; Name: CTD_TD_STATUS_VIEW
--
  CREATE OR REPLACE FORCE VIEW "BIOMART"."CTD_TD_STATUS_VIEW" ("ID", "REF_ARTICLE_PROTOCOL_ID", "TRIAL_STATUS", "TRIAL_PHASE") AS 
  select rownum as ID, v."REF_ARTICLE_PROTOCOL_ID",v."TRIAL_STATUS",v."TRIAL_PHASE"
 from (
 select distinct REF_ARTICLE_PROTOCOL_ID,
       TRIAL_STATUS,
       TRIAL_PHASE   
 from ctd_full
 where TRIAL_STATUS is not null or TRIAL_PHASE is not null
 order by REF_ARTICLE_PROTOCOL_ID
 ) v





;
 
