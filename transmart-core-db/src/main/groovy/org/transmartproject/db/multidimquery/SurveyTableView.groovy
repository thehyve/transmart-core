package org.transmartproject.db.multidimquery

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.*
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.ontology.VariableMetadata

import static org.transmartproject.core.ontology.VariableDataType.*
import static org.transmartproject.core.ontology.Measure.SCALE

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
@CompileStatic
class SurveyTableView implements TabularResult<MetadataAwareDataColumn, DataRow> {

    final List<MetadataAwareDataColumn> indicesList

    @Delegate
    final HypercubeTabularResultView hypercubeTabularResultView

    SurveyTableView(List<MetadataAwareDataColumn> columnList, Hypercube hypercube) {
        def rowDimensions = [DimensionImpl.PATIENT] as List<Dimension>
        def columnDimensions = [DimensionImpl.STUDY, DimensionImpl.CONCEPT] as List<Dimension>
        this.indicesList = columnList
        def valueColumnList = columnList.collect{ it as ValueFetchingDataColumn }
        hypercubeTabularResultView = new HypercubeTabularResultView(hypercube, rowDimensions, columnDimensions, valueColumnList)
    }

    @CompileStatic
    static class FisNumberColumn implements ValueFetchingDataColumn<String, HypercubeDataRow>, MetadataAwareDataColumn {
        final String label = 'FISNumber'
        // FIXME: do not rely on hardcoded id source
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
            def patient = (Patient)row.getDimensionElement(DimensionImpl.PATIENT)
            if (patient) {
                return patient.subjectIds[SUBJ_ID_SOURCE]
            }
            null
        }
    }

    @CompileStatic
    static class MeasurementDateColumn implements ValueFetchingDataColumn<Date, HypercubeDataRow>, MetadataAwareDataColumn {

        final Dimension startTimeDimension = (Dimension)DimensionImpl.START_TIME
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
            def hValue = row.getHypercubeValue(originalColumn.coordinates)
            if (hValue) {
                if (startTimeDimension in hValue.availableDimensions) {
                    Object value = hValue[startTimeDimension]
                    return (Date) value
                }
            }
            null
        }
    }

    @CompileStatic
    static class VariableColumn implements ValueFetchingDataColumn<Object, HypercubeDataRow>, MetadataAwareDataColumn {

        final String label
        final HypercubeDataColumn originalColumn
        final VariableMetadata metadata
        private final Map<String, BigDecimal> labelsToValues
        final Dimension missingValueDimension

        VariableColumn(String label, HypercubeDataColumn originalColumn,
                       VariableMetadata metadata,
                       Dimension missingValueDimension) {
            this.label = label
            this.originalColumn = originalColumn
            this.metadata = metadata
            labelsToValues = this.metadata.valueLabels.collectEntries { BigDecimal key, String value -> [ value, key ] } as Map<String, BigDecimal>
            this.missingValueDimension = missingValueDimension
        }

        Object getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.coordinates)
            if (hValue == null) {
                return null
            }
            def value = hValue.value
            def label = (value == null ? getMissingValueLabel(hValue) : value)
            def label_key = label.toString()

            if (labelsToValues.containsKey(label_key)) {
                return labelsToValues[label_key]
            }
            if (metadata?.type == DATE) {
                if (value instanceof Number) {
                    return toDate(value)
                } else if (value instanceof Date) {
                    return value
                }
            }
            return label
        }

        private static Date toDate(Number value) {
            new Date(value.longValue())
        }

        private String getMissingValueLabel(HypercubeValue hValue) {
            if (this.missingValueDimension in hValue.availableDimensions) {
                hValue[this.missingValueDimension]
            } else {
                null
            }
        }
    }

}
