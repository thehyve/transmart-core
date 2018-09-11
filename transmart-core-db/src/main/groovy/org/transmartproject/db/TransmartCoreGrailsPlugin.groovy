package org.transmartproject.db

import grails.plugins.Plugin
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

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

import org.springframework.stereotype.Component
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery.clinical.InnerClinicalTabularResultFactory
import org.transmartproject.db.dataquery.clinical.variables.ClinicalVariableFactory
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.ontology.AcrossTrialsConceptsResourceDecorator
import org.transmartproject.db.ontology.DefaultOntologyTermsResource
import org.transmartproject.db.support.DatabasePortabilityService
import org.transmartproject.db.user.UsersResourceService

class TransmartCoreGrailsPlugin extends Plugin {
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.10 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Transmart Core DB Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
A runtime dependency for tranSMART that implements the Core API
'''

    def license = "GPL3"

    // Details of company behind the plugin (if there is one)
    def organization = [name: "The Hyve", url: "http://www.thehyve.nl"]

    // Any additional developers beyond the author specified above.
    def developers = [
            [ name: "Kees van Bochove",  email: "kees@thehyve.nl"],
            [ name: "Gustavo Lopes"   ,  email: "gustavo@thehyve.nl" ],
    ]

    def scm = [url: "https://github.com/thehyve/transmart-core"]
    def documentation = "https://github.com/thehyve/transmart-core"

    Closure doWithSpring() {{->
        xmlns context:"http://www.springframework.org/schema/context"

        def config = application.config

        /* unless explicitly disabled, enable across trials functionality */
        def haveAcrossTrials =
                config.org.transmartproject.enableAcrossTrials != false

        accessControlChecks(AccessControlChecks)
        userResource(UsersResourceService)

        clinicalVariableFactory(ClinicalVariableFactory) {
            disableAcrossTrials = !haveAcrossTrials
        }
        innerClinicalTabularResultFactory(InnerClinicalTabularResultFactory)

        if (haveAcrossTrials) {
            ontologyTermsResourceService(AcrossTrialsConceptsResourceDecorator) {
                inner = new DefaultOntologyTermsResource()
            }
        } else {
            ontologyTermsResourceService(DefaultOntologyTermsResource)
        }

        context.'component-scan'('base-package': 'org.transmartproject.db.dataquery.highdim') {
            context.'include-filter'(
                    type:       'assignable',
                    expression: AbstractHighDimensionDataTypeModule.canonicalName)
        }

        context.'component-scan'('base-package': 'org.transmartproject.db.dataquery.highdim') {
            context.'include-filter'(
                    type:       'annotation',
                    expression: Component.canonicalName)
        }

        context.'component-scan'('base-package': 'org.transmartproject.export.Tasks') {
            context.'include-filter'(
                    type:       'annotation',
                    expression: Component.canonicalName)
        }

        context.'component-scan'('base-package': 'org.transmartproject.db.config')

        if (!config.org.transmartproject.i2b2.user_id) {
            config.org.transmartproject.i2b2.user_id = 'i2b2'
        }
        if (!config.org.transmartproject.i2b2.group_id) {
            config.org.transmartproject.i2b2.group_id = 'Demo'
        }

        namedParameterJdbcTemplate(NamedParameterJdbcTemplate, ref('dataSource'))

    }}

    void doWithDynamicMethods() {
        /* Force this bean to be initialized, as it has some dynamic methods
         * to register during its init() method */
        applicationContext.getBean DatabasePortabilityService
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
