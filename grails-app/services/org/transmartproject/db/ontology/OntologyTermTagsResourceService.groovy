package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource

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
                        like 'ontologyTermFullName', (term.fullName.asLikeLiteral() + '%')
                    } else {
                        eq 'ontologyTermFullName', term.fullName
                    }
                }
            }
            order 'ontologyTermFullName'
            order 'position'
        }

        def terms = I2b2.findAllByFullNameInList((orderedTags*.ontologyTermFullName).unique())
        def termsMap = terms.collectEntries { [it.fullName, it] }

        orderedTags.groupBy { termsMap[it.ontologyTermFullName] }
    }

}
