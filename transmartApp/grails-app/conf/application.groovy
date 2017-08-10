hibernate {
    cache.use_query_cache        = true
    cache.use_second_level_cache = true

    // make sure hibernate.cache.provider_class is not being set
    // see http://stackoverflow.com/a/3690212/127724 and the docs for the cache-ehcache plugin
    //cache.region.factory_class   = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory'
    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory'
}

environments {
    test {
        dataSources {
            dataSource {
                driverClassName = 'org.h2.Driver'
                url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM './h2_init.sql'"
                dialect = 'org.hibernate.dialect.H2Dialect'
                username = 'sa'
                password = ''
                dbCreate = 'update'
                logSql = true
                formatSql = true
            }
        }
        hibernate {
            cache.use_second_level_cache = true
            cache.use_query_cache = false
        }
    }
}
