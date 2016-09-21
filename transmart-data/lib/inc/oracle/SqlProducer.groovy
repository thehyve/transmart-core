/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

package inc.oracle

import groovy.sql.Sql

import java.sql.Connection
import java.sql.DriverManager

import static java.lang.System.getenv
import static groovyx.gpars.GParsPool.withPool

class SqlProducer {
    static Sql createFromEnv(String user = null, String password = null, Boolean enhance = false) {
        Class.forName 'oracle.jdbc.driver.OracleDriver'
        def url = "jdbc:oracle:thin:@${getenv 'ORAHOST'}:${getenv 'ORAPORT'}"
        if (getenv('ORASVC')) {
            url += "/${getenv 'ORASVC'}"
        } else {
            url += ":${getenv 'ORASID'}"
        }

        Connection connection = DriverManager.getConnection(url,
                user ?: getenv('ORAUSER'),
                password ?: getenv('ORAPASSWORD'))
        connection.autoCommit = false
        /* by creating an Sql from a Connection, we ensure only that connection
         * is used by the Sql; it will not generate connections by itself.
         * The effect is basically the same as if we were running always under
         * Sql::cacheConnection() */
        def sql = new Sql(connection)
        if (enhance) {
            sql = new EnhancedSql(sql)
        }
        sql
    }

    static List<Sql> createMultipleFromEnv(Integer n,
                                           String user = null,
                                           String password = null,
                                           Boolean enhance = false) {
        withPool n, {
            def sqlProducerAsync = SqlProducer.&createFromEnv.curry(user, password, enhance).asyncFun()
            Collections.nCopies(n, sqlProducerAsync)*.call()*.get()
        }
    }

    static closeConnections(Collection<Sql> sqls, Closure beforeShutdown = null) {
        if (sqls.empty) {
            return
        }

        withPool sqls.size(), {
            sqls.eachParallel { Sql sql ->
                if (beforeShutdown) {
                    beforeShutdown.call sql
                }
                sql.close()
            }
        }
    }
}
