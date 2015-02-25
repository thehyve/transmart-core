package org.transmartproject.batch.highdim.mrna.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.db.PostgresPartitionTasklet
import org.transmartproject.batch.highdim.assays.SaveAssayIdListener
import org.transmartproject.batch.highdim.compute.MeanAndVariancePromoter
import org.transmartproject.batch.highdim.platform.Platform
import org.transmartproject.batch.highdim.platform.PlatformCheckTasklet

/**
 * Account for values stored in the job execution context.
 */

@JobScope
@Component
class MrnaDataJobContextItems {

    @Value('#{jobExecution.executionContext}')
    ExecutionContext jobExecutionContext

    Double getCalculatedMean() {
        jobExecutionContext.getDouble(MeanAndVariancePromoter.CALCULATED_MEAN)
    }

    Double getCalculatedVariance() {
        jobExecutionContext.getDouble(MeanAndVariancePromoter.CALCULATED_VARIANCE)
    }

    Long getNumberOfDataPoints() {
        jobExecutionContext.getLong(MeanAndVariancePromoter.CALCULATED_N)
    }

    Integer getPartitionId() {
        jobExecutionContext.getInt(PostgresPartitionTasklet.PARTITION_ID_JOB_CONTEXT_KEY)
    }

    Map<String, Long> getPatientIdAssayIdMap() {
        jobExecutionContext.get(SaveAssayIdListener.MAPPINGS_CONTEXT_KEY)
    }

    Platform getPlatformObject() {
        jobExecutionContext.get(PlatformCheckTasklet.PLATFORM_OBJECT_CTX_KEY)
    }
}
