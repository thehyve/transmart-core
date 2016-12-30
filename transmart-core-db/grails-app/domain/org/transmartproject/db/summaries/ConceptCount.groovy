package org.transmartproject.db.summaries

/**
 * Created by piotrzakrzewski on 01/08/16.
 */
class ConceptCount {

    String concept_path
    String parent_concept_path
    int patient_count

    static mapping = {
        table   schema: 'i2b2demodata', name: 'concept_counts'
        version false
    }

}
