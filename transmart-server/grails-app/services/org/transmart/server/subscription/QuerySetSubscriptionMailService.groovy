package org.transmart.server.subscription

import grails.plugins.mail.MailService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
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
class QuerySetSubscriptionMailService {

    def grailsApplication

    MailService mailService

    @Autowired
    UsersResource usersResource

    @Autowired
    UserQuerySetResource userQueryDiffResource

    Integer maxNumberOfSets = grailsApplication.config.org.transmart.server.subscription.maxNumberOfSets

    private final static String NEW_LINE = "\n"

    /**
     * Creates and sends a daily or weekly email for each user having an email specified.
     *
     * The email is sent only when there are changes for one or more of the results of queries the user subscribed for.
     * A query result is changed when there is an object ID in the result that wasn't there the last time it was run,
     * or when an object ID isn't there which was there the last time it was run.
     *
     */
    def run(SubscriptionFrequency frequency) {
        List<User> users = usersResource.getUsersWithEmailSpecified()
        for (user in users) {
            List<UserQuerySetChangesRepresentation> patientSetChanges =
                    getPatientSetChangesRepresentation(frequency, user.username)

            if (patientSetChanges.size() > 0) {
                String emailBody = generateEmail(patientSetChanges)
                String title = "Test email for query subscription"
                sendEmail(user.email, title, emailBody)
            }
        }
    }

    /**
     * Generates an email for specific user with data updates for a query the user is subscribed for.
     *
     * The email contains a list of each query with changed results with:
     * - a name of the query,
     * - list of added and removed ids of objects that the query relates to
     * - over what period the change was
     *
     * @param username
     * @param freq
     * @return The body of the email
     *
     */
    private static String generateEmail(List<UserQuerySetChangesRepresentation> patientSetsChanges) {

        def currentDate = new Date()

        StringBuilder textStringBuilder = new StringBuilder()
        textStringBuilder.append("Generated as per day ${currentDate.format("d.' of 'MMMM Y h:mm aa z")}")
        textStringBuilder.append(NEW_LINE)
        textStringBuilder.append("List of updated query results:")
        textStringBuilder.append(NEW_LINE)

        int i = 0
        for (setChange in patientSetsChanges) {

            if (i == 0 || setChange.queryId == patientSetsChanges.get(i - 1).queryId) {
                textStringBuilder.append(NEW_LINE)
                textStringBuilder.append("For a query named: '$setChange.queryName' (id='$setChange.queryId') \n")
            }
            textStringBuilder.append("date of the change: $setChange.createDate \n")
            if (setChange.objectsAdded.size() > 0) {
                textStringBuilder.append("added patients with ids: $setChange.objectsAdded \n")
            }
            if (setChange.objectsRemoved.size() > 0) {
                textStringBuilder.append("removed patients with ids: $setChange.objectsRemoved \n")
            }
            i++
        }
        textStringBuilder.append(NEW_LINE)
        return textStringBuilder.toString()
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
     * @param title
     * @param emailBody
     */
    private void sendEmail(String address, String title, String emailBody) {

        mailService.sendMail {
            to address
            body emailBody
            subject(title)
        }
    }
}
