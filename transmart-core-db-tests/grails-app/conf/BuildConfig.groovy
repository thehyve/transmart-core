grails.project.repos.default = 'repo.thehyve.nl-snapshots'
grails.project.repos."${grails.project.repos.default}".url = 'https://repo.thehyve.nl/content/repositories/snapshots/'
grails.plugin.location.'transmart-core-db' = '../.'

grails.project.dependency.resolution = {
    log "warn"

    inherits('global') {}

    repositories {
        mavenLocal()
        mavenRepo 'https://repo.thehyve.nl/content/repositories/public/'
    }

    dependencies {
        compile('org.hamcrest:hamcrest-library:1.3',
                'org.hamcrest:hamcrest-core:1.3')

        test('junit:junit:4.11') {
            transitive = false /* don't bring hamcrest */
            export     = false
        }

        test('org.gmock:gmock:0.8.3') {
            transitive = false /* don't bring groovy-all */
            export     = false
        }

        /* for reasons I don't want to guess (we'll move away from ivy soon
         * anyway), javassist is not being included in the test classpath
         * when running test-app in Travis even though the hibernate plugin
         * depends on it */
        test('org.javassist:javassist:3.16.1-GA') {
            export = false
        }
    }

    plugins {
        build(":release:2.2.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }
}
