package org.transmartproject.rest

import com.google.common.base.Strings
import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.db.user.User
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.DimensionElementSerializer
import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

@Slf4j
class DimensionController extends AbstractQueryController {

    @Autowired
    AccessControlChecks accessControlChecks

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource
    
    static responseFormats = ['json', 'hal']
    
    /**
     * Dimension endpoint:
     * <code>GET /v2/dimensions/${dimensionName}/elements</code>
     *
     * @return a list of all dimension elements that user has access to.
     */
    def list(@PathVariable('dimensionName') String dimensionName) {

        checkForUnsupportedParams(params, ['dimensionName', 'constraint'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def dimension = getDimension(dimensionName, user)

        Constraint constraint = Strings.isNullOrEmpty(params.constraint) ? new TrueConstraint() : bindConstraint(params.constraint)

        def results = multiDimService.getDimensionElements(dimension, constraint, user)
        render wrapElements(dimension, results) as JSON
    }

    private Dimension getDimension(String dimensionName, User user) {
        def dimension = multiDimensionalDataResource.getDimension(dimensionName)
        // We need to return the same response for nonexisting dimensions and for inaccessible dimensions to prevent
        // an information leak. Users should not be able to find out if a certain (modifier-)dimension exists in a
        // study they don't have access to.
        if(dimension != null &&
                accessControlChecks.getInaccessibleDimensions([dimension], user).empty) {
            return dimension
        }

        throw new NoSuchResourceException("Dimension '$dimensionName' is not valid or you don't have access")
    }

    private ContainerResponseWrapper wrapElements(Dimension dim, Iterable elements) {
        def des = new DimensionElementSerializer(dim, elements.iterator())
        new ContainerResponseWrapper(
                container: des,
                key: 'elements',
                componentType: Object)
    }

}
