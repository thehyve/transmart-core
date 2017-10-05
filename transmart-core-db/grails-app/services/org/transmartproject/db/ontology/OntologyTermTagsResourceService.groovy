/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.ontology

import groovy.transform.CompileStatic
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.db.util.StringUtils

class OntologyTermTagsResourceService implements OntologyTermTagsResource {

    private List<OntologyTermTag> retrieveTags(Collection<String> ontologyTermPaths, boolean includeDescendantsTags) {
        I2b2Tag.createCriteria().list {
            or {
                ontologyTermPaths.each { String path ->
                    if (includeDescendantsTags) {
                        add(StringUtils.startsWith('ontologyTermFullName', path))
                    } else {
                        eq 'ontologyTermFullName', path
                    }
                }
            }
            order 'ontologyTermFullName'
            order 'position'
        } as List<OntologyTermTag>
    }

    @Override
    Map<OntologyTerm, List<OntologyTermTag>> getTags(Set<OntologyTerm> ontologyTerms, boolean includeDescendantsTags) {
        if (!ontologyTerms) {
            return [:]
        }

        def orderedTags = retrieveTags(ontologyTerms*.fullName, includeDescendantsTags)
        if (!orderedTags) {
            return [:]
        }

        def terms = I2b2.findAllByFullNameInList((orderedTags*.ontologyTermFullName).unique()) as List<OntologyTerm>
        def termsMap = terms.collectEntries { [it.fullName, it] } as Map<String, OntologyTerm>

        orderedTags.groupBy { termsMap[it.ontologyTermFullName] }
    }

    @Override
    @CompileStatic
    Map<String, List<OntologyTermTag>> getTags(Set<String> ontologyTermPaths) {
        if (!ontologyTermPaths) {
            return [:]
        }

        def orderedTags = retrieveTags(ontologyTermPaths, false)
        if (!orderedTags) {
            return [:]
        }

        orderedTags.groupBy { it.ontologyTermFullName }
    }
}
