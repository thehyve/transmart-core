package org.transmartproject.db.dataquery.highdim.dataconstraints

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Iterables
import com.google.common.collect.Multimap
import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.search.SearchKeywordCoreDb

class SearchKeywordDataConstraint implements CriteriaDataConstraint {

    enum Correlations {

        GENE_IDENTITY   ('GENE'),
        PROTEIN_IDENTITY('Protein'),
        PATHWAY_GENE    ('PATHWAY_GENE'),
        HOMOLOGENE      ('HOMOLOGENE_GENE'),
        PROTEIN_GENE    ('PROTEIN TO GENE'),
        GENE_PROTEIN    ('GENE TO PROTEIN'),
        GENE_SIGNATURE  ('GENE_SIGNATURE_ITEM', 'SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW', 'DOMAIN_OBJECT_ID'),


        public String name
        public String correlationTable
        public String correlationTableBioMarkerColumn

        Correlations(String correlationType) {
            this.name = correlationType
            this.correlationTable = 'BIOMART.BIO_MARKER_CORREL_MV'
            this.correlationTableBioMarkerColumn = 'BIO_MARKER_ID'

        }

        Correlations(String correlationType,
                     String correlationTable,
                     String correlationColumn) {
            this.name = correlationType
            this.correlationTable = correlationTable
            this.correlationTableBioMarkerColumn = correlationColumn
        }
    }

    CorrelatedBiomarkersDataConstraint innerConstraint = new CorrelatedBiomarkersDataConstraint()

    static CriteriaDataConstraint createForSearchKeywords(Map map,
                                                          List<SearchKeywordCoreDb> searchKeywords) {
        List<Correlations> origCorrelationTypes = map.correlationTypes

        if (!origCorrelationTypes) {
            throw new IllegalArgumentException('Correlation types unspecified')
        }
        if (!searchKeywords) {
            throw new InvalidArgumentsException(
                    'Search keyword list cannot be empty. If trying to create the' +
                            'constraint using identifiers, check that they do exist')
        }

        /* if we have correlation types that encompass more than 1 correlation
         * table AND we actually have search keywords applicable to more than
         * 1 correlation table, we need to build a disjunction constraint with
         * two SearchKeywordDataConstraints there */

        /* map from correlation table to sk */
        Multimap<String, SearchKeywordCoreDb> multimap = ArrayListMultimap.create()

        searchKeywords.each {
            def type = it.uniqueId.split(':')[0]
            if (type == 'GENESIG' || type == 'GENELIST') {
                multimap.put Correlations.GENE_SIGNATURE.correlationTable, it
            } else {
                /* GENE_IDENTITY or anything else */
                multimap.put Correlations.GENE_IDENTITY.correlationTable, it
            }
        }

        def buildParams = multimap.asMap().collect { String correlationTable,
                                                     Collection<SearchKeywordCoreDb> keywords ->
            [
                    [
                            *:map,
                            correlationTypes: origCorrelationTypes.findAll { it.correlationTable == correlationTable },
                    ],
                    keywords
            ]

        }

        if (buildParams.size() == 1) {
            createForSearchKeywordIdsInternal(*buildParams[0])
        } else {
            def ret = new DisjunctionDataConstraint()
            ret.constraints = buildParams.collect {
                createForSearchKeywordIdsInternal(*it)
            }
            ret
        }
    }

    static CriteriaDataConstraint createForUniqueIds(Map map, List<String> uniqueIds) {
        createForSearchKeywords(map,
                SearchKeywordCoreDb.findAllByUniqueIdInList(uniqueIds))
    }

    static CriteriaDataConstraint createForSearchKeywordIds(Map map, List<Number> ids) {
        createForSearchKeywords(map,
                SearchKeywordCoreDb.findAllByIdInList(ids))
    }

    private static SearchKeywordDataConstraint createForSearchKeywordIdsInternal(
            Map map, List<SearchKeywordCoreDb> searchKeywordIds) {
        def constraint = createObject map
        constraint.searchKeywords = searchKeywordIds
        constraint
    }

//    /**
//     * Creates a constraint of this type
//     * @param keywords Multimap from data category to keyword
//     * @return
//     */
//    static SearchKeywordDataConstraint createForKeyords(Map map, Multimap<String, String> keywords) {
//        def ret = createObject map
//        List results = SearchKeywordCoreDb.withCriteria {
//            or {
//                keywords.asMap().each { String category, Collection<String> kw ->
//                    and {
//                        eq   'dataCategory', category
//                        'in' 'keyword',  kw
//                    }
//                }
//            }
//        }
//
//        ret.initialBioMarkerIds = results
//        ret
//    }

    private static SearchKeywordDataConstraint createObject(Map map) {
        def constraint = new SearchKeywordDataConstraint()

        [ 'entityAlias', 'propertyToRestrict', 'correlationTypes' ].each {
            if (map."$it" == null) {
                throw new IllegalArgumentException("Entry '$it' expected")
            }
            constraint."$it" = map."$it"
        }

        constraint
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        innerConstraint.doWithCriteriaBuilder criteria
    }

    void setCorrelationTypes(Set<Correlations> correlations)   {
        innerConstraint.correlationTypes = correlations*.name

        Set<String> tables = correlations*.correlationTable
        if (tables.size() != 1) {
            throw new IllegalArgumentException('Empty or impermissibly mixed correlation types')
        }

        innerConstraint.correlationTable = Iterables.getFirst tables, null
        innerConstraint.correlationColumn =
            Iterables.getFirst(correlations, null).correlationTableBioMarkerColumn
    }

    void setEntityAlias(String alias) {
        innerConstraint.entityAlias = alias
    }

    void setPropertyToRestrict(String property) {
        innerConstraint.propertyToRestrict = property
    }

    void setSearchKeywords(List<SearchKeywordCoreDb> searchKeywords) {
        innerConstraint.searchKeywords = searchKeywords
    }
}
