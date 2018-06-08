/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.ontology

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import org.hibernate.criterion.CriteriaSpecification
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.db.util.StringUtils

class OntologyTermTagsResourceService implements OntologyTermTagsResource {

    /**
     * The query for tags is split into two separate queries, to make sure
     * that tags associated with tag types (through the 'option' field)
     * appear first.
     * This could be done in a single query once 'NULLS LAST' is supported in Grails.
     */
    private List<OntologyTermTag> retrieveTags(Collection<String> ontologyTermPaths, boolean includeDescendantsTags) {
        /**
         * The query for tags is split into two separate queries, to make sure
         * that tags associated with tag types (through the 'option' field)
         * appear first.
         * This could be done in a single query once 'NULLS LAST' is supported in Grails.
         */
        List<OntologyTermTag> orderedTags = I2b2Tag.createCriteria().listDistinct {
            createAlias('option', 'o', CriteriaSpecification.INNER_JOIN)
            createAlias('o.type', 't', CriteriaSpecification.INNER_JOIN)
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
            order 't.index'
        }

        orderedTags.addAll(
                I2b2Tag.createCriteria().list {
                    or {
                        ontologyTermPaths.each { String path ->
                            if (includeDescendantsTags) {
                                like 'ontologyTermFullName', (path.replaceAll(/[\\%_]/, '\\\\$0') + '%')
                            } else {
                                eq 'ontologyTermFullName', path
                            }
                        }
                    }
                    isNull 'option'
                    order 'ontologyTermFullName'
                    order 'position'
                }
        )

        return orderedTags
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
    @Cacheable('org.transmartproject.db.ontology.OntologyTermTagsResourceService')
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

    /**
     * Clears the tags cache. This function should be called after loading, removing or updating
     * tags in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.ontology.OntologyTermTagsResourceService',
            allEntries = true)
    void clearTagsCache() {
        log.info 'Clearing tags cache ...'
    }

}
