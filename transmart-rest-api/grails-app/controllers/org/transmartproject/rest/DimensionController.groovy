package org.transmartproject.rest

import com.google.common.base.Strings
import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.user.User

@Slf4j
class DimensionController extends AbstractQueryController {

    @Autowired
    AccessControlChecks accessControlChecks
    
    static responseFormats = ['json', 'hal']
    
    /**
     * Dimension endpoint:
     * <code>GET /v2/dimension/${dimensionName}/elements</code>
     *
     * @return a list of all dimension elements that user has access to.
     */
    def list(@PathVariable('dimensionName') String dimensionName) {

        checkParams(params, ['dimensionName', 'constraint'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def dimension = DimensionImpl.fromNameOrNull(dimensionName)
        verifyDimension(Collections.singleton(dimension), dimensionName, user)

        Constraint constraint = Strings.isNullOrEmpty(params.constraint) ? null : bindConstraint(params.constraint)

        def results = multiDimService.listDimensionElements(dimension, user, constraint)
        render results as JSON
    }

    private void verifyDimension(Collection<Dimension> dimensions, String dimensionName, user) {
        if (dimensions.empty) {
            throw new InvalidArgumentsException("Dimension with a name $dimensionName is invalid.")
        } else if (dimensions.size() > 1) {
            throw new InvalidArgumentsException("More than one dimension found with name $dimensionName.")
        }
        accessControlChecks.verifyDimensionsAccessible(dimensions, user)
    }

}
