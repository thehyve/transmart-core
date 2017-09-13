package org.transmartproject.db.multidimquery

import groovy.util.logging.Slf4j
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.ValueFetchingDataColumn
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.i2b2data.Study

import java.rmi.UnexpectedException

import static org.transmartproject.db.multidimquery.ColumnDataType.DATE
import static org.transmartproject.db.multidimquery.ColumnDataType.NUMERIC
import static org.transmartproject.db.multidimquery.ColumnDataType.STRING
import static org.transmartproject.db.multidimquery.Measure.SCALE

/**
 * Custom tabular view
 * | <subject id> | item1 | item1.date | item2 | item2.date |
 * - Adds first column with subject id
 * - Adds the start time column after each measurement column.
 * - Renames column names to the study specific
 * - Blends in the missing values
 * - Returns back original codebook values
 *
 * You can customise table by specifying in the configuration file following structure:
 * export {
     table {
         customizations {
             subjectIdColumnName = ...
             subjectIdColumnDescription = ...
             subjectIdSource = 'EXTDB'
             dateColDescription = 'Date of measurement'
         }
     }
 }
 */
@Slf4j
class HypercubeTabularResultTransformedView implements TabularResult<MetadataAwareDataColumn, DataRow> {

    @Delegate
    final HypercubeTabularResultView hypercubeTabularResultView

    final Hypercube hypercube

    public static final String DEFAULT_SUBJ_ID_COL_NAME = 'subject'
    public static final String DEFAULT_SUBJ_ID_COL_DESCRIPTION = 'Subject Id'
    public static final String DEFAULT_SUBJ_ID_SOURCE = 'SUBJ_ID'
    public static final String DEFAULT_DATE_COL_DESCRIPTION = 'Date and time of observation'

    final String subjectIdColumnName
    final String subjectIdColumnDescription
    final String subjectIdSource
    final String dateColDescription

    HypercubeTabularResultTransformedView(Map<String, String> customizations, Hypercube hypercube) {
        this.hypercube = hypercube

        def rowDimensions = [DimensionImpl.PATIENT]
        def columnDimensions = [DimensionImpl.STUDY, DimensionImpl.CONCEPT]
        hypercubeTabularResultView = new HypercubeTabularResultView(hypercube, rowDimensions, columnDimensions)

        subjectIdColumnName = customizations?.subjectIdColumnName ?: DEFAULT_SUBJ_ID_COL_NAME
        subjectIdColumnDescription = customizations?.subjectIdColumnDescription ?: DEFAULT_SUBJ_ID_COL_DESCRIPTION
        subjectIdSource = customizations?.subjectIdSource ?: DEFAULT_SUBJ_ID_SOURCE
        dateColDescription = customizations?.dateColDescription ?: DEFAULT_DATE_COL_DESCRIPTION
    }

    @Lazy
    List<MetadataAwareDataColumn> indicesList = {
        def originalColumns = hypercubeTabularResultView.indicesList
        def transformedColumns = []
        transformedColumns << new SubjectIdColumn()
        transformedColumns.addAll(originalColumns.collect { originalColumn ->
            String varName = getVariableName(originalColumn)
            [
                    new VariableColumn(varName, originalColumn),
                    new StartDateColumn("${varName}.date", originalColumn)
            ]
        }.flatten().sort { DataColumn a, DataColumn b -> a.label <=> b.label })
        transformedColumns
    }()

    class SubjectIdColumn implements ValueFetchingDataColumn<Long, HypercubeDataRow>, MetadataAwareDataColumn {
        String label = subjectIdColumnName

        ColumnMetadata metadata = new ColumnMetadata(
                type: NUMERIC,
                measure: SCALE,
                description: subjectIdColumnDescription,
                width: 12,
                decimals: 0,
                columns: 12,
        )

        @Override
        Long getValue(HypercubeDataRow row) {
            org.transmartproject.db.i2b2data.PatientDimension patient = row.getDimensionElement(DimensionImpl.PATIENT)
            if (patient) {
                return patient.mappings.find { it.source == subjectIdSource }.encryptedId as Long
            }
        }
    }

    class StartDateColumn implements ValueFetchingDataColumn<Date, HypercubeDataRow>, MetadataAwareDataColumn {

        final String label
        final HypercubeDataColumn originalColumn

        ColumnMetadata metadata = new ColumnMetadata(
                type: DATE,
                measure: SCALE,
                description: dateColDescription,
                width: 22,
                columns: 22,
        )

        StartDateColumn(String label, HypercubeDataColumn originalColumn) {
            this.label = label
            this.originalColumn = originalColumn
        }

        Date getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.index)
            hValue[DimensionImpl.START_TIME] as Date
        }
    }

    class VariableColumn implements ValueFetchingDataColumn<Object, HypercubeDataRow>, MetadataAwareDataColumn {

        private final static MISSING_VALUE_MODIFIER_DIMENSION_NAME = 'missing_value'
        final String label
        final HypercubeDataColumn originalColumn
        final ColumnMetadata metadata
        private final Map<String, Integer> labelsToValues = [:]

        VariableColumn(String label, HypercubeDataColumn originalColumn) {
            this.label = label
            this.originalColumn = originalColumn
            org.transmartproject.db.i2b2data.ConceptDimension conceptDimension =
                    originalColumn.getDimensionElement(DimensionImpl.CONCEPT)
            metadata = parseColumnMetadata(conceptDimension.conceptBlobAsJson(), conceptDimension.nameChar)
            labelsToValues = metadata.valueLabels.collectEntries { key, value -> [value, key] }
        }

        Object getValue(HypercubeDataRow row) {
            def hValue = row.getHypercubeValue(originalColumn.index)
            def value = hValue.value
            switch (metadata?.type) {
                case NUMERIC:
                    if (value instanceof Number) return value
                    String label = value == null ? getMissingValueLabel(hValue) : value
                    if (labelsToValues.containsKey(label)) return labelsToValues[label]
                    if (value == null) return null
                    throw new UnexpectedException("${value} is not of a number type.")
                case DATE:
                    if (value == null) return null
                    if (value instanceof Number) return toDate(value)
                    throw new UnexpectedException("${value} can't be converted to the date type.")
                case STRING:
                    return value as String
                default:
                    return value
            }
        }

        private Date toDate(Number value) {
            new Date((value * 1000) as Long)
        }

        private String getMissingValueLabel(HypercubeValue hValue) {
            hValue.availableDimensions
                    .find { it.name == MISSING_VALUE_MODIFIER_DIMENSION_NAME }
                    .with { naDim -> hValue[naDim] }
        }

        ColumnMetadata parseColumnMetadata(json, String description) {
            if (!json) return null
            new ColumnMetadata(
                    type: json.type?.toUpperCase() as ColumnDataType,
                    measure: json.measure?.toUpperCase() as Measure,
                    description: description,
                    width: json.width,
                    decimals: json.decimals,
                    valueLabels: json.valueLabels?.collectEntries { key, value -> [key as Integer, value] } ?: [:],
                    missingValues: json.missingValues?.collect { it as Integer } ?: [],
                    columns: json.columns
            )
        }
    }

    private static String getVariableName(HypercubeDataColumn originalColumn) {
        Study study = originalColumn.getDimensionElement(DimensionImpl.STUDY)
        org.transmartproject.db.i2b2data.ConceptDimension conceptDimension =
                originalColumn.getDimensionElement(DimensionImpl.CONCEPT)

        def studyMetadata = study.studyBlobAsJson()
        if (studyMetadata
                && studyMetadata.conceptToVariableName
                && conceptDimension.conceptCode in studyMetadata.conceptToVariableName) {
            return studyMetadata.conceptToVariableName[conceptDimension.conceptCode]
        } else {
            conceptDimension.conceptCode
        }
    }
}

enum ColumnDataType {
    NUMERIC, DATE, STRING
}

enum Measure {
    NOMINAL, ORDINAL, SCALE
}

class ColumnMetadata {
    ColumnDataType type
    Measure measure
    String description
    Integer width
    Integer decimals
    Integer columns
    Map<Integer, String> valueLabels
    List<Integer> missingValues
}

interface MetadataAwareDataColumn extends DataColumn {
    ColumnMetadata getMetadata()
}