package org.transmartproject.rest

import com.google.common.base.Strings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.user.User
import org.transmartproject.rest.marshallers.DimensionWrapper

@Slf4j
class DimensionController extends AbstractQueryController {
    
    @Autowired
    VersionController versionController
    
    static responseFormats = ['json', 'hal']
    
    /**
     * Dimension endpoint:
     * <code>GET /v2/dimension/${dimensionName}</code>
     *
     * @return a list of all dimension elements that user has access to.
     */
    def list(
            @RequestParam('api_version') String apiVersion,
            @PathVariable('dimensionName') String dimensionName) {
        checkParams(params, ['dimensionName', 'constraint'])
        
        Constraint constraint = Strings.isNullOrEmpty(params.constraint) ? null : bindConstraint(params.constraint)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        
        def elements = multiDimService.listDimensionElements(dimensionName, user, constraint)
        respond wrapDimensionElements(elements, apiVersion)
    }

    private DimensionWrapper wrapDimensionElements(List<Object> elements, String apiVersion) {
        String currentVersion = versionController.currentVersion(apiVersion)
        new DimensionWrapper(
                apiVersion: currentVersion,
                elements: elements
        )
    }
}
