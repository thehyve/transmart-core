# Upgrade an existing installation

This page describes how to upgrade to the latest version of tranSMART from an older version of tranSMART.

## Database

If you want to upgrade existing tranSMART database, created with `transmart-data`,
to a new release, follow the [database update instructions](../transmart-data/updatedb) there.
If your database was created by applying the Liquibase scripts in [transmart-schemas](../transmart-schemas)
at application startup, database migration should be run automatically at application restart. 

## tranSMART API Server with GlowingBear user interface

If you want to start using [tranSMART API Server](../transmart-api-server) and the new user interface:

 1. [Deploy a new war for tranSMART API Server](README.md#4-build-and-run-transmart-server).
 2. [Create a configuration file](README.md#3-generate-configuration-files).
 3. [Set up the authentication](.../transmart-api-server#how-to-set-up-authentication-for-the-api-server).
 4. [Install the latest version of Glowing Bear web application](https://github.com/thehyve/glowing-bear/tree/master).
