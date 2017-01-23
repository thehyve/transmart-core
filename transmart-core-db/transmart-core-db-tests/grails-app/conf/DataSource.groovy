hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
//    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
}

// environment specific settings
environments {
    test {
        dataSource {
            driverClassName = "org.h2.Driver"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM './h2_init.sql'"
            //url = "jdbc:h2:tcp://localhost//tmp/h2;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM '/home/glopes/repos/core-db/transmart-core-db-tests/h2_init.sql'"
            username = "sa"
            password = ""

            logSql    = true
            formatSql = true

            dbCreate = "update"
            pooled = true

            properties {
                // these small values make it easier to find leaks
                maxActive = 3
                minIdle = 1
                maxIdle = 1
            }
        }
    }
}
