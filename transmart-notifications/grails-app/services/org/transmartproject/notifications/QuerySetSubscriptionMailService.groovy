package org.transmartproject.notifications

import grails.plugins.mail.MailService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.core.userquery.UserQuerySetChangesRepresentation
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.core.userquery.SetType

/**
 * Generates and sends a daily or weekly subscription email for each user
 * that subscribed for a query data result updates.
 *
 * The DSL for sending emails is provided by the Grails mail plugin.
 * See Grails Mail Plugin documentation for more information:
 * http://gpc.github.io/grails-mail/
 *
 */
@Slf4j
@CompileStatic
class QuerySetSubscriptionMailService {

    @Value('${org.transmartproject.notifications.maxNumberOfSets}')
    Integer maxNumberOfSets

    @Value('${org.transmartproject.notifications.clientApplicationName}')
    String clientApplicationName

    MailService mailService

    @Autowired
    UsersResource usersResource

    @Autowired
    UserQuerySetResource userQueryDiffResource

    /**
     * Creates and sends a daily or weekly email for each subscribed user having an email specified.
     *
     * The email is sent only when there are changes for one or more of the results of queries the user subscribed for.
     * A query result is changed when there is an object ID in the result that wasn't there the last time it was run,
     * or when an object ID isn't there which was there the last time it was run.
     *
     */
    def run(SubscriptionFrequency frequency) {
        List<User> users = usersResource.getUsersWithEmailSpecified()
        Date reportDate = new Date()
        for (user in users) {
            List<UserQuerySetChangesRepresentation> patientSetChanges =
                    getPatientSetChangesRepresentation(frequency, user.username)

            if (patientSetChanges.size() > 0) {
                String emailSubject = EmailGenerator.getQuerySubscriptionUpdatesSubject(clientApplicationName, reportDate)
                String emailBodyHtml = EmailGenerator.getQuerySubscriptionUpdatesBody(patientSetChanges, clientApplicationName, reportDate)
                sendEmail(user.email, emailSubject, emailBodyHtml)
            }
        }
    }



    /**
     * Fetches a list of patient sets with changes made comparing to a previous set related to the same query
     *
     * @param frequency
     * @param username
     * @return A list of patient sets with changes
     */
    private List<UserQuerySetChangesRepresentation> getPatientSetChangesRepresentation(SubscriptionFrequency frequency,
                                                                                       String username) {
        List<UserQuerySetChangesRepresentation> querySetsChanges =
                userQueryDiffResource.getQueryChangeHistoryByUsernameAndFrequency(frequency, username, maxNumberOfSets)
        return querySetsChanges.findAll { it.setType == SetType.PATIENT }?.sort { it.queryId }
    }

    /**
     * Sends an email to a specific recipient from an email account that is specified in the application config file,
     * using Grails Mail Plugin
     *
     * @param address - a receiver address
     * @param title - an email subject
     * @param emailBodyHtml - html to send as an email body
     */
    private void sendEmail(String address, String title, String emailBodyHtml) {

        mailService.sendMail {
            to address
            html emailBodyHtml
            subject(title)
        }
    }
}
