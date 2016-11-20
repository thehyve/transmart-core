package cacheehcache

import grails.plugin.cache.ehcache.EhcacheConfigLoader
import grails.plugin.cache.ehcache.GrailsEhCacheManagerFactoryBean
import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import grails.plugins.Plugin
import net.sf.ehcache.management.ManagementService

class CacheEhcacheGrailsPlugin extends Plugin {

   // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def version = '2.0.0.BUILD-SNAPSHOT'

    def title = "Cache Ehcache" // Headline display name of the plugin
    def author = "Jeff Brown"
    def authorEmail = "jeff@jeffandbetsy.net"
    def description = '''\
An Ehcache-based implementation of the Cache plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/cache-ehcache"

    def license = "APACHE"

    def developers = [ [ name: "Burt Beckwith", email: "burt@burtbeckwith.com" ]]

    def scm = [ url: "https://github.com/grails-plugins/grails-cache-ehcache" ]

    def loadAfter = ['cache']

    Closure doWithSpring() {
        { ->
            def cacheConfig = grailsApplication.config.grails?.cache
            def ehcacheConfig = cacheConfig?.ehcache
            def ehcacheConfigLocation
            boolean reloadable

            // customizable name for the cache manager
            String ehcacheCacheManagerName = ehcacheConfig?.cacheManagerName

            if (ehcacheConfig.reloadable instanceof Boolean) {
                reloadable = ehcacheConfig.reloadable
            } else {
                reloadable = true
            }
            if (ehcacheConfig.ehcacheXmlLocation instanceof CharSequence) {
                // use the specified location
                ehcacheConfigLocation = ehcacheConfig.ehcacheXmlLocation
                log.info "Using Ehcache configuration file $ehcacheConfigLocation"
            } else if (cacheConfig.config instanceof Closure || grailsApplication.cacheConfigClasses) {
                // leave the location null to indicate that the real configuration will
                // happen in doWithApplicationContext (from the core plugin, using this
                // plugin's grailsCacheConfigLoader)
            } else {
                // no config and no specified location, so look for ehcache.xml in the classpath,
                // and fall back to ehcache-failsafe.xml in the Ehcache jar as a last resort
                def ctx = springConfig.unrefreshedApplicationContext
                def defaults = ['classpath:ehcache.xml', 'classpath:ehcache-failsafe.xml']
                ehcacheConfigLocation = defaults.find { ctx.getResource(it).exists() }
                if (ehcacheConfigLocation) {
                    log.info "No Ehcache configuration file specified, using $ehcacheConfigLocation"
                } else {
                    log.error "No Ehcache configuration file specified and default file not found"
                    ehcacheConfigLocation = defaults[1] // won't work but will fail more helpfully
                }
            }

            ehcacheCacheManager(GrailsEhCacheManagerFactoryBean) {
                cacheManagerName = ehcacheCacheManagerName
                configLocation = ehcacheConfigLocation
                rebuildable = reloadable
            }

            grailsCacheConfigLoader(EhcacheConfigLoader) {
                rebuildable = reloadable
            }

            grailsCacheManager(GrailsEhcacheCacheManager) {
                cacheManager = ref('ehcacheCacheManager')
            }

//            grailsCacheMbeanServer(MBeanServerFactoryBean) {
//                locateExistingServerIfPossible = true
//            }

            ehCacheManagementService(ManagementService) { bean ->
                bean.initMethod = 'init'
                bean.destroyMethod = 'dispose'
                bean.constructorArgs = [ehcacheCacheManager, ref('mbeanServer'), true, true, true, true, true]
            }
        }
    }
}
