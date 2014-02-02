## dalliance-plugin for tranSMART

Dalliance is a lightweight genome visualization tool that's easy to embed in web pages and applications. It supports integration of data from a wide variety of sources, and can integrate data either from DAS servers or directly from popular genomics file formats. (http://www.biodalliance.org/)

This repository contains Dalliance plugin for tranSMART based on Dalliance ver 0.10.5. This plugin currently only works in combination with TheHyve's transmartApp project (https://github.com/thehyve/transmartApp)

## How to Use:

Edit BuildConfig.groovy in transmartApp project.

     plugins {
            ..
            runtime ':dalliance-plugin:0.1-SNAPSHOT'
            ..
    }
    
    ..
    
        repositories {
            mavenRepo([
                    name: 'repo.thehyve.nl-public',
                    url: 'https://repo.thehyve.nl/content/repositories/public/',
    ])


After restarting transmartApp, dalliance-plugin will be downloaded and installed.


