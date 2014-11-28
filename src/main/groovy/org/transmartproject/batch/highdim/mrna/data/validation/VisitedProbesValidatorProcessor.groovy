package org.transmartproject.batch.highdim.mrna.data.validation

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import org.transmartproject.batch.highdim.mrna.data.pass.MrnaDataValue
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.patient.PatientSet

/**
 * Checks whether all the probes for a platform had data.
 */
@Component
@StepScope
@CompileStatic
@Slf4j
class VisitedProbesValidatorProcessor extends ItemStreamSupport implements ItemProcessor<MrnaDataValue, MrnaDataValue> {

    {
        name = ClassUtils.getShortName(getClass())
    }

    @Value('#{stepExecution}')
    StepExecution stepExecution

    @Value('#{annotationEntityMap.probeSet}')
    private Set<String> annotationProbeSet

    @Value('#{mrnaMappings.allSubjectCodes}')
    private Set<String> mappingSubjectCodes

    @Autowired
    private PatientSet patientSet

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Map<Patient, Integer> patientToInt = (Map) ({ ->
        int i = 0
        mappingSubjectCodes.collectEntries { String subjectId ->
            [patientSet[subjectId], i++]
        }
    })()

    private final static String PROBE_PATIENTS_KEY = 'probePatients'

    // saved
    // (probeName, bit set of patients)
    private Map<String, BitSet> probePatients

    @Override
    MrnaDataValue process(MrnaDataValue item) throws Exception {
        def probe = item.probe

        if (annotationProbeSet.contains(probe) == null) {
            throw new ValidationException(
                    "Probe $probe is not in this platform")
        }

        def curBitSet = probePatients[probe]
        if (!curBitSet) {
            curBitSet = new BitSet(patientToInt.size())
            probePatients[probe] = curBitSet
        }
        int patientIndex = patientToInt[item.patient]
        boolean seen = curBitSet.get(patientIndex)
        if (seen) {
            throw new ValidationException("Repeated data for probe $probe, " +
                    "patient ${item.patient}")
        }
        curBitSet.set(patientIndex, true)

        item
    }

    @Override
    void update(ExecutionContext executionContext) {
        executionContext.put(
                getExecutionContextKey(PROBE_PATIENTS_KEY),
                probePatients)
    }

    @Override
    void open(ExecutionContext executionContext) {
        probePatients =  (Map) executionContext.get(
                getExecutionContextKey(PROBE_PATIENTS_KEY)) ?:
                (HashMap<String, BitSet>) [:]
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() {
        // Not sure if doing this on close() is the best idea.
        // A step execution listener would probably be more appropriate.
        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.warn("Skipping final validation in order to " +
                    "avoid hiding earlier problems")
        } else {
            finalValidation()
        }
    }

    private void finalValidation() {
        Set<String> seenProbes = probePatients.keySet()
        if (seenProbes != annotationProbeSet) {
            def missingProbes = annotationProbeSet - seenProbes
            throw new ValidationException('The set of seen probes is ' +
                    'smaller than that specified in the annotation file. ' +
                    "Missing probes: $missingProbes")
        }
    }
}
