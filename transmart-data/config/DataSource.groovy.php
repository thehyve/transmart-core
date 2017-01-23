<?php require __DIR__ . '/../lib/php/env_helper.inc.php'; ?>
dataSource {
<?php if (isset($_ENV['ORACLE'])) { ?>
    driverClassName = 'oracle.jdbc.driver.OracleDriver'
    url             = 'jdbc:oracle:thin:@<?= $_ENV['ORAHOST'] ?>:<?= $_ENV['ORAPORT'] ?>:<?= $_ENV['ORASID'] ?>'
    dialect         = 'org.hibernate.dialect.Oracle10gDialect'
<?php } else { ?>
    driverClassName = 'org.postgresql.Driver'
    url             = 'jdbc:postgresql://<?= $host ?>:<?= $_ENV['PGPORT'] ?>/<?= $_ENV['PGDATABASE'] ?>'
    dialect         = 'org.hibernate.dialect.PostgreSQLDialect'
<?php } ?>
    username        = 'biomart_user'
    password        = '<?= $biomart_user_pwd ?>'
    dbCreate        = 'none'
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

// for old versions that don't specify this in the in-tree one
if (hibernate.cache.region.factory_class != 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory') {
    hibernate {
        cache.use_query_cache        = true
        cache.use_second_level_cache = true
        cache.provider_class         = 'org.hibernate.cache.EhCacheProvider'
    }
}

// vim: set ts=4 sw=4 et:
