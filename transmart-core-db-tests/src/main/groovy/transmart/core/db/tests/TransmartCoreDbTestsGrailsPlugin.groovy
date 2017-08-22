package transmart.core.db.tests

import grails.plugins.Plugin
import org.transmartproject.db.test.DbDictionaryTablesLoader
import org.transmartproject.db.test.H2Views

/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
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
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

class TransmartCoreDbTestsGrailsPlugin extends Plugin {
    def grailsVersion = "3.1.10 > *"

    def title = "Transmart Core Db Tests Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
        The aim of this plugin is to reuse logic for populating db with test data.
        It also contains tests for core-db project to prevent circular
        plugin dependencies, which grails does not resolve.
    '''

    def documentation = "http://transmartproject.org"

    def scm = [url: "https://fisheye.ctmmtrait.nl/browse/transmart_core_db"]

    def developers = [
            [name: "Ruslan Forostianov", email: "ruslan@thehyve.nl"],
            [name: "Peter Kok", email: "peter@thehyve.nl"]
    ]

    @Override
    Closure doWithSpring() {
        return { ->
            h2Views(H2Views)
            dbDictionaryTablesLoader(DbDictionaryTablesLoader)
        }
    }
}
