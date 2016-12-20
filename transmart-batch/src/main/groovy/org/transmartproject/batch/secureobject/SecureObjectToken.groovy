package org.transmartproject.batch.secureobject

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Whatever tranSMART calls a "secure object token". In practice, PUBLIC or
 * EXP: plus a study id.
 */
@Component
@JobScope
class SecureObjectToken {

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobParameters['SECURITY_REQUIRED']}")
    String securityRequired // should be Y or N

    Long experimentId

    boolean isPublic() {
        securityRequired != 'Y'
    }

    String toString() {
        this.public ? 'PUBLIC' : "EXP:$studyId"
    }
}
