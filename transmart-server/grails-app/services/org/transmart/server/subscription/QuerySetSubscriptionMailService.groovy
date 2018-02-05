package org.transmart.server.subscription

import grails.plugins.mail.MailService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.userquery.UserQuerySetDiff
import org.transmartproject.core.userquery.ChangeFlag
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
        List<User> users = getUsers()
        for (user in users) {
            String title = "Test email for query subscription"
            String emailBody = generateEmail(user.username, frequency)
            if(emailBody) {
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
    private String generateEmail(String username, SubscriptionFrequency frequency) {

        int firstResult = 0
        Integer numResults = grailsApplication.config.org.transmart.server.subscription.numResults

        def currentDate = new Date()

        List<UserQuerySetDiff> queryDiffEntries = userQueryDiffResource
                .getDiffEntriesByUsernameAndFrequency(frequency, username, firstResult, numResults)

        if(queryDiffEntries.size() > 0) {
            def queryDiffsMap = queryDiffEntries.groupBy { it.querySet }

            StringBuilder textStringBuilder = new StringBuilder()

            textStringBuilder.append("Generated as per day ${currentDate.format("d.' of 'MMMM Y h:mm aa z")}")
            textStringBuilder.append(NEW_LINE)
            textStringBuilder.append("List of updated query results:")
            textStringBuilder.append(NEW_LINE)

            def patientSetMap = queryDiffsMap.findAll{it.key.setType == SetType.PATIENT}
            if(patientSetMap.size() == 0) {
                return null
            }
            for (entry in patientSetMap) {
                def addedIds = entry.value.findAll {
                    it.changeFlag == ChangeFlag.ADDED
                }?.objectId
                def removedIds = entry.value.findAll {
                    it.changeFlag == ChangeFlag.REMOVED
                }?.objectId
                textStringBuilder.append(NEW_LINE)
                textStringBuilder.append("For a query named: '$entry.key.query.name' (id='$entry.key.query.id') \n" +
                        "date of the change: $entry.key.createDate \n")
                if(addedIds.size() > 0) {
                    textStringBuilder.append("added patients with ids: $addedIds \n")
                }
                if(removedIds.size() > 0) {
                    textStringBuilder.append("removed patients with ids: $removedIds \n")
                }
            }
            textStringBuilder.append(NEW_LINE)
            return textStringBuilder.toString()
        }
        return null
    }

    /**
     * Collects the list of all users with specified email address
     *
     * @return List of users
     *
     */
    private List<User> getUsers(){
        usersResource.getUsersWithEmailSpecified()
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
