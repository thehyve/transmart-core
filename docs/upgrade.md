---
title: Upgrade
---
# Upgrade

This page describes how to upgrade to the latest version of tranSMART from an older version of tranSMART.

  * [Database](#database)
  * [Application](#application)

## Database
If you want to upgrade existing tranSMART database to a new release follow [the database update instruction](../transmart-data/updatedb).

## Application

A way to deploy the application has changed since 17.1 version. There are two options to do this.

### tranSMART API Server with GlowingBear user interface

If you want to start using [tranSMART API Server](../transmart-api-server) and the new user interface:

 1. [Deploy a new war for tranSMART API Server](README.md#4-build-and-run-transmart-server).

 2. [Create a configuration file](README.md#3-generate-configuration-files).

 3. [Set up the authentication](.../transmart-api-server#how-to-set-up-authentication-for-the-api-server).

 4. [Install the latest version of Glowing Bear web application](https://github.com/thehyve/glowing-bear/tree/master).


### tranSMART Server with transmartApp user interface

If you want to use tranSMART REST API with transmartApp web application, you just need to deploy a new war for [tranSMART Server](../transmart-server) and take care of regenerating external configuration files.

Follow [the instruction](README.md#4-build-and-run-transmart-server) on how to start the application
and [the instruction](README.md#3-generate-configuration-files) on how to generate configuration files.

