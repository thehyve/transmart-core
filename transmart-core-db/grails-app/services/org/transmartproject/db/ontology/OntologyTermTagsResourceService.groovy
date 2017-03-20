package org.transmartproject.db.ontology

import org.hibernate.criterion.MatchMode
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.db.util.StringUtils

class OntologyTermTagsResourceService implements OntologyTermTagsResource {

    @Override
    Map<OntologyTerm, List<OntologyTermTag>> getTags(Set<OntologyTerm> ontologyTerms, boolean includeDescendantsTags) {
        if (!ontologyTerms) {
            return [:]
        }

        def orderedTags = I2b2Tag.createCriteria().list {
            or {
                ontologyTerms.each { OntologyTerm term ->
                    if (includeDescendantsTags) {
                        add(StringUtils.like('ontologyTermFullName', term.fullName, MatchMode.START))
                    } else {
                        eq 'ontologyTermFullName', term.fullName
                    }
                }
            }
            order 'ontologyTermFullName'
            order 'position'
        }

        if (!orderedTags) {
            return [:]
        }

        def terms = I2b2.findAllByFullNameInList((orderedTags*.ontologyTermFullName).unique())
        def termsMap = terms.collectEntries { [it.fullName, it] }

        orderedTags.groupBy { termsMap[it.ontologyTermFullName] }
    }

}
