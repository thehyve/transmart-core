/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.User

interface MultiDimensionalDataResource {

    /**
     * @param accessibleStudies : The studies the current user has access to.
     * @param dataType : The string identifying the data type. "clinical" for clinical data, for high dimensional data
     * the appropriate identifier string.
     * @param constraints : (nullable) A list of Constraint-s. If null, selects all the data in the database.
     * @param dimensions : (nullable) A list of Dimension-s to select. Only dimensions valid for the selected studies
     * will actually be applied. If null, select all available dimensions.
     *
     * Not yet implemented:
     * @param sort
     *
     * @return a Hypercube result
     */
    Hypercube retrieveData(Map args, String dataType, Collection<MDStudy> accessibleStudies)

    Dimension getDimension(String name)

    /**
     * Observation count: counts the number of observations that satisfy the constraint and that
     * the user has access to.
     *
     * Deprecated in favour of {@link #counts(MultiDimConstraint, User)}.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return the number of observations.
     */
    @Deprecated
    Long count(MultiDimConstraint constraint, User user)

    /**
     * Observation and patient counts: counts the number of observations that satisfy the constraint and that
     * the user has access to, and the number of associated patients.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return the number of observations and patients.
     */
    Counts counts(MultiDimConstraint constraint, User user)

    /**
     * Computes observations and patient counts for all data accessible by the user
     * (applying the 'true' constraint) and puts the result in the counts cache.
     *
     * @param user the user to compute the counts for.
     */
    void rebuildCountsCacheForUser(User user)

    /**
     * Observation and patient counts per concept:
     * counts the number of observations that satisfy the constraint and that
     * the user has access to, and the number of associated patients,
     * and groups them by concept code.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return a map from concept code to the counts.
     */
    Map<String, Counts> countsPerConcept(MultiDimConstraint constraint, User user)

    /**
     * Observation and patient counts per study:
     * counts the number of observations that satisfy the constraint and that
     * the user has access to, and the number of associated patients,
     * and groups them by study id.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return a map from study id to the counts.
     */
    Map<String, Counts> countsPerStudy(MultiDimConstraint constraint, User user)

    /**
     * Observation and patient counts per study and concept:
     * counts the number of observations that satisfy the constraint and that
     * the user has access to, and the number of associated patients,
     * and groups them by first study id and then concept code.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return a map from study id to maps from concept code to the counts.
     */
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(MultiDimConstraint constraint, User user)

    /**
     * Computes counts per study and concept for all data accessible for all users
     * and puts the result in the counts cache.
     */
    void rebuildCountsPerStudyAndConceptCache()

    Iterable getDimensionElements(Dimension dimension, MultiDimConstraint constraint, User user)

    QueryResult createPatientSetQueryResult(String name, MultiDimConstraint constraint, User user, String constraintText, String apiVersion)

    /**
     * Update existing patient set with new patient ids
     * @param queryResultId
     * @param name
     * @param patients
     * @param user
     * @return A patient set with updated results (patient ids)
     */
    QueryResult updatePatientSetQueryResult(Long queryResultId, String name, List<Patient> patients, User user)

    QueryResult findQueryResultById(Long queryResultId, User user)

    QueryResult findQueryResultByConstraint(String constraintText, User user)

    Iterable<QueryResult> findPatientSetQueryResults(User user)

    Long getDimensionElementsCount(Dimension dimension, MultiDimConstraint constraint, User user)

    /**
     * Patient count: counts the number of patients that satisfy the constraint and that
     * the user has access to.
     *
     * Deprecated in favour of {@link #counts(MultiDimConstraint, User)}.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return the number of patients.
     */
    @Deprecated
    Long cachedPatientCount(MultiDimConstraint constraint, User user)

    /**
     * Calculate numerical values aggregates
     *
     * @param constraint specifies from which observations you want to collect values statistics
     * @param user The user whose access rights to consider
     * @return a map where keys are concept keys and values are aggregates
     */
    Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept(MultiDimConstraint constraint, User user)

    /**
     * Calculate categorical values aggregates
     *
     * @param constraint specifies from which observations you want to collect values statistics
     * @param user The user whose access rights to consider
     * @return a map where keys are concept keys and values are aggregates
     */
    Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept(MultiDimConstraint constraint, User user)

    Hypercube highDimension(
            MultiDimConstraint assayConstraint_,
            MultiDimConstraint biomarkerConstraint,
            String projectionName,
            User user,
            String type)

    Hypercube retrieveClinicalData(MultiDimConstraint constraint, User user)

    Hypercube retrieveClinicalData(MultiDimConstraint constraint, User user, List<Dimension> orderByDimensions)

    List<String> retrieveHighDimDataTypes(MultiDimConstraint assayConstraint, User user)

    Iterable<Dimension> getSupportedDimensions(MultiDimConstraint constraint)

    /**
     * Clears the counts cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    void clearCountsCache()

    /**
     * Clears the patient count cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    void clearPatientCountCache()

    /**
     * Clears the counts per concept cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    void clearCountsPerConceptCache()

    /**
     * Clears the counts per study and concept cache. This function should be called after loading, removing or updating
     * observations in the database.
     */
    void clearCountsPerStudyAndConceptCache()

}
