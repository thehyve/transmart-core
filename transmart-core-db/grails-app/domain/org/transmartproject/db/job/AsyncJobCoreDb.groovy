/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.job

import groovy.time.TimeCategory
import groovy.time.TimeDuration

class AsyncJobCoreDb {

    final Set<String> TERMINATION_STATES = ['Completed', 'Cancelled', 'Error'] as Set

    String jobName
    String jobStatus
    Date lastRunOn
    Date jobStatusTime
    String viewerURL
    String altViewerURL
    String results
    String jobType
    String jobInputsJson
    String userId

    static mapping = {
        id      generator: 'sequence',
                params:     [sequence: 'hibernate_sequence', schema: 'searchapp']
        table   'I2B2DEMODATA.ASYNC_JOB'
        version false

        jobName         column: 'JOB_NAME'
        jobStatus       column: 'JOB_STATUS'
        lastRunOn       column: 'LAST_RUN_ON'
        jobStatusTime   column: 'JOB_STATUS_TIME'
        viewerURL       column: 'VIEWER_URL'
        altViewerURL    column: 'ALT_VIEWER_URL'
        results         column: 'JOB_RESULTS'
        jobType         column: 'JOB_TYPE'
        jobInputsJson   column: 'JOB_INPUTS_JSON'
        userId          column: 'USER_ID'
    }

    static constraints = {
        jobName         nullable: true
        jobStatus       nullable: true
        lastRunOn       nullable: true
        jobStatusTime   nullable: true
        viewerURL       nullable: true
        altViewerURL    nullable: true
        results         nullable: true
        jobType         nullable: true
        jobInputsJson   nullable: true
        userId          nullable: true
    }

    TimeDuration getRunTime() {
        def lastTime = TERMINATION_STATES.contains(jobStatus) ?
                jobStatusTime : new Date()
        lastRunOn && lastTime ? TimeCategory.minus(lastTime, lastRunOn) : null
    }

    void setJobStatus(String jobStatus) {
        if (this.jobStatus == jobStatus) {
            return
        }

        this.jobStatusTime = new Date()
        this.jobStatus = jobStatus
    }
}
