package org.transmartproject.db.dataquery.highdim.dataconstraints

import grails.orm.HibernateCriteriaBuilder
import grails.util.Holders
import org.hibernate.Criteria
import org.hibernate.HibernateException
import org.hibernate.criterion.CriteriaQuery
import org.hibernate.criterion.SQLCriterion
import org.hibernate.type.LongType
import org.hibernate.type.StringType
import org.hibernate.type.Type

/*
 * Search for biomarkers in this fashion:
 * - start with biomarker ids (PK) from BIO_MARKER
 * - search correlations of the types specified in BIO_MARKER_CORREL_MV
 *   where the found biomarker ids are present in the BIO_MARKER_ID column
 *   (1 st column)
 * - collect resulting associated biomarker ids (ASSO_BIO_MARKER_ID)
 * - go back to bio_marker to find the PRIMARY_EXTERNAL_ID of these new biomarker ids
 */
class CorrelatedBiomarkersDataConstraint implements CriteriaDataConstraint {

    List<Long> initialBioMarkerIds

    List<String> correlationTypes // hopefully these all map to the same data type!

    String entityAlias // entity to restrict against the final primary ext ids

    String propertyToRestrict // entity properties whose values will be matched
                              // against the final primary external ids

    // optional settings
    String correlationTable = 'BIOMART.BIO_MARKER_CORREL_MV'
    String correlationColumn = 'BIO_MARKER_ID' // in $correlationTable

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteriaBuilder) {
        /* call private addToCriteria, but this is necessary. Calling
         * just add() would add the criterion to the root of the criteria,
         * not to any open or() or and() criteria */
        criteriaBuilder.addToCriteria(new CorrelatedBiomarkersCriterion(this))
    }

    class CorrelatedBiomarkersCriterion extends SQLCriterion {

        CorrelatedBiomarkersDataConstraint outer

        CorrelatedBiomarkersCriterion(CorrelatedBiomarkersDataConstraint c) {
            super(
                    "CAST ({alias}.{property} AS VARCHAR(200)) IN (\n" +
                            '   SELECT bm.primary_external_id\n' +
                            '   FROM biomart.bio_marker bm\n' +
                            "       INNER JOIN $c.correlationTable correl\n" +
                            '           ON correl.asso_bio_marker_id = bm.bio_marker_id\n' +
                            '       INNER JOIN biomart.bio_marker bm_orig\n' +
                            "           ON correl.$c.correlationColumn = bm_orig.bio_marker_id\n" +
                            '   WHERE \n' +
                            '       correl.correl_type IN (' + c.correlationTypes.collect { '?' }.join(', ') + ')\n' +
                            '           AND bm_orig.bio_marker_id IN (' +
                            '               ' + c.initialBioMarkerIds.collect { '?' }.join(', ') + ')\n' +
                    ')',
                    (c.correlationTypes + c.initialBioMarkerIds) as Object[],
                    (c.correlationTypes.collect { StringType.INSTANCE } +
                            c.initialBioMarkerIds.collect { LongType.INSTANCE }) as Type[]
            )

            System.err.println "Params: " + (c.correlationTypes + c.initialBioMarkerIds)

            outer = c
        }

        @Override
        String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            Criteria associationCriteria = criteriaQuery.getCriteria(outer.entityAlias)

            // Yikes!
            String propertyColumn = Holders.applicationContext.sessionFactory.
                    getClassMetadata(criteriaQuery.getEntityName(associationCriteria)).
                    getPropertyColumnNames(outer.propertyToRestrict)[0]

            String sqlAlias = criteriaQuery.getSQLAlias(associationCriteria)
            toString().replaceAll(/\{alias\}/, sqlAlias).
                    replaceAll(/\{property\}/, propertyColumn)
        }
    }
}
