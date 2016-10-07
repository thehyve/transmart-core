package org.transmartproject.rest

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.dataquery2.QueryService
import org.transmartproject.db.dataquery2.query.DimensionMetadata
import org.transmartproject.db.dataquery2.query.Field
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.dataquery2.query.Query
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser

@Slf4j
class QueryController {

    QueryService queryService

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    private boolean bindQuery(Query query) {
        if (!params.query) {
            throw new InvalidArgumentsException("Query parameter is missing.")
        }
        String queryParam = URLDecoder.decode(params.query, 'UTF-8')
        try {
            Map queryData = JSON.parse(queryParam) as Map
            bindData(query, queryData)
            query.validate()
            if (query.hasErrors()) {
                response.status = 403
                render query.errors as JSON
                return false
            }
        } catch (ConverterException e) {
            throw new InvalidArgumentsException("Cannot parse query parameter", e)
        }
        return true
    }

    def observations() {
        ObservationQuery query = new ObservationQuery()
        if (bindQuery(query)) {
            User user = (User)usersResource.getUserFromUsername(currentUser.username)
            def observations = queryService.list(query, user)
            render observations as JSON
        }
    }

    def supportedFields() {
        List<Field> fields = DimensionMetadata.supportedFields
        render fields as JSON
    }
}
