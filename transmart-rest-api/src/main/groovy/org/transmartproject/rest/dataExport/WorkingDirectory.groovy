package org.transmartproject.rest.dataExport

import grails.util.Holders
import org.apache.commons.lang.StringUtils
import org.transmartproject.core.users.User

class WorkingDirectory {

    static final String tempFolderDirectory = Holders.config.com.recomdata.plugins.tempFolderDirectory

    static File forUser(User user) {
        String jobTmpDirectory
        if (StringUtils.isEmpty(tempFolderDirectory)) {
            jobTmpDirectory = '/var/tmp/jobs/'
        } else {
            jobTmpDirectory = tempFolderDirectory
        }
        String userDirectory = jobTmpDirectory + File.separator + user.username
        File fileDirectory = new File(userDirectory)

        if (!fileDirectory.isDirectory()) fileDirectory.mkdirs()
        fileDirectory
    }

    static File createDirectoryUser(User user, String prefix, String suffix = null) {
        def workingDir = File.createTempFile(prefix, suffix, forUser(user))
        workingDir.delete()
        workingDir.mkdirs()
        workingDir
    }

}
