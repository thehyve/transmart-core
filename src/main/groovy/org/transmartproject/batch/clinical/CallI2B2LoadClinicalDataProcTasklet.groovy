package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcCall

/**
 *
 */
class CallI2B2LoadClinicalDataProcTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['topNode']}")
    String topNode

    @Value("#{jobParameters['studyId']}")
    String studyId

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
        jdbcCall.schemaName = 'tm_cz'
        jdbcCall.procedureName = 'i2b2_load_clinical_data'

        Map<String,Object> dbResult = jdbcCall.execute(studyId, getTopNode(), 'N', 'N', System.currentTimeMillis())
        println "result is $dbResult"

        return RepeatStatus.FINISHED
    }

    String getTopNode() {
        String result = topNode
        if (!result) {
            result = "\\Public Studies\\$studyId\\"
        }
        result
    }

}
