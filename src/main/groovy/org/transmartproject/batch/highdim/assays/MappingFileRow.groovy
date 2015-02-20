package org.transmartproject.batch.highdim.assays

import groovy.transform.ToString
import org.springframework.context.i18n.LocaleContextHolder
import org.transmartproject.batch.concept.ConceptFragment

/**
 * A row in a high dimensional data mapping file.
 * Columns: study_id, site_id, subject_id, sample_cd, platform, tissuetype,
 * attr1, attr2, category_cd
 */
@ToString(includes = 'subjectId,sampleCd')
class MappingFileRow {
    String studyId /* must be constant */
    String siteId  /* IGNORED */
    String subjectId
    String sampleCd
    String platform /* must be constant */
    String tissueType
    String attr1 /* just for replacing placeholders */
    String attr2 /* just for replacing placeholders */
    String categoryCd /* the path under TOP_NODE/NODE_NAME */
    String sourceCd /* IGNORED */

    void setStudyId(String studyId) {
        /* normalize study id */
        this.studyId = studyId.toUpperCase(LocaleContextHolder.locale)
    }

    private final Map<String, Closure<String>> replacements = [
            PLATFORM: { -> platform },
            TISSUETYPE: { -> tissueType },
            ATTR1: { -> attr1 },
            ATTR2: { -> attr2 },
    ]

    ConceptFragment getConceptFragment() {
        // after TOP_NODE/NODE_NAME
        def parts = categoryCd.split('\\+|\\\\').collect {
            if (replacements[it]) {
                replacements[it].call()
            } else {
                it
            }
        }.findAll() // remove empty segments

        new ConceptFragment(parts)
    }
}
