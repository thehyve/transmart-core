package org.transmartproject.db.clinical

import grails.orm.HibernateCriteriaBuilder
import grails.transaction.Transactional
import grails.util.Holders
import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.hibernate.Criteria
import org.hibernate.criterion.*
import org.hibernate.internal.CriteriaImpl
import org.hibernate.internal.StatelessSessionImpl
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.db.querytool.*
import org.transmartproject.db.support.ParallelPatientSetTaskService
import org.transmartproject.db.util.HibernateUtils

import java.util.function.Function

@CompileStatic
class PatientSetService extends AbstractDataResourceService implements PatientSetResource {

    public static final String QUERY_RESULT_IDS_CACHE = "${PatientSetService.name}.QUERY_RESULT_IDS_CACHE"

    @Autowired
    ParallelPatientSetTaskService parallelPatientSetTaskService

    @Autowired
    CacheManager cacheManager

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate

    @Transactional(readOnly = true)
    @CompileDynamic
    QtQueryResultType getPatientSetResultType() {
        QtQueryResultType.load(QueryResultType.PATIENT_SET_ID)
    }

    /**
     * Synchronously executes the query and tracks its progress by saving the status.
     *
     * @param name meaningful text for the user to find back the query results.
     * @param user user that executes the query
     * @param constraint the constraint to base the patient set on.
     * @param apiVersion v1 or v2
     * @param reusePatientSet whether to allow the implementation to return an existing patient set for the same query.
     * @param queryExecutor closure that actually executes the query.
     * It gets the query result object and returns the number of the result set.
     * It also responsible for saving all actual results.
     * @return the query result object that does not contains result as such but rather status of execution
     * and result instance id you might use to fetch actual results.
     */
    QtQueryResultInstance createQueryResult(String name,
                                            User user,
                                            Constraint constraint,
                                            String apiVersion,
                                            boolean reusePatientSet,
                                            Function<PatientSetDefinition, Long> queryExecutor) {

        QtQueryMaster queryMaster = createQueryMaster(user, constraint, name, apiVersion)
        queryMaster.save(failOnError: true)
        log.debug("Saved a query master ${queryMaster}.")

        QtQueryInstance queryInstance = createQueryInstance(user, queryMaster)
        queryInstance.save(failOnError: true)
        log.debug("Saved a query instance ${queryInstance}")

        QtQueryResultInstance resultInstance = createQueryResultInstance(queryInstance, name)
        resultInstance.save(failOnError: true, flush: true)
        log.debug("Saved a query result instance ${resultInstance}")

        try {
            log.info("Execute query ${constraint}")
            resultInstance.setSize = queryExecutor.apply(
                    new PatientSetDefinition(resultInstance, constraint, user, apiVersion, reusePatientSet))

            log.info('Query execution has finished successfully')
            resultInstance.endDate = new Date()
            resultInstance.statusTypeId = (short) QueryStatus.FINISHED.id
            queryInstance.endDate = new Date()
            queryInstance.statusTypeId = QueryStatus.FINISHED.id
            putQueryResultIdToCache(user, constraint, resultInstance.id)
        } catch (Throwable t) {
            log.error('Query execution has failed', t)
            resultInstance.setSize = resultInstance.realSetSize = -1L
            resultInstance.errorMessage = t.message
            resultInstance.endDate = new Date()
            resultInstance.statusTypeId = (short) QueryStatus.ERROR.id
            queryInstance.endDate = new Date()
            queryInstance.statusTypeId = QueryStatus.ERROR.id
        }
        resultInstance
    }

    protected QtQueryMaster createQueryMaster(User user, Constraint constraint, String name, String apiVersion) {
        log.debug("Create a query master for ${user}.")
        def queryMaster = new QtQueryMaster()
        queryMaster.name = name
        queryMaster.userId = user.username
        queryMaster.groupId = Holders.config.getProperty('org.transmartproject.i2b2.group_id', String)
        queryMaster.createDate = new Date()
        queryMaster.requestConstraints = constraint.toJson()
        queryMaster.apiVersion = apiVersion
        return queryMaster
    }

    protected QtQueryInstance createQueryInstance(User user, QtQueryMaster queryMaster) {
        log.debug("Create a query instance for ${queryMaster} master object.")
        def queryInstance = new QtQueryInstance()
        queryInstance.userId = user.username
        queryInstance.groupId = Holders.config.getProperty('org.transmartproject.i2b2.group_id', String)
        queryInstance.startDate = new Date()
        queryInstance.statusTypeId = QueryStatus.PROCESSING.id
        queryInstance.queryMaster = queryMaster
        queryMaster.addToQueryInstances(queryInstance)
        return queryInstance
    }

    protected QtQueryResultInstance createQueryResultInstance(QtQueryInstance queryInstance, String name) {
        log.debug("Create a query result instance for ${queryInstance} query instance.")
        def resultInstance = new QtQueryResultInstance()
        resultInstance.statusTypeId = (short) QueryStatus.PROCESSING.id
        resultInstance.startDate = new Date()
        resultInstance.queryInstance = queryInstance
        resultInstance.queryResultType = patientSetResultType
        resultInstance.description = name
        return resultInstance
    }

    /**
     * Find a query result based on a constraint and user in the cache.
     *
     * @param user the creator of the query result.
     * @param constraint the constraint used in the lookup.
     * @return the query result if it exists in the cache and the database; null otherwise.
     */
    QueryResult findFinishedQueryResultInCacheBy(User user,
                                                 Constraint constraint) {
        Long queryResultId = getQueryResultIdFromCache(user, constraint)
        if (log.debugEnabled) {
            log.debug("Get query result ${queryResultId} for ${user} and ${constraint}")
        }
        if (queryResultId) {
            QtQueryResultInstance queryResult = QtQueryResultInstance.get(queryResultId)
            boolean usable = isQueryResultUsable(queryResult)
            if (log.debugEnabled) {
                log.debug("Got ${queryResult} query result by ${queryResultId} id with usability calculated to ${usable}")
            }
            if (usable) {
                return queryResult
            }
        }
        return null
    }

    /**
     * Tries to reuse query result that satisfy provided constraint for the user before creating it.
     *
     * @return A new one or reused query result.
     */
    QueryResult createOrReuseQueryResult(String name,
                                         User user,
                                         Constraint constraint,
                                         String apiVersion,
                                         boolean reusePatientSet,
                                         Function<PatientSetDefinition, Long> queryExecutor) {
        log.info "Create or reuse patient set ..."
        if (constraint instanceof SubSelectionConstraint && constraint.dimension == 'patient') {
            log.info "Flattening subselection constraint."
            return createOrReuseQueryResult(name, user, constraint.constraint, apiVersion, reusePatientSet, queryExecutor)
        } else if (constraint instanceof MultipleSubSelectionsConstraint
                && constraint.dimension == 'patient' && constraint.args.size() == 1) {
            log.info "Flattening singleton multiple subselections constraint."
            return createOrReuseQueryResult(name, user, constraint.args[0], apiVersion, reusePatientSet, queryExecutor)
        }

        QueryResult result = null
        if (reusePatientSet) {
            result = findFinishedQueryResultInCacheBy(user, constraint)
        }
        if (result == null) {
            result = createQueryResult(name, user, constraint, apiVersion, reusePatientSet, queryExecutor)
        }
        if (result.status != QueryStatus.FINISHED) {
            throw new UnexpectedResultException("Query has ${result.status} status.")
        }
        result
    }

    /**
     * Function for creating a patient set consisting of patients for which there are observations
     * that are specified by <code>query</code>.
     *
     * @param name a name for the patient set
     * @param constraint the constraint used for querying patients
     * @param user the current user
     * @param apiVersion the API version used at time of patient set creation
     * @param reusePatientSet whether to allow the implementation to return an existing patient set for the same query.
     */
    @Override
    @Transactional
    QueryResult createPatientSetQueryResult(String name,
                                            Constraint constraint,
                                            User user,
                                            String apiVersion,
                                            boolean reusePatientSet) {
        checkAccess(constraint, user, PatientDataAccessLevel.minimalAccessLevel)

        createOrReuseQueryResult(
                name,
                user,
                constraint,
                apiVersion,
                reusePatientSet,
                this.&populatePatientSetQueryResult
        )
    }

    @Canonical
    @CompileStatic
    static class PatientIdListWrapper {
        List<Long> patientIds
    }

    private List<PatientIdListWrapper> getPatientIdsTask(ParallelPatientSetTaskService.SubtaskParameters parameters) {
        log.info "Task ${parameters.task}"
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
            CriteriaImpl criteria = (CriteriaImpl) q.instance

            HibernateCriteriaQueryBuilder builder = getCheckedQueryBuilder(parameters.user, PatientDataAccessLevel.minimalAccessLevel)

            criteria.add(HibernateCriteriaQueryBuilder.defaultModifierCriterion)
            criteria.setProjection(Projections.projectionList().add(
                    Projections.distinct(Projections.property('patient.id')), 'patientId'))
            criteria.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
            builder.applyToCriteria(criteria, [parameters.constraint])

            def t1 = new Date()

            def result = criteria.list().collect { it -> (Long)((Map)it).patientId } as List<Long>

            def t2 = new Date()
            log.info "Task ${parameters.task}: fetched ${result.size()} patients. (took ${t2.time - t1.time} ms.)"
            return [new PatientIdListWrapper(result)]
        } finally {
            session.close()
        }
    }

    private Integer insertPatientsToQueryResult(QueryResult queryResult, Collection<Long> patientIds) {
        def t1 = new Date()
        def session = (StatelessSessionImpl)sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            def tx = session.beginTransaction()
            int insertCount = 0
            int i = 0
            def statement = session.connection().prepareStatement(
                    'insert into i2b2demodata.qt_patient_set_collection (patient_num, result_instance_id) values (?, ?)')
            for (Long patientId : patientIds) {
                statement.setLong(1, patientId)
                statement.setLong(2, queryResult.id)
                statement.addBatch()
                i++
                if ((i % 500) == 0) {
                    log.debug 'Inserting batch ...'
                    int[] insertCounts = statement.executeBatch()
                    insertCount += insertCounts.sum()
                }
            }
            log.debug 'Inserting final batch ...'
            int[] insertCounts = statement.executeBatch()
            insertCount += insertCounts.sum()
            tx.commit()
            def t2 = new Date()
            log.info "Patient set inserted. (took ${t2.time - t1.time} ms.)"
            insertCount
        } finally {
            session.close()
        }
    }

    private Constraint findAndPersistSubquery(Constraint constraint, User user, String apiVersion) {
        if (constraint instanceof SubSelectionConstraint) {
            def subSelect = ((SubSelectionConstraint)constraint)
            if (subSelect.dimension == 'patient') {
                log.info "Creating new patient set ..."
                QueryResult subQueryResult = createPatientSetQueryResult('temp',
                        subSelect.constraint, user, apiVersion, true)
                def result = new PatientSetConstraint(subQueryResult.id)
                log.debug "Result: ${result.toJson()}"
                return result
            }
        } else if (constraint instanceof Negation) {
            def arg = ((Negation)constraint).arg
            if (arg instanceof SubSelectionConstraint) {
                def subSelect = ((SubSelectionConstraint)arg)
                if (subSelect.dimension == 'patient') {
                    log.info "Creating new patient set ..."
                    QueryResult subQueryResult = createPatientSetQueryResult('temp',
                            subSelect.constraint, user, apiVersion, true)
                    def result = new Negation(new PatientSetConstraint(subQueryResult.id))
                    log.debug "Result: ${result.toJson()}"
                    return result
                }
            }
        }
        null
    }

    @CompileStatic
    static class PersistSubqueriesResult {
        private boolean patientSetsOnly
        private MultipleSubSelectionsConstraint patientSetsConstraint
        private Constraint constraint

        private PersistSubqueriesResult(
                boolean patientSetsOnly, MultipleSubSelectionsConstraint patientSetsConstraint, Constraint constraint) {
            this.patientSetsOnly = patientSetsOnly
            this.patientSetsConstraint = patientSetsConstraint
            this.constraint = constraint
        }

        Constraint getConstraint() {
            if (patientSetsOnly) {
                throw new UnexpectedResultException('Method not allowed')
            }
            constraint
        }

        MultipleSubSelectionsConstraint getPatientSetsConstraint() {
            if (!patientSetsOnly) {
                throw new UnexpectedResultException('Method not allowed')
            }
            patientSetsConstraint
        }

        static PersistSubqueriesResult patientSets(MultipleSubSelectionsConstraint constraint) {
            new PersistSubqueriesResult(true, constraint, null)
        }

        static PersistSubqueriesResult notPatientSets(Constraint constraint) {
            new PersistSubqueriesResult(false, null, constraint)
        }
    }

    private PersistSubqueriesResult persistPatientSubqueries(Constraint constraint, User user, String apiVersion) {
        List<Constraint> subQueries = []
        List<Constraint> topQueryParts = []

        def query = findAndPersistSubquery(constraint, user, apiVersion)
        if (query) {
            log.info "Query is a subquery."
            return PersistSubqueriesResult.patientSets(new MultipleSubSelectionsConstraint('patient', Operator.OR, [query]))
        }

        if (!(constraint instanceof Combination)) {
            log.info "Query is not a combination."
            return PersistSubqueriesResult.notPatientSets(constraint)
        }
        def combination = (Combination)constraint
        def operator = combination.operator
        log.info "Query with operator ${operator.symbol} and ${combination.args.size()} arguments."
        for (Constraint part: combination.args) {
            def subQuery = findAndPersistSubquery(part, user, apiVersion)
            if (subQuery) {
                // subquery or negated subquery found, replaced with patient set
                subQueries.add(subQuery)
                log.info "Argument is a subquery and has been persisted."
            } else if (operator == Operator.OR && part instanceof StudyNameConstraint) {
                // study constraint found in a (patient query level) disjunction, create patient set for study
                log.info "Creating new patient set for constraint ..."
                QueryResult studySubset = createPatientSetQueryResult('temp',
                        part, user, apiVersion, true)
                def studySubsetConstraint = new PatientSetConstraint(studySubset.id)
                subQueries.add(studySubsetConstraint)
            } else {
                // recursive call, to support nested subqueries
                def partResult = persistPatientSubqueries(part, user, apiVersion)
                if (partResult.patientSetsOnly) {
                    log.info "Argument is a nested subquery."
                    subQueries.add(partResult.patientSetsConstraint)
                } else {
                    log.info "Argument is not a subquery."
                    topQueryParts.add(partResult.constraint)
                }
            }
        }
        Constraint topQuery
        Operator setOperator
        switch (operator) {
            case Operator.AND:
                topQuery = new AndConstraint(topQueryParts)
                setOperator = Operator.INTERSECT
                break
            case Operator.OR:
                topQuery = new OrConstraint(topQueryParts)
                setOperator = Operator.UNION
                break
            default:
                throw new InvalidQueryException("Unexpected operator ${operator.symbol}.")
        }
        if (!subQueries.empty) {
            if (!topQueryParts.empty) {
                QueryResult topQueryResult = createPatientSetQueryResult('temp',
                        topQuery, user, apiVersion, true)
                subQueries.add(new PatientSetConstraint(topQueryResult.id))
            }
            def result = new MultipleSubSelectionsConstraint('patient', setOperator, subQueries)
            log.info "Subqueries replaced by patient sets."
            return PersistSubqueriesResult.patientSets(result)
        }
        log.info "No subqueries found, returning constraint."
        return PersistSubqueriesResult.notPatientSets(constraint)
    }

    /**
     *
     * @param property
     * @param constraint
     * @return
     */
    public Criterion buildSubselectCriterion(String property, Constraint constraint) {
        if (constraint instanceof PatientSetConstraint) {
            log.info "Build patient set constraint for patientSetId ${((PatientSetConstraint)constraint).patientSetId}"
            DetachedCriteria subCriteria = DetachedCriteria.forClass(QtPatientSetCollection, 'qt_patient_set_collection')
            subCriteria.add(Restrictions.eq('resultInstance.id', ((PatientSetConstraint)constraint).patientSetId))
            Subqueries.propertyIn(property, subCriteria.setProjection(Projections.property('patient')))
        } else if (constraint instanceof Negation) {
            log.info "Build negation constraint"
            return Restrictions.not(buildSubselectCriterion(property, ((Negation)constraint).arg))
        } else if (constraint instanceof MultipleSubSelectionsConstraint) {
            def subSelections = (MultipleSubSelectionsConstraint)constraint
            assert subSelections.dimension == 'patient': "Dimension not supported: ${subSelections.dimension}"
            if (subSelections.args.empty) {
                throw new QueryBuilderException('Empty list of subselection constraints.')
            }
            log.info "Build subselection constraints (${constraint.operator}, ${constraint.args.size()})"
            Criterion criterion = buildSubselectCriterion(property, constraint.args.head())
            List<Constraint> tail = constraint.args.tail()
            if (!tail.empty) {
                def tailConstraint = new MultipleSubSelectionsConstraint('patient', constraint.operator, tail)
                def tailCriterion = buildSubselectCriterion(property, tailConstraint)
                switch(constraint.operator) {
                    case Operator.INTERSECT:
                        criterion = Restrictions.and(criterion, tailCriterion)
                        break
                    case Operator.UNION:
                        criterion = Restrictions.or(criterion, tailCriterion)
                        break
                    default:
                        throw new QueryBuilderException("Operator not supported: ${constraint.operator.name()}")
                }
            }
            return criterion
        } else {
            throw new InvalidQueryException("Query type not supported: ${constraint.class.simpleName}.")
        }
    }


    /**
     * Create a query for the combination using union, intersect:
     * using {@link HibernateCriteriaQueryBuilder#build(MultipleSubSelectionsConstraint)}.
     */
    private List<Long> getPatientIdsFromSubselections(MultipleSubSelectionsConstraint constraint) {
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false
            HibernateCriteriaBuilder q = HibernateUtils.createCriteriaBuilder(PatientDimension, 'patient_dimension', session)
            CriteriaImpl criteria = (CriteriaImpl) q.instance

            // Criterion of the form 'patient' in (select ...)
            // Can be used to e.g., get all from patient dimension that satisfy this criterion.
            Criterion subselectCriterion = buildSubselectCriterion('id', constraint)

            criteria.add(subselectCriterion)
            criteria.setProjection(Projections.projectionList().add(
                    Projections.id()))

            def t1 = new Date()
            log.info "Querying patient ids ..."

            def result = criteria.list() as List<Long>

            def t2 = new Date()
            log.info "Found ${result.size()} patients (took ${t2.time - t1.time} ms.)"

            result
        } finally {
            session.close()
        }
    }

    /**
     * Find the intersection between a patient set and a list of subject ids.
     */
    private List<Long> getPatientIdsFromSubjectIds(Collection<String> subjectIds, QueryResult patientSet) {
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        try {
            session.connection().autoCommit = false

            def t1 = new Date()
            // create a temporary table
            def tx = session.beginTransaction()
            def tempTableDdl = session.connection().prepareStatement(
                    'create temporary table subject_ids(patient_ide varchar(200) not null)')
            tempTableDdl.execute()

            // populate temporary table with subject identifiers
            int insertCount = 0
            int i = 0
            def statement = session.connection().prepareStatement(
                    'insert into subject_ids (patient_ide) values (?)')
            for (String subjectId : subjectIds) {
                statement.setString(1, subjectId)
                statement.addBatch()
                i++
                if ((i % 500) == 0) {
                    log.debug 'Inserting batch ...'
                    int[] insertCounts = statement.executeBatch()
                    insertCount += insertCounts.sum()
                }
            }
            log.debug 'Inserting final batch ...'
            int[] insertCounts = statement.executeBatch()
            insertCount += insertCounts.sum()

            def t2 = new Date()
            log.info "Temporary table created, ${insertCount} subjects inserted. (took ${t2.time - t1.time} ms.)"

            // compute intersection between patient set and temporary table
            List<Long> result = []
            def query = session.connection().prepareStatement(
                    '''select p.patient_num as patient_num from i2b2demodata.qt_patient_set_collection p
                      where p.result_instance_id = ?
                      and p.patient_num in (
                          select pm.patient_num
                          from patient_mapping pm
                          inner join subject_ids si on pm.patient_ide = si.patient_ide)''')
            query.setLong(1, patientSet.id)
            def rs = query.executeQuery()
            while (rs.next()) {
                result.add(rs.getLong('patient_num'))
            }
            tx.commit()
            def t3 = new Date()
            log.info "Patient list fetched: ${result.size()} patients. (took ${t3.time - t2.time} ms.)"
            result
        } finally {
            session.close()
        }
    }

    @CompileStatic
    @Canonical
    static class PatientSetDefinition {
        QueryResult queryResult
        Constraint constraint
        User user
        String apiVersion
        boolean reusePatientSets
    }

    /**
     * Populates given query result with patient set that satisfy provided constraints with regards with user access rights.
     * @param queryResult query result to populate with patients
     * @param constraint constraint to get results that satisfy it
     * @param user user for whom to execute the patient set query. Result will depend on the user access rights.
     * @param apiVersion the API version
     * @return Number of patients inserted in the patient set
     */
    private Integer populatePatientSetQueryResult(PatientSetDefinition patientSetDefinition) {
        def queryResult = patientSetDefinition.queryResult
        def constraint = patientSetDefinition.constraint
        def user = patientSetDefinition.user
        def apiVersion = patientSetDefinition.apiVersion
        def reusePatientSets = patientSetDefinition.reusePatientSets

        assert queryResult
        assert queryResult.id

        if (reusePatientSets) {
            Map<String, Long> userToQueryResultId = getUsernameToQueryResultIdFromCache(user, constraint)
            if (userToQueryResultId) {
                for (Map.Entry<String, Long> usernameToPatientSetIdEntry: userToQueryResultId.entrySet()) {
                    if (usernameToPatientSetIdEntry.key != user.username) {
                        Long otherUserPatientSetId = usernameToPatientSetIdEntry.value
                        boolean patientSetUsable = isQueryResultUsable(QtQueryResultInstance.get(otherUserPatientSetId))
                        if (log.debugEnabled) {
                            log.debug("Found patient set ${otherUserPatientSetId} of ${user} user with usability calculated to ${patientSetUsable}.")
                        }
                        if (patientSetUsable) {
                            return copyPatientSet(otherUserPatientSetId, queryResult.id)
                        }
                    }
                }
            }
        }

        if (constraint instanceof TrueConstraint) {
            return materializePatientSet(queryResult, constraint, user)
        } else {
            /**
             * - First split into multiple subselects if of the form (subselect() op subselect()) with op in {'or', 'and'}
             * - Create patient sets for each of the subselects
             */
            boolean usePatientSets = false
            PersistSubqueriesResult persistSubqueriesResult = null
            if (reusePatientSets) {
                log.info "Check if the query can be split into subqueries ..."
                persistSubqueriesResult = persistPatientSubqueries(constraint, user, apiVersion)
                usePatientSets = persistSubqueriesResult.patientSetsOnly
            }
            if (usePatientSets) {
                // Create a patient set as a combination of patient subsets.
                log.info "Fetch patients based on patient subsets ..."
                def patientIds = getPatientIdsFromSubselections(persistSubqueriesResult.patientSetsConstraint)
                insertPatientsToQueryResult(queryResult, patientIds)
            } else {
                // Base case: executing the leaf query on observation_fact or patient id list.
                def t1 = new Date()
                log.info "Start patient set creation ..."
                List<Long> patientIds
                if (reusePatientSets && constraint instanceof PatientSetConstraint && ((PatientSetConstraint)constraint).subjectIds != null) {
                    // Find or create the set of all patients
                    QueryResult allPatientsResult = createPatientSetQueryResult('All subjects',
                            new TrueConstraint(), user, apiVersion, true)
                    log.info "Computing intersection of set of all subjects and list of subject ids ..."
                    patientIds = getPatientIdsFromSubjectIds(((PatientSetConstraint)constraint).subjectIds, allPatientsResult)
                } else {
                    log.info "Executing query on observation_fact ..."
                    def taskParameters = new ParallelPatientSetTaskService.TaskParameters(constraint, user)
                    patientIds = parallelPatientSetTaskService.run(
                            taskParameters,
                            { ParallelPatientSetTaskService.SubtaskParameters params ->
                                getPatientIdsTask(params)
                            },
                            { List<PatientIdListWrapper> patientIdLists ->
                                patientIdLists.collectMany { it.patientIds } as List<Long>
                            }
                    )
                }
                def t2 = new Date()
                log.info "Fetched ${patientIds.size()} patients (took ${t2.time - t1.time} ms.)."
                insertPatientsToQueryResult(queryResult, patientIds)
            }
        }
    }

    protected Integer copyPatientSet(Long fromPatientSetId, Long toPatientSetId) {
        if (log.debugEnabled) {
            log.debug("Copying patients from patient set (id = ${fromPatientSetId}) to patient set (id = ${toPatientSetId}) with insert-select sql.")
        }
        int records = namedParameterJdbcTemplate.update('''
          insert into i2b2demodata.qt_patient_set_collection(patient_num, result_instance_id)
          select from_ps.patient_num, :to_result_instance_id
          from i2b2demodata.qt_patient_set_collection from_ps
          where from_ps.result_instance_id = :from_result_instance_id
        ''',
                [
                        from_result_instance_id: fromPatientSetId,
                        to_result_instance_id  : toPatientSetId,
                ])
        if (log.debugEnabled) {
            log.debug("Copied ${records} rows to patient set (id = ${toPatientSetId}).")
        }
        return records
    }

    protected int materializePatientSet(QueryResult queryResult, Constraint constraint, User user) {
        log.info "Saving patient set for the True constraint."
        DetachedCriteria patientSetDetachedCriteria = getCheckedQueryBuilder(user, PatientDataAccessLevel.minimalAccessLevel)
                .buildCriteria(constraint)
                .setProjection(
                Projections.projectionList()
                        .add(Projections.distinct(Projections.property('patient.id')), 'pid')
                        .add(Projections.sqlProjection("${queryResult.id} as rid", ['rid'] as String[],
                        [StandardBasicTypes.LONG] as Type[])))

        Criteria patientSetCriteria = getExecutableCriteria(patientSetDetachedCriteria)
        return HibernateUtils
                .insertResultToTable(QtPatientSetCollection, ['patient.id', 'resultInstance.id'], patientSetCriteria)
    }

    /**
     * Find a query result based on a result instance id.
     *
     * @param queryResultId the result instance id of the query result.
     * @param user the creator of the query result.
     * @return the query result if it exists.
     * @throws org.transmartproject.core.exceptions.NoSuchResourceException iff the query result does not exist.
     */
    @Override
    @Transactional(readOnly = true)
    QueryResult findQueryResult(Long queryResultId, User user) {
        def criteria = DetachedCriteria.forClass(QtQueryResultInstance.class, 'qri')
                .createCriteria('qri.queryInstance', 'qi')
                .createCriteria('qi.queryMaster', 'qm')
                .add(Restrictions.eq('qri.queryResultType.id', QueryResultType.PATIENT_SET_ID))
                .add(Restrictions.eq('qri.deleteFlag', 'N'))
                .add(Restrictions.eq('qri.statusTypeId', (short)QueryStatus.FINISHED.id))
                .add(Restrictions.eq('qi.userId', user.username))
                .add(Restrictions.eq('qi.deleteFlag', 'N'))
                .add(Restrictions.eq('qm.deleteFlag', 'N'))
                .add(Restrictions.eq('qri.id', queryResultId))
        def queryResult = getFirst(criteria) as QtQueryResultInstance
        if (queryResult == null) {
            throw new NoSuchResourceException("Patient set not found with id ${queryResultId} for user ${user.username}.")
        }
        queryResult
    }

    /**
     * Retrieves all query results for a user.
     *
     * @param user the user to retrieve query results for.
     * @return the query result objects.
     */
    @Override
    Iterable<QueryResult> findPatientSetQueryResults(User user) {
        def criteria = DetachedCriteria.forClass(QtQueryResultInstance.class, 'qri')
                .createCriteria('qri.queryInstance', 'qi')
                .createCriteria('qi.queryMaster', 'qm')
                .add(Restrictions.eq('qri.queryResultType.id', QueryResultType.PATIENT_SET_ID))
                .add(Restrictions.eq('qri.deleteFlag', 'N'))
                .add(Restrictions.eq('qri.statusTypeId', (short)QueryStatus.FINISHED.id))
                .add(Restrictions.eq('qi.userId', user.username))
                .add(Restrictions.eq('qi.deleteFlag', 'N'))
                .add(Restrictions.eq('qm.deleteFlag', 'N'))
        return getIterable(criteria)
    }

    /**
     * Clear  query result ids cache.
     * This function should be called after data loading.
     */
    void clearPatientSetIdsCache() {
        log.info 'Clearing patient set ids cache ...'
        getQueryResultIdsCache().clear()
    }

    /**
     * Get's query result id (aka patient set id) for the given user and constraint from the cache.
     * The cache meant to be cleaned after the data loading. {@link this.clearPatientSetIdsCache()}
     * @param user user result belongs to
     * @param constraint constraint used in the query
     * @return query result id (aka patient set id)
     */
    protected Long getQueryResultIdFromCache(User user, Constraint constraint) {
        def key = calculateCacheKey(user, constraint)
        Long id = (Long) getQueryResultIdsCache().get(key, Map)?.get(user.username)
        if (log.debugEnabled) {
            log.debug("Get query result instance ${id} by ${key} key for user ${user.username}.")
        }
        return id
    }

    protected Map<String, Long> getUsernameToQueryResultIdFromCache(User user, Constraint constraint) {
        def key = calculateCacheKey(user, constraint)
        Map<String, Long> userToQueryResultId = (Map<String, Long>) getQueryResultIdsCache().get(key, Map)
        if (log.debugEnabled) {
            log.debug("Get users to query userToQueryResultId instance map (${userToQueryResultId}) by ${key} key for user ${user.username}.")
        }
        return userToQueryResultId
    }

    /**
     * Put a query result id to the cache
     * @param user defines the cache key
     * @param constraint defines the cache key
     * @param id query result id
     */
    protected void putQueryResultIdToCache(User user, Constraint constraint, Long id) {
        def key = calculateCacheKey(user, constraint)
        if (log.debugEnabled) {
            log.debug("Put query result instance ${id} to ${key} key for user ${user.username}.")
        }
        getQueryResultIdsCache().put(key, [(user.username): id])
    }

    private boolean isQueryResultUsable(QtQueryResultInstance queryResult) {
        queryResult && queryResult.status == QueryStatus.FINISHED && queryResult.deleteFlag == 'N'
    }

    private Object calculateCacheKey(User user, Constraint constraint) {
        [user.admin, user.studyToPatientDataAccessLevel, constraint.toJson()]
    }

    private Cache getQueryResultIdsCache() {
        cacheManager.getCache(QUERY_RESULT_IDS_CACHE)
    }

}
