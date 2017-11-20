package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.HypercubeTabularResultView
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.user.User
import org.transmartproject.rest.serialization.tabular.TabularResultTSVSerializer

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class DataTableController extends AbstractQueryController {

    @Autowired
    MultiDimensionalDataResource multiDimService

    def index() {
        def params = getGetOrPostParams()
        checkForUnsupportedParams(params, ['constraint', 'rows', 'columns'])

        if (params.constraint == null) throw new InvalidArgumentsException("Parameter 'constraint' is required")

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        Constraint constraint = bindConstraint(params.constraint)

        Hypercube hypercube = multiDimService.retrieveClinicalData(constraint, user)
        def rows = []
        if (params.rows) {
            rows = DimensionDescription.findAllByNameInList(params.rows)*.dimension
        }
        def columns = []
        if (params.columns) {
            columns = DimensionDescription.findAllByNameInList(params.columns)*.dimension
        }
        def dataTable = new HypercubeTabularResultView(hypercube, rows, columns)
        TabularResultTSVSerializer.writeValues(dataTable, response.outputStream)
    }
}
