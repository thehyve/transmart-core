package org.transmartproject.rest

import com.google.common.base.Strings
import grails.converters.JSON
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.DimensionElementSerializer

import java.util.stream.Collectors

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

@Slf4j
class DimensionController extends AbstractQueryController {

    @Autowired
    AuthorisationChecks authorisationChecks

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    @Autowired
    MDStudiesResource studiesResource

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

    @CompileStatic
    private Dimension getDimension(String dimensionName, User user) {
        Set<String> dimensionNames = studiesResource.getStudies(user, PatientDataAccessLevel.MEASUREMENTS).stream()
                .flatMap({ MDStudy study ->
                    study.dimensions.stream().map({ Dimension dimension -> dimension.name }) })
                .collect(Collectors.toSet())
        if (!dimensionNames.contains(dimensionName)) {
            // We need to return the same response for nonexisting dimensions and for inaccessible dimensions to prevent
            // an information leak. Users should not be able to find out if a certain (modifier-)dimension exists in a
            // study they don't have access to.
            throw new NoSuchResourceException("Dimension '$dimensionName' is not valid or you don't have access")
        }
        return multiDimensionalDataResource.getDimension(dimensionName)
    }

    private ContainerResponseWrapper wrapElements(Dimension dim, Iterable elements) {
        def des = new DimensionElementSerializer(dim, elements.iterator())
        new ContainerResponseWrapper(
                container: des,
                key: 'elements',
                componentType: Object)
    }

}
