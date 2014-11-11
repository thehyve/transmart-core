package org.transmartproject.batch.support

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import javax.annotation.PostConstruct

/**
 * Whatever tranSMART calls a "secure object token". In practice, EXP: plus
 * either PUBLIC or a study id.
 */
@Component
@JobScope
class SecureObjectToken {

    @Value("jobContext['STUDY_ID']")
    String studyId

    @Value("jobContext['SECURITY_REQUIRED']")
    String securityRequired // should be Y or N

    @PostConstruct
    void checkSecurityRequiredValue() {
        if (securityRequired != 'Y' && securityRequired != 'N') {
            throw new IllegalArgumentException(
                    "Expected security required: $securityRequired")
        }
        Assert.notNull(studyId, "Study id not given")
    }

    String toString() {
        securityRequired == 'Y' ? 'EXP:PUBLIC' : "EXP:$studyId"
    }
}
