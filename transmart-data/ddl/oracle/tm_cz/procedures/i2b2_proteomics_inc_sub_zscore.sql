--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_PROTEOMICS_INC_SUB_ZSCORE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_PROTEOMICS_INC_SUB_ZSCORE" 
( trial_id IN VARCHAR
) AS
  TrialID		varchar2(100);
  
  
  
 cursor zscore_params is 
  
   SELECT count(*) coun
FROM UPDATEZSCORE_PROTEOMICS;

  

BEGIN
dbms_output.put_line('-');
	TrialID := upper(trial_id);
        
        -- call the cursor and update the z-score value for incremental data;
/*for UpdateZscore in zscore_params
 loop
 
 /*update DE_SUBJECT_PROTEIN_DATA d set d.zscore=(CASE WHEN UpdateZscore.stddev_value=0 THEN 0 ELSE (d.log_intensity - UpdateZscore.median_value ) / UpdateZscore.stddev_value END)
                                              where d.trial_name=TrialID
                                              and d.component=UpdateZscore.component;
                                              dbms_output.put_line(UpdateZscore.coun);
                                              
  end loop;*/
dbms_output.put_line('-');
--Normalize the  zscore value when greater than 2.5 and lesser than -2.5
/*
update DE_SUBJECT_PROTEIN_DATA set zscore=round(CASE WHEN zscore < -2.5 THEN -2.5 WHEN zscore >  2.5 THEN  2.5 ELSE round(zscore,5) END,5)
                                        where trial_name=TrialID;
                                        
                     dbms_output.put_line('111');                   
         -- select 0 into rtn_code from dual;*/
        insert into UPDATEZSCORE_PROTEOMICS(mean_value,median_value,stddev_value) select 0,0,0 from dual;
        
        for UpdateZscore in zscore_params
 loop
 
 /*update DE_SUBJECT_PROTEIN_DATA d set d.zscore=(CASE WHEN UpdateZscore.stddev_value=0 THEN 0 ELSE (d.log_intensity - UpdateZscore.median_value ) / UpdateZscore.stddev_value END)
                                              where d.trial_name=TrialID
                                              and d.component=UpdateZscore.component;*/
                                              dbms_output.put_line(UpdateZscore.coun);
                                              
  end loop;
        




END ;
/
