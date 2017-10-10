/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

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
     * @param constraint the constraint.
     * @param user the current user.
     * @return the number of observations.
     */
    Long count(MultiDimConstraint constraint, User user)

    Long cachedCount(MultiDimConstraint constraint, User user)

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

    Iterable getDimensionElements(Dimension dimension, MultiDimConstraint constraint, User user)

    QueryResult createPatientSetQueryResult(String name, MultiDimConstraint constraint, User user, String constraintText, String apiVersion)

    QueryResult createObservationSetQueryResult(String name, User user, String constraintText, String apiVersion)

    QueryResult findQueryResult(Long queryResultId, User user)

    MultiDimConstraint createQueryResultsDisjunctionConstraint(List<Long> queryResultIds, User user)

    Iterable<QueryResult> findPatientSetQueryResults(User user)

    Iterable<QueryResult> findObservationSetQueryResults(User user)

    Long getDimensionElementsCount(Dimension dimension, MultiDimConstraint constraint, User user)

    Long cachedPatientCount(MultiDimConstraint constraint, User user)

    /**
     * Retrieve aggregate information
     *
     * @param types the list of aggregates you want
     * @param constraint specifies which observations you want to aggregate
     * @param user The user whose access rights to consider
     * @return a map of aggregates. The keys are the names of the aggregates.
     */
    Map aggregate(List<AggregateType> types, MultiDimConstraint constraint, User user)

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

}
