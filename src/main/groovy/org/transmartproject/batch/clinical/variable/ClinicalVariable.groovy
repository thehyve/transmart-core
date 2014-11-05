package org.transmartproject.batch.clinical.variable

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.batch.clinical.patient.DemographicVariable
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.MappingHelper

/**
 * Represents a Variable, as defined in column map file
 */
@ToString
@EqualsAndHashCode(includes = ['filename','columnNumber'])
class ClinicalVariable implements Serializable {

    private static final long serialVersionUID = 1L

    static final String SUBJ_ID = 'SUBJ_ID'
    static final String STUDY_ID = 'STUDY_ID'
    static final String SITE_ID = 'SITE_ID'
    static final String VISIT_NAME = 'VISIT_NAME'
    static final String OMIT = 'OMIT'

    static final List<String> RESERVED = [ SUBJ_ID, STUDY_ID, SITE_ID, VISIT_NAME, OMIT ]

    //the columns have fixed position, but not fixed names
    //most of the files have headers [filename, category_cd, col_nbr, data_label]
    //but some files dont, so we use position (not names) to identify columns
    static final FIELDS = ['filename','categoryCode','columnNumber','dataLabel']

    String filename

    String categoryCode

    Integer columnNumber

    String dataLabel

    ConceptPath conceptPath

    DemographicVariable demographicVariable

    boolean isDemographic() {
        demographicVariable != null
    }

    /*
     * TODO: why does ClinicalVariable know about parsing input streams?...
     */
    static List<ClinicalVariable> parse(InputStream input, LineListener listener, ConceptTree conceptTree) {
        MappingHelper.parseObjects(input,
                new VariableLineMapper(topNodePath: conceptTree.topNodePath),
                listener)
    }

}

