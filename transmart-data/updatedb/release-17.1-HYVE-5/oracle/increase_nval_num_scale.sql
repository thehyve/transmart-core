-- Increase the scale of numerical observations to 16.
alter table I2B2DEMODATA.OBSERVATION_FACT modify (NVAL_NUM NUMBER(29,16));
