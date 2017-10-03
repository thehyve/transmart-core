// configuration for plugin testing - will not be included in the plugin zip

/* Keep pre-2.3.0 behavior */
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

///*
//Example configuration for using the reveng plugin
grails.plugin.reveng.defaultSchema = 'i2b2demodata'
grails.plugin.reveng.includeTables = ['modifier_dimension', 'modifier_metadata']
grails.plugin.reveng.packageName = 'org.transmartproject.db.i2b2data'
//*/

grails.converters.default.pretty.print=true

grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory'
}

// environment specific settings
environments {
    test {
        dataSource {
            url = 'jdbc:postgresql://localhost:5433/transmart'
            driverClassName = 'org.postgresql.Driver'
            username = 'biomart_user'
            password = 'biomart_user'
            logSql = true
            formatSql = true
        }
    }
    test_postgresql {
        dataSource {
            url = 'jdbc:postgresql://localhost:5432/transmart'
            driverClassName = 'org.postgresql.Driver'
            username = 'biomart_user'
            password = 'biomart_user'
            logSql = true
            formatSql = true
        }
    }
    test_oracle {
        dataSource {
            url = 'jdbc:oracle:thin:@localhost:1521:ORCL'
            driverClassName = 'oracle.jdbc.driver.OracleDriver'
            username = 'biomart_user'
            password = 'biomart_user'
            logSql = true
            formatSql = true
        }
    }
}
