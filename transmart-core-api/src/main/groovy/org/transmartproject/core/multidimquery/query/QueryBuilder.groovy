/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery.query

import org.transmartproject.core.multidimquery.MultiDimConstraint

interface QueryBuilder<QueryResult> {

    /**
     * Builds a queryable object for the {@link MultiDimConstraint} object.
     *
     * @param constraint
     * @return the result.
     */
    QueryResult buildCriteria(MultiDimConstraint constraint)

}
