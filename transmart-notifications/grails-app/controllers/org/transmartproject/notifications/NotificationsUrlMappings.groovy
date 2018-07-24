package org.transmartproject.notifications

class NotificationsUrlMappings {

    static mappings = {
        group "/v2", {
            group "/admin", {
                "/subscription/notify"(method: 'GET',controller: 'subscriptionMail', action: 'subscriptionNotify')
            }
        }
    }
}
