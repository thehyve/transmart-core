-- Increase the scale of numerical observations to 16.
alter table i2b2demodata.observation_fact alter column nval_num type numeric(29,16);
