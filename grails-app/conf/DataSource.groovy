// dataSource {
//     pooled = true
//     driverClassName = "org.h2.Driver"
//     username = "sa"
//     password = ""
// }
// hibernate {
//     cache.use_second_level_cache = true
//     cache.use_query_cache = false
//     cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
// //    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
// }

// // environment specific settings
// environments {
//     development {
//         dataSource {
//             dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
//             url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
//         }
//     }
//     test {
//         dataSource {
//             dbCreate = "update"
//             url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
//         }
//     }
//     production {
//         dataSource {
//             dbCreate = "update"
//             url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
//             properties {
//                maxActive = -1
//                minEvictableIdleTimeMillis=1800000
//                timeBetweenEvictionRunsMillis=1800000
//                numTestsPerEvictionRun=3
//                testOnBorrow=true
//                testWhileIdle=true
//                testOnReturn=false
//                validationQuery="SELECT 1"
//                jdbcInterceptors="ConnectionState"
//             }
//         }
//     }
// }

dataSource {
    // driverClassName = 'org.postgresql.Driver'
    // url             = 'jdbc:postgresql://localhost:5432/transmart'
    // dialect         = 'org.hibernate.dialect.PostgreSQLDialect'

    // driverClassName = "org.h2.Driver"
    // username = "sa"
    // password = ""

    driverClassName = 'oracle.jdbc.driver.OracleDriver'
    url             = 'jdbc:oracle:thin:@localhost:11521:CI'
    dialect         = 'org.hibernate.dialect.Oracle10gDialect'
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
//             dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
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
//            dbCreate = "update"
            logSql    = true
            formatSql = true
            //url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=" +
                    "RUNSCRIPT FROM './h2_init.sql'"
             properties {
                maxActive   = 10
                maxIdle     = 5
                minIdle     = 2
                initialSize = 2
            }

        }
    }
  production {
        dataSource {
            logSql    = false
            formatSql = false
             properties {
                maxActive   = 50
                maxIdle     = 25
                minIdle     = 5
                initialSize = 5
            }
        }
    }
}

// vim: set ts=4 sw=4 et:
