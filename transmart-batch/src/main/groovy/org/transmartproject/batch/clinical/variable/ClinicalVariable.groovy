package org.transmartproject.batch.clinical.variable

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.patient.DemographicVariable

/**
 * Represents a Variable, as defined in column map file
 */
@ToString
@EqualsAndHashCode(includes = ['filename', 'columnNumber'])
class ClinicalVariable implements Serializable {

    private static final long serialVersionUID = 1L

    public static final String SUBJ_ID = 'SUBJ_ID'
    public static final String STUDY_ID = 'STUDY_ID'
    public static final String SITE_ID = 'SITE_ID'
    public static final String VISIT_NAME = 'VISIT_NAME'
    public static final String OMIT = 'OMIT'
    public static final String DATA_LABEL = 'DATA_LABEL'
    public static final String TEMPLATE = '\\'

    //Template placeholders. Note that they do not have underscore in it.
    //We have to support exactly these names for placeholders as legacy.
    public static final String SITE_ID_PLACEHOLDER = SITE_ID.replace('_', '')
    public static final String VISIT_NAME_PLACEHOLDER = VISIT_NAME.replace('_', '')
    public static final String DATA_LABEL_PLACEHOLDER = DATA_LABEL.replace('_', '')

    public static final List<String> RESERVED = [SUBJ_ID, STUDY_ID, SITE_ID, VISIT_NAME, OMIT, TEMPLATE, DATA_LABEL]

    /* The columns have fixed position, but not fixed names.
     * Most of the files have headers [filename, category_cd, col_nbr, data_label]
     * but some files dont, so we use position (not names) to identify columns
     */
    public static final String FIELD_FILENAME = 'filename'
    public static final String FIELD_CATEGORY_CODE = 'categoryCode'
    public static final String FIELD_COLUMN_NUMBER = 'columnNumber'
    public static final String FIELD_DATA_LABEL = 'dataLabel'
    public static final String FIELD_DATA_LABEL_SOURCE = 'dataLabelSource'
    public static final String FIELD_CONTROL_VOCAB_CODE = 'controlledVocabularyCode' // ignored
    public static final String FIELD_CONCEPT_TYPE = 'conceptType'

    static final FIELDS = [FIELD_FILENAME,
                           FIELD_CATEGORY_CODE,
                           FIELD_COLUMN_NUMBER,
                           FIELD_DATA_LABEL,
                           FIELD_DATA_LABEL_SOURCE,
                           FIELD_CONTROL_VOCAB_CODE,
                           FIELD_CONCEPT_TYPE]

    public static final String CONCEPT_TYPE_CATEGORICAL = 'CATEGORICAL'
    public static final String CONCEPT_TYPE_NUMERICAL = 'NUMERICAL'

    /* can be filled directly from fields */
    String filename

    String categoryCode

    Integer columnNumber

    void setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber - 1 /* to make it 0-based */
    }

    String dataLabel

    Integer dataLabelSource

    void setDataLabelSource(Integer dataLabelSource) {
        this.dataLabelSource = dataLabelSource ? dataLabelSource - 1 : null /* to make it 0-based */
    }

    String conceptType

    void setConceptType(String conceptType) {
        // note that we don't convert empty strings to null when we load the
        // column mappings file (emptyStringsToNull: false for tsvFileReader())
        this.conceptType = conceptType == '' ? null : conceptType
    }

    /**
     * These are to be calculated
     * @see ClinicalVariableFieldMapper */

    /** */
    ConceptPath conceptPath

    DemographicVariable demographicVariable

    boolean isDemographic() {
        demographicVariable != null
    }

}

