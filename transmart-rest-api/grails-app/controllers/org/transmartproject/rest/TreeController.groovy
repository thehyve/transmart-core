/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.rest.Link
import grails.rest.render.util.AbstractLinkingRenderer
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.tree.TreeNode
import org.transmartproject.db.tree.TreeService
import org.transmartproject.db.user.User
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.CurrentUser

@Slf4j
class TreeController {

    static responseFormats = ['json', 'hal']

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

    TreeService treeService

    /**
     * Tree nodes endpoint:
     * <code>/${apiVersion}/tree_nodes</code>
     *
     * Optional parameters:
     * - root
     * - depth
     * - counts
     * - tags
     *
     * @return
     */
    def index(@RequestParam('api_version') String apiVersion,
              @RequestParam('root') String root,
              @RequestParam('depth') Integer depth,
              @RequestParam('counts') Boolean counts,
              @RequestParam('tags') Boolean tags) {
        def acceptedParams = ['action', 'controller', 'apiVersion', 'root', 'depth', 'counts', 'tags']
        params.keySet().each { param ->
            if (!acceptedParams.contains(param)) {
                throw new InvalidArgumentsException("Parameter not supported: $param.")
            }
        }
        if (root) {
            root = URLDecoder.decode(root, 'UTF-8')
        }
        log.info "Tree. apiVersion = ${apiVersion}, root = ${root}, depth = ${depth}, counts = ${counts}, tags = ${tags}"
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        List<TreeNode> nodes = treeService.findNodesForUser(
                root,
                depth,
                counts,
                tags,
                user)
        log.info "${nodes.size()} results."
        respond wrapNodes(apiVersion, root, depth, nodes)
    }

    private setVersion(String apiVersion, List<TreeNode> nodes) {
        nodes.each {
            it.apiVersion = apiVersion
            setVersion(apiVersion, it.children)
        }
    }

    /**
     * @param source
     * @return CollectionResponseWrapper so we can provide a proper HAL response
     */
    private ContainerResponseWrapper wrapNodes(String apiVersion,
                                               String root,
                                               Integer depth,
                                               List<TreeNode> source) {
        setVersion(apiVersion, source)
        def params = [
                root: root,
                depth: depth
        ].findAll { k, v -> v }.collect { k, v ->
            "${k}=${URLEncoder.encode(v.toString(), 'UTF-8')}"
        }.join('&')
        if (params) {
            params = "?$params"
        }
        new ContainerResponseWrapper(
                key: 'tree_nodes',
                container: source,
                componentType: TreeNode,
                links: [
                        new Link(AbstractLinkingRenderer.RELATIONSHIP_SELF,
                                "/${apiVersion}/tree_nodes${params}"
                        )
                ]
        )
    }

}
