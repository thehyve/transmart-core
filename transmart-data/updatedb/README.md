Database update scripts
========================================

:warning: Deprecated
--------------------

The transmart-data collection of scripts is not anymore
the recommended way to create a TranSMART database.
Please use the Liquibase scripts in [transmart-schemas](../../transmart-schemas)
instead.


Introduction
------------

This directory contains a set of scripts to update the database for:
- release 1.2.4 installation to release 16.1
- release 16.1 to release 16.2
- release 16.1 to release 17.1
- release 17.1 to release 17.1-HYVE-1
- release 17.1-HYVE-1 to release 17.1-HYVE-2
- release 17.1-HYVE-2 to release 17.1-HYVE-3
- release 17.1-HYVE-3 to release 17.1-HYVE-4
- release 17.1-HYVE-4 to release 17.1-HYVE-5
- release 17.1-HYVE-5 to release 17.1-HYVE-5.2
- release 17.1-HYVE-5.2 to release 17.1-HYVE-5.7
- release 17.1-HYVE-5.7 to release 17.1-HYVE-5.8
- release 17.1-HYVE-5.8 to release 17.1-HYVE-6
- latest release to current development version

Each release folder contains the scripts to update from the previous release.
Release folders contain subfolders for Postgres and Oracle databases.
Use `dev` as a template or folder to collect database changes during development.
