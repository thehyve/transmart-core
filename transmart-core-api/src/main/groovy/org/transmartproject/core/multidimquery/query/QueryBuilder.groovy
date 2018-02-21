/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery.query

interface QueryBuilder<QueryResult> {

    /**
     * Builds a queryable object for the {@link Constraint} object.
     *
     * @param constraint
     * @return the result.
     */
    QueryResult buildCriteria(Constraint constraint)

}
