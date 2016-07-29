package org.transmartproject.batch.secureobject

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.concept.ConceptPath

/**
 * If the study is private, insert the necessary data into bio_experiment and
 * search_secure_object.
 */
@Component
@JobScopeInterfaced
@Slf4j
class CreateSecureStudyTasklet implements Tasklet {

    @Autowired
    SecureObjectToken secureObjectToken

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    SecureObjectDAO secureObjectDAO

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {

        if (secureObjectToken.toString() == 'EXP:PUBLIC') {
            log.info("Study is public; will not take any action " +
                    "(existing secure objects will NOT be deleted)")
            return RepeatStatus.FINISHED
        }

        secureObjectDAO.createSecureObject(
                displayName, secureObjectToken)
    }

    private String getDisplayName() {
        if (topNodePath.length >= 2) {
            /* Not great logic, but it's what the stored procedures currently have */
            "${topNodePath[0]} - $studyId"
        } else {
            studyId
        }
    }


}
