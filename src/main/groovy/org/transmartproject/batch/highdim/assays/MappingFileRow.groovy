package org.transmartproject.batch.highdim.assays

import groovy.transform.ToString
import org.springframework.context.i18n.LocaleContextHolder
import org.transmartproject.batch.concept.ConceptFragment

/**
 * A row in a high dimensional data mapping file.
 * Columns: study_id, site_id, subject_id, sample_cd, platform, tissuetype,
 * sampleType, timePoint, category_cd
 */
@ToString(includes = 'subjectId,sampleCd')
class MappingFileRow {
    String studyId /* must be constant */
    String siteId  /* IGNORED */
    String subjectId
    String sampleCd
    String platform /* must be constant */
    String tissueType
    String sampleType
    String timePoint
    String categoryCd /* the path under TOP_NODE */
    String sourceCd /* IGNORED */

    void setStudyId(String studyId) {
        /* normalize study id */
        this.studyId = studyId.toUpperCase(LocaleContextHolder.locale)
    }

    private final Map<String, Closure<String>> replacements = [
            //Legacy
            ATTR1     : { -> tissueType },
            ATTR2     : { -> timePoint },
            //Current
            PLATFORM  : { -> platform },
            TISSUETYPE: { -> tissueType },
            SAMPLETYPE: { -> sampleType },
            TIMEPOINT : { -> timePoint },
    ]

    ConceptFragment getConceptFragment() {
        List<String> unprocessedParts = ConceptFragment.decode(categoryCd).parts

        List<String> processedParts = unprocessedParts.collect {
            if (replacements[it]) {
                replacements[it].call()
            } else {
                it
            }
        }

        new ConceptFragment(processedParts)
    }
}
