## dalliance-plugin for tranSMART

Dalliance is a lightweight genome visualization tool that's easy to embed in web pages and applications. It supports integration of data from a wide variety of sources, and can integrate data either from DAS servers or directly from popular genomics file formats. (http://www.biodalliance.org/)

This repository contains Dalliance plugin for tranSMART based on Dalliance ver 0.12. This plugin currently only works in combination with TheHyve's transmartApp project (https://github.com/thehyve/transmartApp)

### How to Use:

Dalliance plugin can be embedded in your transmart application by include it in transmartApp's BuildConfig.groovy to have dalliance-plugin for transmart and add The Hyve repository

```
     plugins {
            ..
            runtime ':dalliance-plugin:0.2-SNAPSHOT'
            ..
    }
    
    ..
    
        repositories {
            mavenRepo([
                    name: 'repo.thehyve.nl-public',
                    url: 'https://repo.thehyve.nl/content/repositories/public/',
    ])
```

After restarting transmartApp, dalliance-plugin will be downloaded and installed.


###Embedding dalliance-plugin as inplace-plugin (for Development)

- Clone this project
- Add following line in transmartApp's BuildConfig.groovy  

```
     grails.plugin.location.'dalliance-plugin' = '/path/to/dalliance-plugin project'
```
