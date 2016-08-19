/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.correlations

import grails.orm.HibernateCriteriaBuilder
import grails.util.Holders
import groovy.util.logging.Log4j
import org.hibernate.Criteria
import org.hibernate.HibernateException
import org.hibernate.criterion.CriteriaQuery
import org.hibernate.criterion.SQLCriterion
import org.hibernate.type.LongType
import org.hibernate.type.StringType
import org.hibernate.type.Type
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.search.SearchKeywordCoreDb

/*
 * Search for biomarkers in this fashion:
 * - start with search keywords
 * - search in BIO_MARKER_CORREL_MV/SEARCH_BIO_MKR_CORREL_VIEW for correlations
 *   of the types specified where the found biomarker ids/domain ids (in any case
 *   always the value of SEARCH_KEYWORD.BIO_DATA_ID) are present in the
 *   BIO_MARKER_ID/DOMAIN_OBJECT_ID column of the view
 * - collect resulting associated biomarker ids (ASSO_BIO_MARKER_ID)
 * - go back to bio_marker to find the PRIMARY_EXTERNAL_ID of these new biomarker ids
 */
@Log4j
class CorrelatedBiomarkersDataConstraint implements CriteriaDataConstraint {

    List<SearchKeywordCoreDb> searchKeywords

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
                            '   WHERE \n' +
                            '       correl.correl_type IN (' + c.correlationTypes.collect { '?' }.join(', ') + ')\n' +
                            "           AND correl.$c.correlationColumn IN ( " +
                            '               ' + c.searchKeywords.collect { '?' }.join(', ') + ')\n' +
                    ')',
                    (c.correlationTypes + c.searchKeywords*.bioDataId) as Object[],
                    (c.correlationTypes.collect { StringType.INSTANCE } +
                            c.searchKeywords.collect { LongType.INSTANCE }) as Type[]
            )

            log.debug "Params: ${(c.correlationTypes + c.searchKeywords*.bioDataId)}"

            outer = c
        }

        @Override
        String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            Criteria relevantCriteria =
                    criteriaQuery.getAliasedCriteria(outer.entityAlias)
            if (relevantCriteria == null) {
                throw new HibernateException("Could not find Criteria with " +
                        "alias ${outer.entityAlias}. Available aliases " +
                        "are ${criteriaQuery.aliasCriteriaMap.keySet()}")
            }

            String entityName = criteriaQuery.getEntityName(relevantCriteria)
            if (entityName == null) {
                throw new HibernateException("Could not find entity name " +
                        "for criteria ${relevantCriteria}. Map of criteria " +
                        "entity names is " +
                        relevantCriteria.criteriaEntityNames)
            }

            // Yikes!
            String propertyColumn = Holders.applicationContext.sessionFactory.
                    getClassMetadata(entityName).
                    getPropertyColumnNames(outer.propertyToRestrict)[0]

            String sqlAlias = criteriaQuery.getSQLAlias(relevantCriteria)
            toString().replaceAll(/\{alias\}/, sqlAlias).
                    replaceAll(/\{property\}/, propertyColumn)
        }
    }
}
