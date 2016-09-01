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


import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.springframework.stereotype.Component
import org.transmartproject.rest.marshallers.MarshallersRegistrar
import org.transmartproject.rest.marshallers.TransmartRendererRegistry

class TransmartRestApiGrailsPlugin {
    def version = "16.2-SNAPSHOT"
    def grailsVersion = "3.1.10 > *"
    def title = "Transmart Rest Api Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
        Plugin adds rest api to transmart applicaion
    '''

    def documentation = "https://wiki.thehyve.nl/"

    def organization = [name: "The Hyve", url: "http://www.thehyve.nl/"]

    def developers = [
            [name: "Ruslan Forostianov", email: "ruslan@thehyve.nl"],
            [name: "Jan Kanis", email: "jan@thehyve.nl"],
    ]

    def issueManagement = [system: "JIRA", url: "https://jira.thehyve.nl/browse/CHERKASY"]

    def scm = [url: "https://fisheye.ctmmtrait.nl/browse/transmart_rest_api"]

    def doWithSpring = {
        xmlns context: 'http://www.springframework.org/schema/context'

        context.'component-scan'('base-package': 'org.transmartproject.rest') {
            context.'include-filter'(
                    type: 'annotation',
                    expression: Component.canonicalName)
        }

        studyLoadingServiceProxy(ScopedProxyFactoryBean) {
            targetBeanName = 'studyLoadingService'
        }

        marshallersRegistrar(MarshallersRegistrar) {
            packageName = 'org.transmartproject.rest.marshallers'
        }

        // override bean
        rendererRegistry(TransmartRendererRegistry) { bean ->
            modelSuffix = application.flatConfig.get('grails.scaffolding.templates.domainSuffix') ?: ''
        }
    }

    def doWithApplicationContext = { ctx ->
        // Force the bean being initialized
        ctx.getBean 'marshallersRegistrar'
    }
}
