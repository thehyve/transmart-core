package org.transmartproject.db

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.querytool.QtQueryResultType

import static org.transmartproject.db.TestDataHelper.save

@Slf4j
@CompileStatic
class Dictionaries {

    final Set<DimensionDescription> dimensionDescriptions
    final Set<QtQueryResultType> qtQueryResultTypes

    Dictionaries() {
        this.dimensionDescriptions = createDimensionDescriptions()
        this.qtQueryResultTypes = createQtQueryResultTypes()
    }

    private static Set<DimensionDescription> createDimensionDescriptions() {
        [
                new DimensionDescription(name: 'study'),
                new DimensionDescription(name: 'concept'),
                new DimensionDescription(name: 'patient'),
                new DimensionDescription(name: 'visit'),
                new DimensionDescription(name: 'start time'),
                new DimensionDescription(name: 'end time'),
                new DimensionDescription(name: 'location'),
                new DimensionDescription(name: 'trial visit'),
                new DimensionDescription(name: 'provider'),
                new DimensionDescription(name: 'biomarker'),
                new DimensionDescription(name: 'assay'),
                new DimensionDescription(name: 'projection'),
                new DimensionDescription(
                        name: 'sample_type',
                        density: 'DENSE',
                        modifierCode: 'TNS:SMPL',
                        valueType: 'T',
                        packable: 'NOT_PACKABLE',
                        size: 'SMALL'),
                new DimensionDescription(
                        name: 'original_variable',
                        density: 'DENSE',
                        modifierCode: 'TRANSMART:ORIGINAL_VARIABLE',
                        valueType: 'T',
                        packable: 'NOT_PACKABLE',
                        size: 'SMALL'),
        ] as Set
    }

    private static Set<QtQueryResultType> createQtQueryResultTypes() {
        def patientSetType = new QtQueryResultType(description: 'Patient set')
        patientSetType.id = QueryResultType.PATIENT_SET_ID

        def genericQueryResultType = new QtQueryResultType(description: 'Generic query result')
        genericQueryResultType.id = QueryResultType.GENERIC_QUERY_RESULT_ID

        [patientSetType, genericQueryResultType] as Set
    }

    void saveAll() {
        log.info('Saving dictionaries.')
        save this.dimensionDescriptions
        save this.qtQueryResultTypes
    }
}
