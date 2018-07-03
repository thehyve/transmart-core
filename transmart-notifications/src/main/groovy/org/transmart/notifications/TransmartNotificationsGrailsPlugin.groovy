package org.transmart.notifications

import grails.plugins.*
import grails.util.Holders

class TransmartNotificationsGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.10 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "Transmart Notifications"
    def author = "The Hyve"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
A plugin that provides an email subscription functionality.
'''

    def license = "GPL3"

    def organization = [name: "The Hyve", url: "http://www.thehyve.nl"]

    def scm = [url: "https://github.com/thehyve/transmart-core"]
    def documentation = "https://github.com/thehyve/transmart-core"

    /**
     * Whether the notifications plugin is enabled
     * Depends on setting of the org.transmart.notifications.enabled property
     */
    boolean enabled = true

    Closure doWithSpring() { {->
        // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // enable/disable the plugin
        enabled = Holders.config.org.transmart.notifications.enabled
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
