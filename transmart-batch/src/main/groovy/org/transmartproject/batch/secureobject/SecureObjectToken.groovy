package org.transmartproject.batch.secureobject

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

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobParameters['SECURITY_REQUIRED']}")
    String securityRequired // should be Y or N

    @PostConstruct
    void checkSecurityRequiredValue() {
        if (securityRequired != 'Y' && securityRequired != 'N') {
            throw new IllegalArgumentException(
                    "Unexpected SECURITY_REQUIRED value: $securityRequired")
        }
        Assert.notNull(studyId, "Study id not given")
    }

    String toString() {
        securityRequired == 'Y' ? "EXP:$studyId" : 'EXP:PUBLIC'
    }
}
