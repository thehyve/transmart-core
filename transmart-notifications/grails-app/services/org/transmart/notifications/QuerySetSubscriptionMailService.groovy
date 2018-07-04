package org.transmart.notifications

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

    @Value('${org.transmart.notifications.maxNumberOfSets}')
    Integer maxNumberOfSets

    MailService mailService

    @Autowired
    UsersResource usersResource

    @Autowired
    UserQuerySetResource userQueryDiffResource

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

        List<GString> queryResultsList = new ArrayList<>()
        int i = 0
        for (setChange in patientSetsChanges) {
            queryResultsList.add("""
                ${ i == 0 || setChange.queryId == patientSetsChanges.get(i - 1).queryId ?
                    "For a query named: '$setChange.queryName' (id='$setChange.queryId')" : ""
                }
                date of the change: $setChange.createDate
                ${ setChange.objectsAdded.size() > 0 ?
                    "added patients with ids: $setChange.objectsAdded" : ""
                }
                ${ setChange.objectsRemoved.size() > 0 ?
                    "removed patients with ids: $setChange.objectsRemoved" : ""
                }
            """)
            i++
        }
        def emailBody = """
            Generated as per day ${currentDate.format("d.' of 'MMMM Y h:mm aa z")}
            
            List of updated query results:
            $queryResultsList
            
        """
        return emailBody
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
