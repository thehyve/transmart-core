package org.transmartproject.db.dataquery.highdim.mrna

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.Criteria
import org.hibernate.HibernateException
import org.hibernate.criterion.CriteriaQuery
import org.hibernate.criterion.SQLCriterion
import org.hibernate.type.StringType
import org.hibernate.type.Type
import org.hibernate.util.StringHelper
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.search.SearchKeyword

class MrnaGeneDataConstraint implements CriteriaDataConstraint {

    List ids
    String type

    /**
     * Create a constraint based on "long" gene ids. This is (apparently)
     * tranSMART-speak for the identifier stored in SEARCHAPP.SEARCH_KEYWORD,
     * column UNIQUE_ID. Data in this column is in the form <type>:<id>,
     * e.g. GENE:12345
     *
     * Also supported are gene signature ids. These are unique ids in the form
     * GENESIG:<gene signature id>
     *
     * The two cannot be mixed.
     *
     * See GeneExpressionDataService::derivePathwayName()
     *
     * @param ids the long ids of the genes or gene signature ids prefixed by
     * GENESIG:. GENELIST: is also allowed.
     * @return
     */
    static MrnaGeneDataConstraint createForLongIds(List<String> ids) {
        Set<String> idTypes = ids*.split(':')*.getAt(0)
        if (idTypes.size() != 1) {
            throw new InvalidArgumentsException('Expected exactly one type ' +
                    "of search keywords; got $idTypes for argument $ids")
        }

        String type = idTypes.iterator().next()
        if (type != 'GENE' && type != 'GENESIG' && type != 'GENELIST') {
            throw new InvalidArgumentsException('Only "GENE", "GENESIG" and ' +
                    '"GENELIST" are allowed as keyword types, got "' + type + '"')
        }

        new MrnaGeneDataConstraint(ids: ids, type: type)
    }

    static MrnaGeneDataConstraint createForSearchKeywordIds(List<Number> ids) {
        def longIds = SearchKeyword.findAllByIdInList(ids*.longValue()).
                collect { it.uniqueId }
        createForLongIds longIds
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteriaBuilder) {
        criteriaBuilder.add(new MrnaGeneCriterion(ids, type))
    }

    static class MrnaGeneCriterion extends SQLCriterion {
        MrnaGeneCriterion(List longGeneIds, String type) {
            /* this logic (different views) was taken from code on
             * GeneExpressionDataService. I'm trusting this is sound. */
            this(longGeneIds,
                type == 'GENE' ? 'biomart.bio_marker_correl_mv'
                               : 'searchapp.search_bio_mkr_correl_view',
                type == 'GENE' ? 'bio_marker_id'
                               : 'domain_object_id')
        }

        MrnaGeneCriterion(List longGeneIds, String table, String column) {
            super(
                    'CAST ({alias}.GENE_ID AS VARCHAR(30)) IN (\n' +
                            '   SELECT primary_external_id\n' +
                            '       FROM biomart.bio_marker bm\n' +
                            "           INNER JOIN $table sbm\n" +
                            '               ON sbm.asso_bio_marker_id = bm.bio_marker_id\n' +
                            '           INNER JOIN searchapp.search_keyword sk\n' +
                            "               ON sk.bio_data_id = sbm.$column\n" +
                            '       WHERE sk.unique_id IN (' + longGeneIds.collect { '?' }.join(', ') + ')\n' +
                            ')',
                    longGeneIds as Object[],
                    longGeneIds.collect { StringType.INSTANCE } as Type[]
            )
        }

        @Override
        String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            StringHelper.replace(toString(), "{alias}",
                    criteriaQuery.getSQLAlias(criteriaQuery.getCriteria('jProbe')))
        }
    }


}
