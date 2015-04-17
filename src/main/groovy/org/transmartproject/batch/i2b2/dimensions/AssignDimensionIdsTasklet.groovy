package org.transmartproject.batch.i2b2.dimensions

import com.google.common.collect.ImmutableMap
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.db.SequenceReserver

import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Look in the {@link DimensionsStore} for dimension objects without ids and
 * assigned some to them.
 */
@Component
@Slf4j
class AssignDimensionIdsTasklet implements Tasklet {

    private final Map<String /* dimension key */, String /* sequences */> sequences =
            ImmutableMap.of(
                    PATIENT_DIMENSION_KEY, Sequences.PATIENT,
                    VISITS_DIMENSION_KEY, Sequences.VISIT,)

    @Autowired
    private DimensionsStore dimensionsStore

    @Autowired
    private SequenceReserver sequenceReserver

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        sequences.each { String dimensionKey, String sequence ->
            log.info "Assigning keys for dimensions of type $dimensionKey"

            def externalIdIterator = dimensionsStore
                    .getExternalIdsWithoutAssociatedInternalIds(dimensionKey)
            int count = 0
            externalIdIterator.each { externalId ->
                def sequenceValue = sequenceReserver.getNext(sequence)
                if (log.traceEnabled) {
                    log.trace("Will assign internal id $sequenceValue to " +
                            "$dimensionKey/$externalId")
                }
                dimensionsStore.assignInternalId(
                        dimensionKey,
                        externalId,
                        sequenceValue as String)
                count++
            }
            contribution.incrementWriteCount(count)
        }

        RepeatStatus.FINISHED
    }
}
