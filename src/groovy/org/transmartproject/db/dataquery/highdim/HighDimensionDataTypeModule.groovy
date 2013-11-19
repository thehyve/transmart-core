package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.SessionFactory
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.TabularResult

public interface HighDimensionDataTypeModule {

    /**
     * The name of the data type supported by this module.
     * @return
     */
    String getName()

    /**
     * The session factory used by this module.
     * @return
     */
    SessionFactory getSessionFactory()

    /**
     * A set of assay constraints supported by this data type/
     */
    Set<String> getSupportedAssayConstraints()

    /**
     * A set of data constraints supported by this data type.
     */
    Set<String> getSupportedDataConstraints()

    /**
     * A set of projections supported by this data type,.
     */
    Set<String> getSupportedProjections()

    /**
     * Creates an assay constraint from a name a set of parameters.
     * @param name
     * @param params
     * @return
     */
    AssayConstraint createAssayConstraint(String name, Map<String, Object> params)

    /**
     * Creates a data constraint from a name a set of parameters.
     * @param name
     * @param params
     * @return
     */
    DataConstraint createDataConstraint(String name, Map<String, Object> params)

    /**
     * Creates a projection from a name a set of parameters.
     * @param name
     * @param params
     * @return
     */
    Projection createProjection(String name, Map<String, Object> params)

    /**
     * Prepares the Criteria-based query to be issued. The data constraints will
     * have the opportunity to modify the criteria before it is issued.
     * @param projection
     * @return
     */
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session)

    /**
     * Transform the query result into the final object to be returned by
     * {@link org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource#retrieveData()}.
     */
    TabularResult transformResults(ScrollableResults results,
                                     List<AssayColumn> assays,
                                     Projection projection)
}
