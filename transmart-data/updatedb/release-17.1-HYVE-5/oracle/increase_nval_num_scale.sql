-- Increase the scale of numerical observations to 16.
alter table I2B2DEMODATA.OBSERVATION_FACT modify column NVAL_NUM set data type NUMBER(29,16);
