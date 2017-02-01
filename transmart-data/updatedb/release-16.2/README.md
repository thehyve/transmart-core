Database update scripts for release 16.2
========================================

Overview
--------

These are main changes:

- new tables for xnat functionality
- length of `subject_id` column has been increased
- fix type of the measurements columns (e.g. intensity, z-score, ...). It has to be a real number. 
- some fixes to the ETL stored procedures.

How to apply all changes
------------------------

Given that transmart-data is configured correctly you could run the following make command

    make -C postgres migrate