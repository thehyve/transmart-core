package org.transmart.notifications

import grails.util.Holders
import groovy.util.logging.Slf4j
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException
import org.transmartproject.core.userquery.SubscriptionFrequency

/**
 * Quartz plugin job for user query subscription
 * See Grails Quartz Plugin documentation for more information:
 * http://grails-plugins.github.io/grails-quartz/latest/guide/
 */
@Slf4j
class QuerySetSubscriptionDailyJob {

    static jobEnabled =  Holders.config.org.transmart.notifications.enabled

    QuerySetSubscriptionMailService querySetSubscriptionMailService

    /**
     * Parse org.transmart.notifications.dailyJobTriggerTime setting specified in the configuration
     * Format of the setting is hh-mm, which represent hour and minutes, separated by '-'.
     * Hours and minutes have to be numbers from a proper range (0-23 and 0-59 respectively).
     * Specified setting of hour and minutes is changed to a proper cron expression that will be triggered daily.
     * If the specified setting is invalid, the default one will be set, which is 00:00 every day.
     *
     * @return valid cron expression for a daily job
     */
    private static String parseConfiguredTriggerTime() {
        try {
            def (hour, minute) = Holders.config.org.transmart.notifications.dailyJobTriggerTime.tokenize('-')
            def validMinute = minute as Integer
            def validHour = hour as Integer
            if (validMinute >= 0 && validMinute <= 59 && validHour >= 0 && validHour <= 23){
                return " 0 $minute $hour 1/1 * ?"
            } else {
                throw new InvalidConfigurationException('Property out of allowed range.')
            }
        } catch (Exception e) {
            throw new InvalidConfigurationException(
                    "Invalid trigger time setting in transmart-notifications configuration. $e.message")
        }
    }

    /**
     * Specifies a cron expression for job trigger
         cronExpression: "s m h D M W Y"
             Second, 0-59;
             Minute, 0-59;
             Hour, 0-23;
             Day of Month, 1-31, ?;
             Month, 1-12 or JAN-DEC;
             Day of Week, 1-7 or SUN-SAT, ?;
             Year [optional]
      Either Day-of-Week or Day-of-Month must be "?".
      Can't specify both fields, nor leave both as the all values wildcard "*"
     **/
    static triggers = {
        cron name: 'dailySubscriptionTrigger', cronExpression: parseConfiguredTriggerTime()
    }

    /**
     * A short description of the job
     */
    def description = "Daily job to check for user query data updates."

    /**
     * Runs generating emails
     */
    void execute() {
        querySetSubscriptionMailService.run(SubscriptionFrequency.DAILY)
        // todo add a new row to AsyncJob table (?)
        log.info "Daily subscription job executed."
    }
}
