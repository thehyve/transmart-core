package org.transmartproject.rest

import grails.plugins.Plugin
import grails.util.Environment
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.springframework.stereotype.Component

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
import org.transmartproject.rest.http.BusinessExceptionResolver
import org.transmartproject.rest.marshallers.MarshallersRegistrar
import org.transmartproject.rest.marshallers.TransmartRendererRegistry
import org.transmartproject.rest.misc.HandleAllExceptionsBeanFactoryPostProcessor
import org.transmartproject.rest.user.SpringSecurityAuthContext

class TransmartRestApiGrailsPlugin extends Plugin {
    def grailsVersion = "3.1.10 > *"

    def title = "Transmart Rest Api Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
REST API for Transmart
'''

    def license = "GPL3"

    def organization = [name: "The Hyve", url: "http://www.thehyve.nl"]
    // For a complete overview of developers, see contributors.md.
    def developers = [
        [name: "Ruslan Forostianov", email: "ruslan@thehyve.nl"],
        [name: "Jan Kanis", email: "jan@thehyve.nl"],
    ]

    def scm = [url: "https://github.com/thehyve/transmart-core"]
    def documentation = "https://github.com/thehyve/transmart-core"
    def issueManagement = [system: "JIRA", url: "https://jira.thehyve.nl/browse/CHERKASY"]

    def loadAfter = ['restResponder']

    @Override
    Closure doWithSpring() {{ ->
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
        rendererRegistry(TransmartRendererRegistry)

        businessExceptionController(BusinessExceptionController)

        businessExceptionResolver(BusinessExceptionResolver)

        handleAllExceptionsBeanFactoryPostProcessor(HandleAllExceptionsBeanFactoryPostProcessor)
    }}

    @Override
    void doWithApplicationContext() {
        // Force the bean being initialized
        applicationContext.getBean 'marshallersRegistrar'
    }
}
