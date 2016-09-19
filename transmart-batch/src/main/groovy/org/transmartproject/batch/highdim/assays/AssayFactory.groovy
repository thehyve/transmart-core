package org.transmartproject.batch.highdim.assays

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.highdim.platform.Platform
import org.transmartproject.batch.patient.PatientSet

import javax.annotation.PostConstruct

/**
 * Creates Assay objects.
 */
@JobScope
@Component
class AssayFactory {

    @Autowired
    private SequenceReserver sequenceReserver

    @Autowired
    private PatientSet patientSet

    @Autowired
    MappingFileRowToConceptMapper mapper

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Value("#{jobExecutionContext['platformObject']}")
    private Platform platform

    @PostConstruct
    void validateProperties() {
        assert platform != null
    }

    Assay createFromMappingRow(MappingFileRow row) {
        new Assay(
                id: sequenceReserver.getNext(Sequences.ASSAY_ID),
                patient: patientSet[row.subjectId],
                concept: mapper[row],
                studyId: studyId,
                sampleType: row.sampleType,
                sampleCode: row.sampleCd,
                tissueType: row.tissueType,
                timePoint: row.timePoint,
                platform: platform,
        )
    }
}
