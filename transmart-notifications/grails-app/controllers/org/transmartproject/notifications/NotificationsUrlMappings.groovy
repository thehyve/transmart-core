package org.transmartproject.notifications

class NotificationsUrlMappings {

    static mappings = {
        group "/v2", {
            group "/admin", {
                "/notifications/notify"(method: 'GET',controller: 'notificationsMail', action: 'notificationsNotify')
            }
        }
    }
}
