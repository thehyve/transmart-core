// usually overridden in ~/.grails/transmartConfig/DataSource.groovy
dataSource {
    driverClassName = 'org.postgresql.Driver'
    url             = 'jdbc:postgresql://localhost:5432/transmart'
    dialect         = 'org.hibernate.dialect.PostgreSQLDialect'

    username        = 'biomart_user'
    password        = 'biomart_user'
    dbCreate        = 'none'
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache        = true
    cache.provider_class         = 'org.hibernate.cache.EhCacheProvider'
}

environments {
    development {
        dataSource {
            logSql    = true
            formatSql = true

            properties {
                maxActive   = 10
                maxIdle     = 5
                minIdle     = 2
                initialSize = 2
            }
        }
    }

    test {
        dataSource {
            driverClassName = 'org.h2.Driver'
            url             = 'jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000'

            username        = 'sa'
            password        = ''
            dbCreate        = 'update'

            logSql          = true
            formatSql       = true
        }
    }
}

// vim: set ts=4 sw=4 et:
