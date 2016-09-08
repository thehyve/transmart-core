#!/usr/bin/env python3

# Usage: ./compare_grails_upgraded_sources.py <grails2 plugin> <grails3 plugin>
# This will open 'meld' sequentially on all the source paths that differ between the grails versions.
# see http://meldmerge.org, available for all platforms.


dirs = '''
grails-app/conf/BuildConfig.groovy      build.gradle	Build time configuration is now defined in a Gradle build file
grails-app/conf/Config.groovy	grails-app/conf/application.groovy	Renamed for consistency with Spring Boot
grails-app/conf/UrlMappings.groovy	grails-app/controllers/UrlMappings.groovy	Moved since grails-app/conf is not a source directory anymore
grails-app/conf/BootStrap.groovy	grails-app/init/BootStrap.groovy	Moved since grails-app/conf is not a source directory anymore
scripts	src/main/scripts	Moved for consistency with Gradle
src/groovy	src/main/groovy	Moved for consistency with Gradle
src/java	src/main/java	Moved for consistency with Gradle
test/unit	src/test/groovy	Moved for consistency with Gradle
test/integration	src/integration-test/groovy	Moved for consistency with Gradle
web-app	src/main/webapp or src/main/resources/	Moved for consistency with Gradle
*GrailsPlugin.groovy	src/main/groovy	The plugin descriptor moved to a source directory'''

dirmap = [
#    ['grails-app/conf/BuildConfig.groovy', 'build.gradle'],
    ['grails-app/conf/Config.groovy', 'grails-app/conf/application.groovy'],
    ['grails-app/conf/UrlMappings.groovy', 'grails-app/controllers/UrlMappings.groovy'],
    ['grails-app/conf/BootStrap.groovy', 'grails-app/init/BootStrap.groovy'],
    ['grails-app/views','grails-app/views'],
    ['grails-app', 'grails-app'],
    ['scripts', 'src/main/scripts'],
    ['src/groovy', 'src/main/groovy'],
    ['src/java', 'src/main/java'],
    ['test/unit', 'src/test/groovy'],
    ['test/integration', 'src/integration-test/groovy'],
    ['web-app', 'src/main/resources/public'],
    ['*GrailsPlugin.groovy', 'src/main/groovy']
]


import click
from os import path
import subprocess, glob

@click.command()
@click.argument('grails2', type=click.Path(exists=True, file_okay=False))
@click.argument('grails3', type=click.Path(exists=True, file_okay=False))
def diff(grails2, grails3):
    print("comparing", grails2, "and", grails3)

    for source, target in dirmap:
        grails2_root = path.join(grails2, source)
        grails3_root = path.join(grails3, target)

        if '*' in source:
            # find the new location of *GrailsPlugin.groovy
            try:
            	grails2_root = glob.glob(grails2_root)[0]
            except IndexError:
            	print('IndexError for source: {!r}, grails2_root: {!r}, glob: {}'.format(
            					source, grails2_root, glob.glob(grails2_root)))
            	raise
            	
            find_output = subprocess.check_output(['find', grails3_root, '-name', path.basename(grails2_root)]).split(b'\n')
            if len(find_output) == 0:
                print("Ported equivalent of {} not found".format(grails2_root))
                print("Skipping", grails2_root)
                continue
            grails3_root = find_output[0].decode()

        if path.exists(grails2_root):
            print("Running meld", grails2_root, grails3_root)
            subprocess.call(['meld', grails2_root, grails3_root])
        else:
            print("Skipping", grails2_root)



if __name__ == '__main__':
    diff()
