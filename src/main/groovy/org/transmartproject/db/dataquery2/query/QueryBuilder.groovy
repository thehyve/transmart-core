package org.transmartproject.db.dataquery2.query

interface QueryBuilder<ResultType> {

    /**
     * Builds a String serialisation of the {@link Constraint} object.
     *
     * @param constraint
     * @return the String serialisation.
     */
    ResultType build(Constraint constraint)

    /**
     * Builds a RenderedQuery serialisation of the {@link Query} object.
     *
     * @param query
     * @return the String serialisation.
     */
    ResultType build(Query query)

    ResultType build(Object obj)

}

