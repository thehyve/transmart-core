package org.transmartproject.notifications

import grails.util.Holders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.ServiceNotAvailableException
import org.transmartproject.core.userquery.SubscriptionFrequency

class NotificationsMailController {

    @Autowired
    QuerySetSubscriptionMailService querySetSubscriptionMailService

    /**
     * GET /v2/admin/notifications/notify?frequency=DAILY|WEEKLY
     *
     *
     * Sends emails to users who subscribed for a user query updates.
     *
     * @param frequency specifies whether the mail should be send to users
     *        who subscribed for DAILY or WEEKLY subscription.
     * @return {@link HttpStatus#OK};
     *      or {@link HttpStatus#FORBIDDEN} when the user does not have an ADMIN role,
     *      see request matcher configuration of the current application.
     * @throws {@link InvalidArgumentsException} if frequency parameter is not valid.
     * @throws {@link ServiceNotAvailableException} if endpointEnabled parameter is disabled in the configuration.
     */
    def notificationsNotify(@RequestParam('frequency')String frequency) {
        if (!Holders.config.org.transmartproject.notifications.endpointEnabled) {
            throw new ServiceNotAvailableException( "This endpoint is not enabled.")
        }
        SubscriptionFrequency subscriptionFrequency = frequency?.trim() ? SubscriptionFrequency.forName(frequency) : null
        if (!subscriptionFrequency) {
            throw new InvalidArgumentsException("Invalid frequency parameter: $frequency")
        }
        querySetSubscriptionMailService.run(subscriptionFrequency)
        response.status = HttpStatus.OK.value()
    }

}
