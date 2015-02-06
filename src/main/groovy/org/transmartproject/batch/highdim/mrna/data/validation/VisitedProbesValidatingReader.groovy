package org.transmartproject.batch.highdim.mrna.data.validation

import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.*
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.ClassUtils
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

import javax.annotation.PostConstruct

/**
 * Validates that are no repeated probes and that all probes can be found.
 */
@Slf4j
class VisitedProbesValidatingReader extends ItemStreamSupport implements ItemStreamReader<FieldSet> {
    {
        name = ClassUtils.getShortName(getClass())
    }

    ItemReader<FieldSet> delegate

    @Value('#{stepExecution}')
    StepExecution stepExecution

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    private final static String SEEN_PROBES_KEY = 'seenProbes'

    private BiMap<String, Integer> probeToIndex

    // saved
    private BitSet seenProbeIndexes

    @PostConstruct
    void init() {
        assert delegate != null

        def builder = ImmutableBiMap.<String, Integer>builder()
        annotationEntityMap.annotationNames.eachWithIndex { entry, i ->
            builder.put entry, i
        }

        probeToIndex = builder.build()
    }

    @Override
    void update(ExecutionContext executionContext) {
        executionContext.put(
                getExecutionContextKey(SEEN_PROBES_KEY),
                seenProbeIndexes.clone())
    }

    @Override
    void open(ExecutionContext executionContext) {
        seenProbeIndexes = executionContext.get(
                getExecutionContextKey(SEEN_PROBES_KEY)) ?:
                new BitSet(annotationEntityMap.annotationNames.size())
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() {

        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.warn("Skipping final validation in order to " +
                    "avoid hiding earlier problems")
        } else {
            finalValidation()
        }
    }

    @SuppressWarnings('UnnecessarySemicolon') // codenarc bug
    private void finalValidation() {
        def missingProbes = [] as Set
        for (int i = seenProbeIndexes.nextClearBit(0);
             i < probeToIndex.size();
             i = seenProbeIndexes.nextClearBit(i)) {
            missingProbes << probeToIndex.inverse().get(i)
            i++
        }

        if (missingProbes) {
            throw new ValidationException('The set of seen probes is ' +
                    'smaller than that specified in the annotation file. ' +
                    "Missing probes: $missingProbes")
        }
    }

    @Override
    FieldSet read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        FieldSet fieldSet = delegate.read()
        if (fieldSet == null) {
            // Not sure if doing this here is the best idea.
            // A step execution listener could be more appropriate, but then
            // I don't think we'd be able to resume step execution. This way
            // we fail the last chunk
            finalValidation()
            return null // the end
        }

        if (fieldSet.fieldCount < 1) {
            throw new ParseException("Found line with no fields; fieldset is $fieldSet")
        }

        process fieldSet.readString(0)

        fieldSet
    }

    private void process(String probe) throws Exception {
        int probeIndex = probeToIndex[probe]
        if (probeIndex == null) {
            throw new ValidationException(
                    "Probe $probe is not in this platform")
        }

        boolean seen = seenProbeIndexes.get(probeIndex)
        if (seen) {
            throw new ValidationException("Repeated data for probe $probe")
        }
        seenProbeIndexes.set(probeIndex, true)
    }
}
