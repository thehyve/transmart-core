package org.transmartproject.db.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.db.i2b2data.PatientDimension

import org.transmartproject.db.user.User

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
    def databasePortabilityService

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

    @Override
    String postProcessQuery(String sql, User user) {
        sql
    }
}
