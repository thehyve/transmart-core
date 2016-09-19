package org.transmartproject.batch.i2b2.misc

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import javax.annotation.PostConstruct

/**
 * Helper to generate values for the i2b2 control columns.
 */
@Component
@JobScope
class I2b2ControlColumnsHelper {

    @Autowired
    private DateConverter dateConverter

    @Value("#{jobParameters['SOURCE_SYSTEM']}")
    private String sourceSystem

    @Value("#{jobParameters['DOWNLOAD_DATE']}")
    private String downloadDateString

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Date downloadDate = downloadDateString ?
            dateConverter?.parse(downloadDateString) : null

    @Value('#{jobExecution.startTime}')
    private Date jobStartTime

    @Value('#{jobExecution.id}')
    private Long jobExecutionId

    @PostConstruct
    void init() {
        Assert.notNull(sourceSystem)
        Assert.notNull(jobStartTime)
        Assert.notNull(jobExecutionId)
    }

    @Lazy
    Map<String, Object> controlValues = createControlValues(null)

    Map<String, Object> createControlValues(Date sourceUpdateDate) {
        [
                update_date    : sourceUpdateDate,
                download_date  : downloadDate,
                import_date    : jobStartTime,
                sourcesystem_cd: sourceSystem,
                upload_id      : jobExecutionId,
        ]
    }
}
