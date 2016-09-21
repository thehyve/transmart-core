package org.transmartproject.batch.highdim.datastd

import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.item.*
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.ClassUtils
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

import javax.annotation.PostConstruct

/**
 * Validates that are no unknown or repeated annotations and that all platform
 * annotations can be found.
 * The reason this is a reader is that it needs to look at the rows before
 * they are split into {@link DataPoint}s.
 */
@Slf4j
class VisitedAnnotationsReadingValidator extends ItemStreamSupport
        implements ItemStreamReader<FieldSet>, StepExecutionListener {

    {
        name = ClassUtils.getShortName(getClass())
    }

    ItemReader<FieldSet> delegate

    @Value('#{stepExecution}')
    StepExecution stepExecution

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    private boolean allowMissingAnnotations // see related setter below

    private final static String SEEN_ANNOTATIONS_KEY = 'seenAnnotations'

    private BiMap<String, Integer> annotationToIndex

    // saved
    private BitSet seenAnnotationIndexes

    @PostConstruct
    void init() {
        assert delegate != null

        def builder = ImmutableBiMap.<String, Integer> builder()
        annotationEntityMap.annotationNames.eachWithIndex { entry, i ->
            builder.put entry, i
        }

        annotationToIndex = builder.build()
    }

    @Value("#{jobParameters['ALLOW_MISSING_ANNOTATIONS']}")
    void setAllowMissingAnnotations(String value) {
        allowMissingAnnotations = value == 'Y'
    }

    @Override
    void update(ExecutionContext executionContext) {
        executionContext.put(
                getExecutionContextKey(SEEN_ANNOTATIONS_KEY),
                seenAnnotationIndexes.clone())
    }

    @Override
    void open(ExecutionContext executionContext) {
        seenAnnotationIndexes = executionContext.get(
                getExecutionContextKey(SEEN_ANNOTATIONS_KEY)) ?:
                new BitSet(annotationEntityMap.annotationNames.size())
    }

    @SuppressWarnings('UnnecessarySemicolon') // codenarc bug
    private void finalValidation() {
        def missingAnnotations = [] as Set
        for (int i = seenAnnotationIndexes.nextClearBit(0);
             i < annotationToIndex.size();
             i = seenAnnotationIndexes.nextClearBit(i)) {
            missingAnnotations << annotationToIndex.inverse().get(i)
            i++
        }

        if (missingAnnotations) {
            def msg = 'The set of seen annotations is ' +
                    'smaller than that specified in the annotation file. ' +
                    "Missing annotations: $missingAnnotations"

            if (allowMissingAnnotations) {
                log.info(msg)
            } else {
                throw new ValidationException(msg)
            }
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

    private void process(String annotation) throws Exception {
        Integer annotationIndex = annotationToIndex[annotation]
        if (annotationIndex == null) {
            throw new ValidationException(
                    "Annotation $annotation is not in this platform")
        }

        boolean seen = seenAnnotationIndexes.get(annotationIndex)
        if (seen) {
            throw new ValidationException("Repeated data for annotation $annotation")
        }
        seenAnnotationIndexes.set(annotationIndex, true)
    }

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.warn("Skipping final validation in order to " +
                    "avoid hiding earlier problems")
            stepExecution.exitStatus
        } else {
            try {
                finalValidation()
                stepExecution.exitStatus
            } catch (ValidationException exc) {
                log.error 'Failed validation after annotations reading', exc
                ExitStatus.FAILED
            }
        }
    }
}
