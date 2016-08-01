package org.transmartproject.db.miscTables

import org.transmartproject.db.summaries.ConceptCount

import static org.transmartproject.db.TestDataHelper.save

/**
 * Created by piotrzakrzewski on 01/08/16.
 */
class ConceptCounts {

    def ConceptCounts() {
        ConceptCount count = new ConceptCount()
        count.concept_path = ""
        count.parent_concept_path = ""
        count.patient_count = 0
        counts.add(count)
    }

    List<ConceptCount> counts = new ArrayList<>()

    def saveAll() {
        counts*.save()
    }

}
