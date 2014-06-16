/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

// these settings can be overriden in ~/.grails/transmartConfig/DataSource-rest-api.groovy

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
//    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
}

environments {
    development {
        dataSource {
            driverClassName = 'org.postgresql.Driver'
            url             = 'jdbc:postgresql://localhost:5432/transmart'
            dialect         = 'org.hibernate.dialect.PostgreSQLDialect'

            //    driverClassName = 'oracle.jdbc.driver.OracleDriver'
            //    url             = 'jdbc:oracle:thin:@localhost:11521:CI'
            //    dialect         = 'org.hibernate.dialect.Oracle10gDialect'

            username        = 'biomart_user'
            password        = 'biomart_user'
            dbCreate        = 'none'
        }
    }
}

environments {
    development {
        dataSource {
            //logSql    = true
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
            url             = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM './h2_init.sql'"
            username        = 'sa'
            password        = ''
            dbCreate        = 'update'
            //logSql          = true
            formatSql       = true
        }
    }
}

// vim: set ts=4 sw=4 et:
