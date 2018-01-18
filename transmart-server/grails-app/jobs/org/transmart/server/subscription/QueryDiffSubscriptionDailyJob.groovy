package org.transmart.server.subscription

import groovy.util.logging.Slf4j
import org.transmartproject.db.userqueries.SubscriptionFrequency

/**
 * Quartz plugin job for user query subscription
 * See Grails Quartz Plugin documentation for more information:
 * http://grails-plugins.github.io/grails-quartz/latest/guide/
 */
@Slf4j
class QueryDiffSubscriptionDailyJob {

    QueryDiffSubscriptionMailService queryDiffSubscriptionMailService
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
        cron name: 'dailySubscriptionTrigger', cronExpression: " 0 0 0 1/1 * ?"
        // cron name: 'dailySubscriptionTestTrigger', cronExpression: "0 0/1 * 1/1 * ?"
    }

    /**
     * A short description of the job
     */
    def description = "Daily job to check for user query data updates."

    /**
     * Runs generating emails
     */
    void execute() {
        queryDiffSubscriptionMailService.run(SubscriptionFrequency.DAILY)
        // todo add a new row to AsyncJob table (?)
        log.info "Daily subscription job executed."
    }
}
