package org.transmartproject.rest

import com.google.common.base.Strings
import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.users.User
import org.transmartproject.db.accesscontrol.AccessControlChecks
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
     * <code>/v2/dimensions/${dimensionName}/elements</code>
     *
     * @return a list of all dimension elements that user has access to.
     */
    def list(@PathVariable('dimensionName') String dimensionName) {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['dimensionName', 'constraint'])

        def dimension = getDimension(dimensionName, authContext.user)

        Constraint constraint = Strings.isNullOrEmpty(args.constraint) ? new TrueConstraint() : bindConstraint(args.constraint)

        def results = multiDimService.getDimensionElements(dimension, constraint, authContext.user)
        render wrapElements(dimension, results) as JSON
    }

    private Dimension getDimension(String dimensionName, User user) {
        Dimension dimension
        try {
            dimension = multiDimensionalDataResource.getDimension(dimensionName)
            if (dimension != null) {
                accessControlChecks.checkDimensionsAccess([dimension], user)
            }
        } catch (NoSuchResourceException|AccessDeniedException e) {
            // We need to return the same response for nonexisting dimensions and for inaccessible dimensions to prevent
            // an information leak. Users should not be able to find out if a certain (modifier-)dimension exists in a
            // study they don't have access to.
            throw new NoSuchResourceException("Dimension '$dimensionName' is not valid or you don't have access")
        }
        dimension
    }

    private ContainerResponseWrapper wrapElements(Dimension dim, Iterable elements) {
        def des = new DimensionElementSerializer(dim, elements.iterator())
        new ContainerResponseWrapper(
                container: des,
                key: 'elements',
                componentType: Object)
    }

}
