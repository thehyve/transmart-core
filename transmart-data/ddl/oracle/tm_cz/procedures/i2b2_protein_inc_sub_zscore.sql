--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_PROTEIN_INC_SUB_ZSCORE
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_PROTEIN_INC_SUB_ZSCORE" 
( trial_id IN VARCHAR
) AS
  TrialID		varchar2(100);
  
  coun number;
  
    cursor zscore_params is 
    SELECT round(median(d.log_intensity),5)median_value,round(STDDEV(d.log_intensity),5)stddev_value,d.component
    FROM DE_SUBJECT_PROTEIN_DATA d
    WHERE d.trial_name = trial_id
    GROUP BY d.trial_name,d.component;
  

BEGIN
        --dbms_output.put_line('1');
	TrialID := upper(trial_id);
        coun:=0;
        
        -- call the cursor and update the z-score value for incmental data;
            for UpdateZscore in zscore_params
              loop
 
               update UPDATEZSCORE_PROTEOMICS d set d.zscore=(CASE WHEN UpdateZscore.stddev_value=0 THEN 0 ELSE ((d.log_intensity - UpdateZscore.median_value ) / UpdateZscore.stddev_value) END)
                                                                where d.trial_name=TrialID
                                                                and d.component=UpdateZscore.component;
                     commit;  
                     /*coun:=coun+1;   
                      dbms_output.put_line(coun); */
            end loop;
                  --dbms_output.put_line('12');
                --Normalize the  zscore value when greater than 2.5 and lesser than -2.5

              update DE_SUBJECT_PROTEIN_DATA set zscore=round(CASE WHEN zscore < -2.5 THEN -2.5 WHEN zscore >  2.5 THEN  2.5 ELSE round(zscore,5) END,5)
                                                        where trial_name=TrialID;
                  commit;                      
                    -- dbms_output.put_line('111');                   
         -- select 0 into rtn_code from dual;


END I2B2_PROTEIN_INC_SUB_ZSCORE;
/
