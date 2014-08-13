hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}

// environment specific settings
environments {
    development {
        dataSource {
            driverClassName = 'org.postgresql.Driver'
            url             = 'jdbc:postgresql://localhost:5433/transmart'
            dialect         = 'org.hibernate.dialect.PostgreSQLDialect'

//            driverClassName = 'oracle.jdbc.driver.OracleDriver'
//            url             = 'jdbc:oracle:thin:@localhost:11521:CI'
//            dialect         = 'org.hibernate.dialect.Oracle10gDialect'

            username        = 'biomart_user'
            password        = 'biomart_user'
            dbCreate        = 'none'
        }
    }
}
