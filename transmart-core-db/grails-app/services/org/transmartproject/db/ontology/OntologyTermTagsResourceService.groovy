package org.transmartproject.db.ontology

import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.CriteriaSpecification
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

        /**
         * The query for tags is split into two separate queries, to make sure
         * that tags associated with tag types (through the 'option' field)
         * appear first.
         * This could be done in a single query once 'NULLS LAST' is supported in Grails.
         */
        def orderedTags = I2b2Tag.createCriteria().listDistinct {
            createAlias('option', 'o', CriteriaSpecification.INNER_JOIN)
            createAlias('o.type', 't', CriteriaSpecification.INNER_JOIN)
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
            order 't.index'
        }

        orderedTags.addAll(
                I2b2Tag.createCriteria().list {
                    or {
                        ontologyTerms.each { OntologyTerm term ->
                            if (includeDescendantsTags) {
                                like 'ontologyTermFullName', (term.fullName.replaceAll(/[\\%_]/, '\\\\$0') + '%')
                            } else {
                                eq 'ontologyTermFullName', term.fullName
                            }
                        }
                    }
                    isNull 'option'
                    order 'ontologyTermFullName'
                    order 'position'
                }
        )

        if (!orderedTags) {
            return [:]
        }

        def terms = I2b2.findAllByFullNameInList((orderedTags*.ontologyTermFullName).unique())
        def termsMap = terms.collectEntries { [it.fullName, it] }

        orderedTags.groupBy { termsMap[it.ontologyTermFullName] }
    }

}
