package org.transmartproject.notifications

import groovy.util.logging.Slf4j
import org.transmartproject.core.userquery.SubscriptionFrequency

/**
 * Quartz plugin job
 * See Grails Quartz Plugin documentation for more information:
 * http://grails-plugins.github.io/grails-quartz/latest/guide/
 */
@Slf4j
class QuerySetSubscriptionWeeklyJob {

    /**
     * Quartz plugin configuration option.
     * By default all jobs are considered enabled. Setting this property to false can disable the job.
     *
     * Value of this option is controlled in the notifications plugin descriptor.
     * @see {@link org.transmartproject.notifications.TransmartNotificationsGrailsPlugin}
     */
    static jobEnabled

    QuerySetSubscriptionMailService querySetSubscriptionMailService
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
        cron name: 'weeklySubscriptionTrigger', cronExpression: "0 0 1 ? * MON"
    }

    /**
     * A short description of the job
     */
    def description = "Weekly job to check for user query data updates."

    /**
     * Runs generating emails
     */
    void execute() {
        querySetSubscriptionMailService.run(SubscriptionFrequency.WEEKLY)
        // todo add a new row to AsyncJob table (?)
        log.info "Weekly subscription job executed."
    }
}
