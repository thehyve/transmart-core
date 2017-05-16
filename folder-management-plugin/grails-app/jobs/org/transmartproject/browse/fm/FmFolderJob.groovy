package org.transmartproject.browse.fm

import grails.util.Holders

class FmFolderJob {

    def fmFolderService

    static triggers = {
        if(!Holders.config.org.tarnsmartproject.mongoFiles.enableMongo){
            def startDelay = Holders.config.com.recomdata.FmFolderJob.startDelayMs
            def cronExpression = Holders.config.com.recomdata.FmFolderJob.cronExpression

            if (startDelay instanceof String) {
                try {
                    startDelay = Integer.parseInt(startDelay)
                } catch (NumberFormatException nfe) {
                    log.error("Folder job not initialized. Configuration not readable")
                }
            } else {
                startDelay = null
            }
            cron name: 'FmFolderJobTrigger',
                    cronExpression: (cronExpression instanceof String) ? cronExpression : '0 0/5 * * * ?',
                    startDelay: startDelay != null ? startDelay : 60000
    
        }
    }

    def execute() {
        fmFolderService.importFiles();
    }
}
