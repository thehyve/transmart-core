package org.transmartproject.db.multidimquery

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.*
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.VariableMetadata

import java.time.Instant

import static org.transmartproject.core.ontology.VariableDataType.*
import static org.transmartproject.core.ontology.Measure.NOMINAL
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

    @CompileStatic
    static class DataColumnComparator<C extends DataColumn> implements Comparator<C> {
        @Override
        int compare(C a, C b) {
            a.label <=> b.label
        }
    }

    static final Comparator<MetadataAwareDataColumn> dataColumnComparator = new DataColumnComparator<MetadataAwareDataColumn>()

    @Delegate
    final HypercubeTabularResultView hypercubeTabularResultView

    final Hypercube hypercube

    SurveyTableView(Hypercube hypercube) {
        this.hypercube = hypercube

        def rowDimensions = [DimensionImpl.PATIENT] as List<Dimension>
        def columnDimensions = [DimensionImpl.STUDY, DimensionImpl.CONCEPT] as List<Dimension>
        hypercubeTabularResultView = new HypercubeTabularResultView(hypercube, rowDimensions, columnDimensions)
    }

    @Lazy
    List<MetadataAwareDataColumn> indicesList = {
        def originalColumns = hypercubeTabularResultView.indicesList
        List<MetadataAwareDataColumn> transformedColumns = []
        transformedColumns.add(new FisNumberColumn())
        for (HypercubeDataColumn originalColumn: originalColumns) {
            String varName = getVariableName(originalColumn)
            transformedColumns.add(new VariableColumn(varName, originalColumn))
            transformedColumns.add(new MeasurementDateColumn("${varName}.date", originalColumn))
        }
        transformedColumns.sort(dataColumnComparator)
        transformedColumns
    }()

    @CompileStatic
    class FisNumberColumn implements ValueFetchingDataColumn<String, HypercubeDataRow>, MetadataAwareDataColumn {
        final String label = 'FISNumber'
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

    @CompileStatic
    class MeasurementDateColumn implements ValueFetchingDataColumn<Date, HypercubeDataRow>, MetadataAwareDataColumn {

        final String label
        final HypercubeDataColumn originalColumn
        final boolean hasStartDate

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
            this.hasStartDate = DimensionImpl.START_TIME in originalColumn.hypercube.dimensions
        }

        Date getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.index)
            if (hValue && hasStartDate) {
                Object value = hValue[(Dimension)DimensionImpl.START_TIME]
                return (Date)value
            }
            null
        }
    }

    @CompileStatic
    class VariableColumn implements ValueFetchingDataColumn<Object, HypercubeDataRow>, MetadataAwareDataColumn {

        final static MISSING_VALUE_MODIFIER_DIMENSION_NAME = 'missing_value'
        final String label
        final HypercubeDataColumn originalColumn
        final VariableMetadata metadata
        private final Map<String, BigDecimal> labelsToValues
        final Dimension missingValueDimension

        VariableColumn(String label, HypercubeDataColumn originalColumn) {
            this.label = label
            this.originalColumn = originalColumn
            def concept = originalColumn.getDimensionElement(DimensionImpl.CONCEPT) as Concept
            metadata = getStudyVariableMetadata(originalColumn) ?: computeColumnMetadata(concept)
            labelsToValues = metadata.valueLabels.collectEntries { BigDecimal key, String value -> [ value, key ] } as Map<String, BigDecimal>
            for (Dimension dim: originalColumn.hypercube.dimensions) {
                if (dim.name == MISSING_VALUE_MODIFIER_DIMENSION_NAME) {
                    missingValueDimension = dim
                    break
                }
            }
        }

        Object getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.index)
            if (hValue == null) {
                return null
            }
            def value = hValue.value
            def label = value == null ? getMissingValueLabel(hValue) : value
            if (labelsToValues.containsKey(label)) {
                return labelsToValues[label]
            }
            if (metadata?.type == DATE && value instanceof Number) {
                return toDate(value)
            }
            return value
        }

        private Date toDate(Number value) {
            new Date(value.longValue())
        }

        private VariableMetadata computeColumnMetadata(Concept concept) {
            new VariableMetadata(
                    type: STRING,
                    measure: NOMINAL,
                    description: concept.name,
                    width: 25,
                    columns: 25
            )
        }

        private String getMissingValueLabel(HypercubeValue hValue) {
            hValue[missingValueDimension]
        }
    }

    private static String getVariableName(HypercubeDataColumn originalColumn) {
        VariableMetadata varMeta = getStudyVariableMetadata(originalColumn)
        if (varMeta?.name) {
            return varMeta?.name
        } else {
            def concept = originalColumn.getDimensionElement(DimensionImpl.CONCEPT) as Concept
            return concept.conceptCode
        }
    }

    private static VariableMetadata getStudyVariableMetadata(HypercubeDataColumn originalColumn) {
        def study = originalColumn.getDimensionElement(DimensionImpl.STUDY) as MDStudy
        def concept = originalColumn.getDimensionElement(DimensionImpl.CONCEPT) as Concept

        study.metadata?.conceptCodeToVariableMetadata?.get(concept.conceptCode)
    }
}