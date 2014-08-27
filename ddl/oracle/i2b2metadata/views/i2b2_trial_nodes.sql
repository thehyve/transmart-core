--
-- Type: VIEW; Owner: I2B2METADATA; Name: I2B2_TRIAL_NODES
--
  CREATE OR REPLACE FORCE VIEW "I2B2METADATA"."I2B2_TRIAL_NODES" ("C_FULLNAME", "TRIAL") AS 
  WITH ranked_i2b2 AS (
    SELECT c_fullname,
           CAST(c_comment AS VARCHAR(100)) as c_comment,
           ROW_NUMBER() OVER(PARTITION BY CAST(c_comment AS VARCHAR(100))
                                 ORDER BY length(c_fullname)) AS rank
      FROM i2b2metadata.i2b2
      WHERE c_comment IS NOT NULL)
SELECT c_fullname, substr(CAST (c_comment AS VARCHAR2(100)), 7) AS trial
  FROM ranked_i2b2
WHERE rank = 1;
 
