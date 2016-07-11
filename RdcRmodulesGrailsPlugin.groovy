import com.google.common.collect.ImmutableMap
import jobs.misc.AnalysisQuartzJobAdapter
import org.springframework.beans.factory.config.CustomScopeConfigurer
import org.springframework.stereotype.Component
import org.transmartproject.core.users.User

/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

class RdcRmodulesGrailsPlugin {
    // the plugin version
    def version = "16.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Sai Kumar Munikuntla"
    def authorEmail = "smunikuntla@recomdata.com"
    def title = "R Modules Plugin"
    def description = '''\\
Brief description of the plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/rmodules"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        xmlns context: 'http://www.springframework.org/schema/context'

        context.'component-scan'('base-package': 'jobs') {
            context.'include-filter'(
                    type:       'annotation',
                    expression: Component.canonicalName)
        }

        jobScopeConfigurer(CustomScopeConfigurer) {
            scopes = ImmutableMap.of('job', ref('jobSpringScope'))
        }

        /* these beans are actually created manually and put
         * in the storage for the job scope */
        jobName(String) { bean ->
            bean.scope = 'job'
        }
        "${AnalysisQuartzJobAdapter.BEAN_USER_IN_CONTEXT}"(User) { bean ->
            bean.scope = 'job'
        }

        /*
         * Prevent the resource plugin from handling the resources in this
         * so that it can be served directly.
         */
        def curExcludes = application.config.grails.resources.adhoc.excludes
        application.config.grails.resources.adhoc.excludes =
                ['/analysisFiles/**', '/images/analysisFiles/**'] +
                        (curExcludes ?: [])
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // currentUserBean is a tranSMART bean
        def currentUserBean = applicationContext.getBean('&currentUserBean')
        if (!currentUserBean) {
            throw new IllegalStateException("The context doesn't provide the " +
                    "bean currentUserBean. Most likely, you're using an " +
                    "incompatible transmartApp")
        }

        // allow currentUserBean to be able to find the current user inside
        // the jobs (quartz) threads
        currentUserBean.registerBeanToTry(AnalysisQuartzJobAdapter.BEAN_USER_IN_CONTEXT)
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
}
