/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

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
    Counts counts(MultiDimConstraint constraint, User user)

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

    Long getDimensionElementsCount(Dimension dimension, MultiDimConstraint constraint, User user)

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

}
