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
import org.transmartproject.db.dataquery.clinical.variables.ClinicalVariableFactory
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.http.BusinessExceptionResolver
import org.transmartproject.db.ontology.AcrossTrialsConceptsResourceDecorator
import org.transmartproject.db.ontology.DefaultConceptsResource
import org.transmartproject.db.support.DatabasePortabilityService

class TransmartCoreGrailsPlugin {
    // the plugin version
    def version = "16.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3 > *"
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

    // URL to the plugin's documentation
    def documentation = "http://transmartproject.org"

    def license = "GPL3"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [
            [ name: "Kees van Bochove",  email: "kees@thehyve.nl"],
            [ name: "Gustavo Lopes"   ,  email: "gustavo@thehyve.nl" ],
    ]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://fisheye.ctmmtrait.nl/browse/transmart_core_db" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        xmlns context:"http://www.springframework.org/schema/context"

        def config = application.config

        /* unless explicitly disabled, enable across trials functionality */
        def haveAcrossTrials =
                config.org.transmartproject.enableAcrossTrials != false

        businessExceptionResolver(BusinessExceptionResolver)

        clinicalVariableFactory(ClinicalVariableFactory) {
            disableAcrossTrials = !haveAcrossTrials
        }

        if (haveAcrossTrials) {
            conceptsResourceService(AcrossTrialsConceptsResourceDecorator) {
                inner = new DefaultConceptsResource()
            }
        } else {
            conceptsResourceService(DefaultConceptsResource)
        }

        context.'component-scan'('base-package': 'org.transmartproject.db.dataquery.highdim') {
            context.'include-filter'(
                    type:       'assignable',
                    expression: AbstractHighDimensionDataTypeModule.canonicalName)
        }

        context.'component-scan'('base-package': 'org.transmartproject.db') {
            context.'include-filter'(
                    type:       'annotation',
                    expression: Component.canonicalName)
        }

        if (!config.org.transmartproject.i2b2.user_id) {
            config.org.transmartproject.i2b2.user_id = 'i2b2'
        }
        if (!config.org.transmartproject.i2b2.group_id) {
            config.org.transmartproject.i2b2.group_id = 'Demo'
        }
    }

    def doWithDynamicMethods = { ctx ->
        String.metaClass.asLikeLiteral = { replaceAll(/[\\%_]/, '\\\\$0') }

        /* Force this bean to be initialized, as it has some dynamic methods
         * to register during its init() method */
        ctx.getBean DatabasePortabilityService
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
