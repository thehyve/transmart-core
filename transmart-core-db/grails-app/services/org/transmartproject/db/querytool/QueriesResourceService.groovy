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

package org.transmartproject.db.querytool

import grails.core.GrailsApplication
import grails.transaction.Transactional
import grails.util.Environment
import grails.util.Holders
import org.hibernate.SessionFactory
import org.hibernate.jdbc.Work
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.*
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

import java.sql.Connection

@Transactional
class QueriesResourceService implements QueriesResource {

    GrailsApplication grailsApplication
    PatientSetQueryBuilderService patientSetQueryBuilderService
    QueryDefinitionXmlService queryDefinitionXmlService
    SessionFactory sessionFactory
    UsersResource usersResource
    LegacyAuthorisationChecks authorisationChecks


    @Override
    @Deprecated
    QueryResult runQuery(QueryDefinition definition) throws InvalidRequestException {
        if (Environment.current.name != 'test') {
            // This functionality is not secured.
            throw new RuntimeException("Functionality is disabled.")
        }
        String username = Holders.config.org.transmartproject.i2b2.user_id
        if (!username) {
            throw new IllegalStateException('org.transmartproject.i2b2.user_id is not specified.')
        }
        User user = usersResource.getUserFromUsername(username)
        runQuery(definition, user)
    }

    @Override
    QueryResult runQuery(QueryDefinition definition, User user) throws InvalidRequestException {
        if (!authorisationChecks.canRun(user, definition)) {
            throw new AccessDeniedException("Denied ${user.username} access " +
                    "for building cohort based on $definition")
        }

        // 1. Populate qt_query_master
        QtQueryMaster queryMaster = new QtQueryMaster(
            name           : definition.name,
            userId         : user.username,
            groupId        : Holders.config.org.transmartproject.i2b2.group_id,
            createDate     : new Date(),
            generatedSql   : null,
            requestXml     : queryDefinitionXmlService.toXml(definition),
            i2b2RequestXml : null,
            requestConstraints  : null,
            apiVersion          : null
        )

        // 2. Populate qt_query_instance
        QtQueryInstance queryInstance = new QtQueryInstance(
                userId       : user.username,
                groupId      : Holders.config.org.transmartproject.i2b2.group_id,
                startDate    : new Date(),
                statusTypeId : QueryStatus.PROCESSING.id,
                queryMaster  : queryMaster,
        )
        queryMaster.addToQueryInstances(queryInstance)

        // 3. Populate qt_query_result_instance
        QtQueryResultInstance resultInstance = new QtQueryResultInstance(
                statusTypeId    : QueryStatus.PROCESSING.id,
                startDate       : new Date(),
                queryInstance   : queryInstance,
                queryResultType : QtQueryResultType.load(QueryResultType.PATIENT_SET_ID)
        )
        queryInstance.addToQueryResults(resultInstance)

        // 4. Save the three objects
        if (!queryMaster.validate()) {
            throw new InvalidRequestException('Could not create a valid ' +
                    'QtQueryMaster: ' + queryMaster.errors)
        }
        if (queryMaster.save() == null) {
            throw new RuntimeException('Failure saving QtQueryMaster')
        }

        // 5. Flush session so objects are inserted & raw SQL can access them
        sessionFactory.currentSession.flush()

        // 6. Build the patient set
        def setSize
        def sql = '<NOT BUILT>'
        try {
            sessionFactory.currentSession.doWork ({ Connection conn ->
                def statement = conn.prepareStatement('SAVEPOINT doWork')
                statement.execute()
            } as Work)

            sql = patientSetQueryBuilderService.buildPatientSetQuery(
                    resultInstance, definition, user)

            sessionFactory.currentSession.doWork ({ Connection conn ->
                def statement = conn.prepareStatement(sql)
                setSize = statement.executeUpdate()

                log.debug "Inserted $setSize rows into qt_patient_set_collection"
            } as Work)
        } catch (InvalidRequestException e) {
            log.error 'Invalid request; rolling back transaction', e
            throw e /* unchecked; rolls back transaction */
        } catch (Exception e) {
            // 6e. Handle error when building/running patient set query
            log.error 'Error running (or building) querytool SQL query, ' +
                    "failing query was '$sql'", e

            // Rollback to save point
            sessionFactory.currentSession.createSQLQuery(
                    'ROLLBACK TO SAVEPOINT doWork').executeUpdate()

            StringWriter sw = new StringWriter()
            e.printStackTrace(new PrintWriter(sw, true))

            resultInstance.setSize = resultInstance.realSetSize = -1
            resultInstance.endDate = new Date()
            resultInstance.statusTypeId = QueryStatus.ERROR.id
            resultInstance.errorMessage = sw.toString()

            queryInstance.endDate = new Date()
            queryInstance.statusTypeId = QueryStatus.ERROR.id
            queryInstance.message = sw.toString()

            if (!resultInstance.save()) {
                log.error("After exception from " +
                        "patientSetQueryBuilderService::buildService, " +
                        "failed saving updated resultInstance and " +
                        "queryInstance")
            }
            return resultInstance
        }

        // 7. Update result instance and query instance
        resultInstance.setSize = resultInstance.realSetSize = setSize
        resultInstance.description = definition.name
        resultInstance.endDate = new Date()
        resultInstance.statusTypeId = QueryStatus.FINISHED.id

        queryInstance.endDate = new Date()
        queryInstance.statusTypeId = QueryStatus.COMPLETED.id

        def newResultInstance = resultInstance.save()
        if (!newResultInstance) {
            throw new RuntimeException('Failure saving resultInstance after ' +
                    'successfully building patient set. Errors: ' +
                    resultInstance.errors)
        }

        // 8. Return result instance
        resultInstance
    }

    QueryResult disableQuery(Long id, User user) throws InvalidRequestException {
        QtQueryResultInstance resultInstance = (QtQueryResultInstance)getQueryResultFromId(id, user)

        resultInstance.deleteFlag = "Y"

        def newResultInstance = resultInstance.save()
        if (!newResultInstance) {
            throw new RuntimeException('Failure disabling resultInstance ' +
                    'Errors: ' +
                    resultInstance.errors)
        }

        resultInstance
    }

    @Override
    QueryResult getQueryResultFromId(Long id, User user) throws NoSuchResourceException {
        QtQueryResultInstance resultInstance = QtQueryResultInstance.findByIdAndDeleteFlag(id, 'N')
        if (!resultInstance || resultInstance.queryInstance.userId != user.username) {
            throw new NoSuchResourceException(
                    "Could not find query result instance with id ${id} and delete_flag = 'N' for user ${user.username}")
        }
        resultInstance
    }

    @Override
    QueryResult getQueryResultFromId(Long id) throws NoSuchResourceException {
        if (Environment.current.name != 'test') {
            // This functionality is not secured.
            throw new RuntimeException("Functionality is disabled.")
        }
        QtQueryResultInstance resultInstance = QtQueryResultInstance.findByIdAndDeleteFlag(id, 'N')
        if (!resultInstance) {
            throw new NoSuchResourceException(
                    "Could not find query result instance with id ${id} and delete_flag = 'N' for user ${user.username}")
        }
        resultInstance
    }

    @Override
    List<QueryResultSummary> getQueryResults(User user) {
        def query = QtQueryResultInstance.where {
            queryInstance.userId == user.username
            deleteFlag == 'N'
        }
        query.collect { it }
    }

    @Override
    QueryDefinition getQueryDefinitionForResult(QueryResult result)
            throws NoSuchResourceException {
        QtQueryResultInstance qtQueryResultInstance = result instanceof QtQueryResultInstance ? result
                : QtQueryResultInstance.get(result.id)

        if (!qtQueryResultInstance) {
            throw new NoSuchResourceException('Could not find definition for query result with id=' + result.id)
        }

        String requestXml = qtQueryResultInstance.queryInstance.queryMaster.requestXml
        queryDefinitionXmlService.fromXml(new StringReader(requestXml))
    }

}
