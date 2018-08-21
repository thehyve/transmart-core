/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import org.transmartproject.core.multidimquery.aggregates.CategoricalValueAggregates
import org.transmartproject.core.multidimquery.aggregates.NumericalValueAggregates
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User

interface AggregateDataResource {

    /**
     * Observation and patient counts: counts the number of observations that satisfy the constraint and that
     * the user has access to, and the number of associated patients.
     *
     * @param constraint the constraint.
     * @param user the current user.
     * @return the number of observations and patients.
     */
    Counts counts(Constraint constraint, User user)

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
    Map<String, Counts> countsPerConcept(Constraint constraint, User user)

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
    Map<String, Counts> countsPerStudy(Constraint constraint, User user)

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
    Map<String, Map<String, Counts>> countsPerStudyAndConcept(Constraint constraint, User user)

    Long getDimensionElementsCount(Dimension dimension, Constraint constraint, User user)

    /**
     * Get threshold value, below which counts are not available for users
     * with `COUNTS_WITH_THRESHOLD` access permission.
     *
     * @return threshold value
     */
    Integer getPatientCountsThreshold()

    /**
     * Calculate numerical values aggregates
     *
     * @param constraint specifies from which observations you want to collect values statistics
     * @param user The user whose access rights to consider
     * @return a map where keys are concept keys and values are aggregates
     */
    Map<String, NumericalValueAggregates> numericalValueAggregatesPerConcept(Constraint constraint, User user)

    /**
     * Calculate categorical values aggregates
     *
     * @param constraint specifies from which observations you want to collect values statistics
     * @param user The user whose access rights to consider
     * @return a map where keys are concept keys and values are aggregates
     */
    Map<String, CategoricalValueAggregates> categoricalValueAggregatesPerConcept(Constraint constraint, User user)

}
