/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.rest.Link
import grails.rest.render.util.AbstractLinkingRenderer
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.core.tree.TreeResource
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.CurrentUser
import org.transmartproject.rest.serialization.TreeJsonSerializer

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

@Slf4j
class TreeController {

    static responseFormats = ['json', 'hal']

    @Autowired
    CurrentUser currentUser

    @Autowired
    TreeResource treeResource

    /**
     * Tree nodes endpoint:
     * <code>/${apiVersion}/tree_nodes</code>
     *
     * Fetches all ontology nodes the satisfy the specified criteria (all nodes by default)
     * as a forest.
     *
     * @param root (Optional) the root element from which to fetch.
     * @param depth (Optional) the maximum number of levels to fetch.
     * @param constraints flag if the constraints should be included in the result
     *   (always true for hal, defaults to true for json)
     * @param counts flag if counts should be included in the result (default: false)
     * @param tags flag if tags should be included in the result (default: false)
     *
     * @return a forest of ontology nodes.
     */
    def index(@RequestParam('api_version') String apiVersion,
              @RequestParam('root') String root,
              @RequestParam('depth') Integer depth,
              @RequestParam('constraints') Boolean constraints,
              @RequestParam('counts') Boolean counts,
              @RequestParam('tags') Boolean tags) {
        checkForUnsupportedParams(params, ['root', 'depth', 'constraints', 'counts', 'tags'])
        if (root) {
            root = URLDecoder.decode(root, 'UTF-8')
        }
        log.info "Tree. apiVersion = ${apiVersion}, root = ${root}, depth = ${depth}, constraints = ${constraints}, counts = ${counts}, tags = ${tags}"
        List<TreeNode> nodes = treeResource.findNodesForUser(
                root,
                depth,
                counts,
                tags,
                currentUser)
        log.info "${nodes.size()} results."
        if (request.format == 'hal') {
            respond wrapNodes(apiVersion, root, depth, nodes)
        } else {
            response.status = 200
            response.contentType = 'application/json'
            response.characterEncoding = 'utf-8'
            new TreeJsonSerializer().write([writeConstraints: constraints], nodes, response.outputStream)
        }
    }

    /**
     * Clears tree node and counts caches:
     * <code>/${apiVersion}/tree_nodes/clear_cache</code>
     *
     * This endpoint should be called after loading, deleting or updating
     * tree nodes in the database.
     * Only available for administrators.
     */
    def clearCache() {
        checkForUnsupportedParams(params, [])
        treeResource.clearCache(currentUser)
        response.status = 200
    }

    /**
     * Clears tree node and counts caches and rebuilds the tree node cache:
     * <code>/${apiVersion}/tree_nodes/rebuild_cache</code>
     *
     * This endpoint should be called after loading, deleting or updating
     * tree nodes in the database.
     * Only available for administrators.
     *
     * Asynchronous call. The call returns when rebuilding has started.
     * Code 503 is returned iff a rebuild operation is already in progress.
     */
    def rebuildCache() {
        checkForUnsupportedParams(params, [])
        treeResource.rebuildCache(currentUser)
        response.status = 200
    }

    /**
     * Checks if a cache rebuild task is running:
     * <code>/${apiVersion}/tree_nodes/rebuild_status</code>
     * Returns an object with a field <code>status</code> with value 'running'
     * or 'stopped'.
     * Example: <code>{"status": "running"}</code>.
     *
     * Only available for administrators.
     *
     * @return an object with a field <code>status</code> with value <code>running</code>
     * or <code>stopped</code>.
     */
    def rebuildStatus() {
        checkForUnsupportedParams(params, [])
        respond status: treeResource.isRebuildActive(currentUser) ? 'running' : 'stopped'
    }

    private setVersion(String apiVersion, List<TreeNode> nodes) {
        nodes.each {
            it.apiVersion = apiVersion
            setVersion(apiVersion, it.children)
        }
    }

    /**
     * Wrapper so we can provide a proper HAL response.
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
