package org.transmartproject.db.multidimquery

import groovy.util.logging.Slf4j
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.*
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.ontology.MDStudy

import static VariableDataType.*
import static org.transmartproject.core.dataquery.Measure.NOMINAL
import static org.transmartproject.core.dataquery.Measure.SCALE

/**
 * Custom tabular view
 * | <subject id> | item1 | item1.date | item2 | item2.date |
 * - Adds first column with subject id
 * - Adds the start time column after each measurement column.
 * - Renames column names to the study specific
 * - Blends in the missing values
 * - Returns back original codebook values
 */
@Slf4j
class SurveyTableView implements TabularResult<MetadataAwareDataColumn, DataRow> {

    @Delegate
    final HypercubeTabularResultView hypercubeTabularResultView

    final Hypercube hypercube

    SurveyTableView(Hypercube hypercube) {
        this.hypercube = hypercube

        def rowDimensions = [DimensionImpl.PATIENT]
        def columnDimensions = [DimensionImpl.STUDY, DimensionImpl.CONCEPT]
        hypercubeTabularResultView = new HypercubeTabularResultView(hypercube, rowDimensions, columnDimensions)
    }

    @Lazy
    List<MetadataAwareDataColumn> indicesList = {
        def originalColumns = hypercubeTabularResultView.indicesList
        def transformedColumns = []
        transformedColumns << new FisNumberColumn()
        transformedColumns.addAll(originalColumns.collect { originalColumn ->
            String varName = getVariableName(originalColumn)
            [
                    new VariableColumn(varName, originalColumn),
                    new MeasurementDateColumn("${varName}.date", originalColumn)
            ]
        }.flatten().sort { DataColumn a, DataColumn b -> a.label <=> b.label })
        transformedColumns
    }()

    class FisNumberColumn implements ValueFetchingDataColumn<String, HypercubeDataRow>, MetadataAwareDataColumn {
        String label = 'FISNumber'
        static final String SUBJ_ID_SOURCE = 'SUBJ_ID'

        VariableMetadata metadata = new VariableMetadata(
                type: NUMERIC,
                measure: SCALE,
                description: 'FIS Number',
                width: 12,
                decimals: 0,
                columns: 12,
        )

        @Override
        String getValue(HypercubeDataRow row) {
            def patient = row.getDimensionElement(DimensionImpl.PATIENT) as Patient
            if (patient) {
                return patient.subjectIds[SUBJ_ID_SOURCE]
            }
            null
        }
    }

    class MeasurementDateColumn implements ValueFetchingDataColumn<Date, HypercubeDataRow>, MetadataAwareDataColumn {

        final String label
        final HypercubeDataColumn originalColumn

        VariableMetadata metadata = new VariableMetadata(
                type: DATE,
                measure: SCALE,
                description: 'Date of measurement',
                width: 22,
                columns: 22,
        )

        MeasurementDateColumn(String label, HypercubeDataColumn originalColumn) {
            this.label = label
            this.originalColumn = originalColumn
        }

        Date getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.index)
            if (hValue && DimensionImpl.START_TIME in hValue.availableDimensions) {
                return hValue[DimensionImpl.START_TIME] as Date
            }
            null
        }
    }

    class VariableColumn implements ValueFetchingDataColumn<Object, HypercubeDataRow>, MetadataAwareDataColumn {

        private final static MISSING_VALUE_MODIFIER_DIMENSION_NAME = 'missing_value'
        final String label
        final HypercubeDataColumn originalColumn
        final VariableMetadata metadata
        private final Map<String, Integer> labelsToValues = [:]

        VariableColumn(String label, HypercubeDataColumn originalColumn) {
            this.label = label
            this.originalColumn = originalColumn
            def concept = originalColumn.getDimensionElement(DimensionImpl.CONCEPT) as Concept
            metadata = concept.metadata ?: computeColumnMetadata()
            labelsToValues = metadata.valueLabels.collectEntries { key, value -> [value, key] }
        }

        Object getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.index)
            if (hValue == null) return null
            def value = hValue.value
            switch (metadata?.type) {
                case NUMERIC:
                    if (value instanceof Number) return value
                    String label = value == null ? getMissingValueLabel(hValue) : value
                    if (labelsToValues.containsKey(label)) return labelsToValues[label]
                    if (value == null) return null
                    throw new UnexpectedResultException("${value} is not of a number type.")
                case DATE:
                    if (value == null) return null
                    if (value instanceof Number) return toDate(value)
                    throw new UnexpectedResultException("${value} can't be converted to the date type.")
                case STRING:
                    return value as String
                default:
                    return value
            }
        }

        private Date toDate(Number value) {
            new Date(value as Long)
        }

        private VariableMetadata computeColumnMetadata() {
            new VariableMetadata(
                    type: STRING,
                    measure: NOMINAL,
                    description: originalColumn.label,
                    width: 25,
                    columns: 25,
            )
        }

        private String getMissingValueLabel(HypercubeValue hValue) {
            hValue.availableDimensions
                    .find { it.name == MISSING_VALUE_MODIFIER_DIMENSION_NAME }
                    .with { naDim -> hValue[naDim] }
        }
    }

    private static String getVariableName(HypercubeDataColumn originalColumn) {
        def study = originalColumn.getDimensionElement(DimensionImpl.STUDY) as MDStudy
        def concept =
                originalColumn.getDimensionElement(DimensionImpl.CONCEPT) as Concept

        def studyMetadata = study.metadata
        if (studyMetadata?.conceptToVariableName?.containsKey(concept.conceptCode)) {
            return studyMetadata.conceptToVariableName[concept.conceptCode]
        } else {
            concept.conceptCode
        }
    }
}