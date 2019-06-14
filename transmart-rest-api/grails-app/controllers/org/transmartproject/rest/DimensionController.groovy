package org.transmartproject.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Strings
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.multidimquery.export.Format
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.hypercube.DimensionProperties
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.rest.misc.LazyOutputStreamDecorator
import org.transmartproject.rest.serialization.DimensionElementSerializer

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
     * Fetch dimension by name if it exists and the user has access.
     * @param dimensionName the dimension name
     * @param user the current user
     * @return the dimension object
     */
    @CompileStatic
    private Dimension getDimension(String dimensionName, User user) {
        Set<String> dimensionNames = studiesResource.getStudiesWithMinimalPatientDataAccessLevel(user, PatientDataAccessLevel.MEASUREMENTS).stream()
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

    @CompileStatic
    private List<DimensionProperties> getDimensions(User user) {
        return studiesResource.getStudiesWithMinimalPatientDataAccessLevel(user, PatientDataAccessLevel.MEASUREMENTS).stream()
                .flatMap({ MDStudy study ->
                    study.dimensions.stream()
                        .map({ Dimension dimension -> DimensionProperties.forDimension(dimension) }) })
                .distinct()
                .collect(Collectors.toList())
    }

    /**
     * Dimensions endpoint:
     * <code>/v2/dimensions</code>
     *
     * @return a list of all dimensions that the user has access to.
     */
    def list() {
        def dimensions = getDimensions(authContext.user)

        response.status = HttpStatus.OK.value()
        response.contentType = 'application/json'
        response.characterEncoding = 'utf-8'
        new ObjectMapper().writeValue(response.outputStream, [dimensions: dimensions])
    }

    /**
     * Dimension endpoint:
     * <code>/v2/dimensions/${dimensionName}</code>
     *
     * @return the dimension properties if the user has access to it.
     */
    def show(@PathVariable('dimensionName') String dimensionName) {
        def dimension = DimensionProperties.forDimension(getDimension(dimensionName, authContext.user))

        response.status = HttpStatus.OK.value()
        response.contentType = 'application/json'
        response.characterEncoding = 'utf-8'
        new ObjectMapper().writeValue(response.outputStream, dimension)
    }

    private getLazyOutputStream(Format format) {
        new LazyOutputStreamDecorator(
                outputStreamProducer: { ->
                    response.contentType = format.toString()
                    response.outputStream
                })
    }

    /**
     * Retrieve dimension elements for a query.
     * <code>POST /v2/dimensions/${dimensionName}/elements</code>
     *
     * Reads a constraint from the body and returns the dimension elements for the dimension
     * specified in the path for the observations matching the constraint that the
     * user has access to.
     *
     * @return the list of dimension elements.
     */
    def listElements(@PathVariable('dimensionName') String dimensionName) {
        def args = getGetOrPostParams()
        checkForUnsupportedParams(args, ['dimensionName', 'constraint'])

        def dimension = getDimension(dimensionName, authContext.user)

        Constraint constraint = Strings.isNullOrEmpty((String)args.constraint) ? new TrueConstraint() : bindConstraint((String)args.constraint)
        if (!constraint) {
            // deserialisation of the constraint failed
            return
        }

        Iterable elements = multiDimService.getDimensionElements(dimension.name, constraint, authContext.user)

        OutputStream out = getLazyOutputStream(Format.JSON)

        new DimensionElementSerializer(dimension, elements, out).write()
    }

}
