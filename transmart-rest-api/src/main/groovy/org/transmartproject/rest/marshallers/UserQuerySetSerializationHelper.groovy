package org.transmartproject.rest.marshallers

import grails.rest.Link

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class UserQuerySetSerializationHelper extends AbstractHalOrJsonSerializationHelper<UserQuerySetWrapper> {

    final Class targetType = UserQuerySetWrapper

    final String collectionName = 'querySets'

    @Override
    Collection<Link> getLinks(UserQuerySetWrapper querySet) {
        [new Link(RELATIONSHIP_SELF, "/v2/query_sets/${querySet.queryId}/diffs"),
         new Link(RELATIONSHIP_SELF, "/v2/query_sets/${querySet.queryId}")]

    }

    @Override
    Map<String, Object> convertToMap(UserQuerySetWrapper object) {
        Collection<UserQuerySetDiffWrapper> diffs = object.diffs
        Collection<UserQuerySetInstanceWrapper> instances = object.instances
        def result = [
                id: object.id,
                queryName: object.queryName,
                queryUsername: object.queryUsername,
                setType: object.setType,
                setSize: object.setSize,
                createDate: object.createDate
        ]
        if (diffs) {
            result.diffs = diffs
        }
        if (instances) {
            result.instances = instances
        }
        result
    }

}
