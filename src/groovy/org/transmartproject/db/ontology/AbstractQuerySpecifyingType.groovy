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



package org.transmartproject.db.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.db.i2b2data.PatientDimension

/**
 * Properties that specify queries to be made in other tables. Used by
 * TableAccess and i2b2 metadata tables
 */
abstract class AbstractQuerySpecifyingType implements MetadataSelectQuerySpecification {

    String       factTableColumn
    String       dimensionTableName
    String       columnName
    String       columnDataType
    String       operator
    String       dimensionCode

    def patientSetQueryBuilderService

    def sessionFactory

    /* implements (hopefully improved) transformations described here:
     * https://community.i2b2.org/wiki/display/DevForum/Query+Building+from+Ontology
     */
    String getProcessedDimensionCode() {
        def v = dimensionCode
        if (!v) {
            return v
        }

        if (columnDataType == 'T' && v.length() > 2) {
            if (operator.equalsIgnoreCase('like')) {
                if (v[0] != "'" && !v[0] != '(') {
                    if (v[-1] != '%') {
                        if (v[-1] != '\\') {
                            v += '\\'
                        }
                        v = v.asLikeLiteral() + '%'
                    }
                }
            }

            if (v[0] != "'") {
                v = v.replaceAll(/'/, "''") /* escape single quotes */
                v = "'$v'"
            }

        }

        if (operator.equalsIgnoreCase('in')) {
            v = "($v)"
        }

        v
    }

    static constraints = {
        factTableColumn      nullable:   false,   maxSize:   50
        dimensionTableName   nullable:   false,   maxSize:   50
        columnName           nullable:   false,   maxSize:   50
        columnDataType       nullable:   false,   maxSize:   50
        operator             nullable:   false,   maxSize:   10
        dimensionCode        nullable:   false,   maxSize:   700
    }

    protected List<Patient> getPatients(OntologyTerm term) {

        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items:  [
                                new Item(conceptKey: term.key)
                        ]
                )
        ])

        def patientsSql = patientSetQueryBuilderService.buildPatientIdListQuery(definition)
        def patientsQuery = sessionFactory.currentSession.createSQLQuery patientsSql
        def patientIdList = patientsQuery.list()

        /*
         This is a hack so integration tests work on the h2 schema.
         There is an hibernate issue affecting BIGINT columns that are also identities. see
         http://stackoverflow.com/questions/18758347/hibernate-returns-bigintegers-instead-of-longs
         http://jadimeo.wordpress.com/2009/09/05/sql-bigint-identity-columns-with-hibernate-annotations/
         There are 2 proposed solutions:
         -use Integer instead of Long
         -implement our own IdentityGenerator and use it in this column
         I don't like either, and until we decide on it i will leave it as it is
         */
        if (patientIdList.size() > 0 && patientIdList[0].getClass() != Long) {
            patientIdList = patientIdList.collect( {it as Long} )
        }
        PatientDimension.findAllByIdInList(patientIdList)
    }
}
