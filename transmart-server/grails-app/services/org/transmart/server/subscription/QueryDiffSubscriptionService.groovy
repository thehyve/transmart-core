package org.transmart.server.subscription

import grails.plugins.mail.MailService
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmart.server.subsctiption.ChangeFlag
import org.transmart.server.subsctiption.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQueryDiff
import org.transmartproject.core.userquery.UserQueryDiffResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

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
class QueryDiffSubscriptionService {

    MailService mailService

    @Autowired
    UsersResource usersResource

    @Autowired
    UserQueryDiffResource userQueryDiffResource

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
            sendEmail(user.email, title, emailBody)
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
        // todo pagination params
        int firstResult = 0
        int numResults = 10

        def currentDate = new Date()


        List<UserQueryDiff> queryDiffs = userQueryDiffResource.getByFrequency(frequency.value(), username,
                firstResult, numResults)
        StringBuilder textStringBuilder = new StringBuilder()

        textStringBuilder.append("Change log of ${currentDate.format("d.' of 'M Y h:mm aa z")}")

        for(diff in queryDiffs) {
            //todo prettify a message text
            def addedIds = diff.queryDiffEntries.collect {
                it.changeFlag == ChangeFlag.ADDED.value()
            }?.toListString()
            def removedIds = diff.queryDiffEntries.collect {
                it.changeFlag == ChangeFlag.REMOVED.value()
            }?.toListString()
            textStringBuilder.append(NEW_LINE)
            textStringBuilder.append("For query $diff.id: \n" +
                    "date of the change: + $diff.date, \n" +
                    "added ids: $addedIds \n" +
                    "removed ids: $removedIds \n")
            textStringBuilder.append(NEW_LINE)
        }
        return textStringBuilder.toString()
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
