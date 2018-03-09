package org.transmartproject.rest.marshallers

import grails.converters.JSON
import grails.rest.Link
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.grails.web.util.WebUtils
import org.springframework.stereotype.Component
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.core.tree.TreeNode

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.*

@Slf4j
@CompileStatic
@Component
class TreeNodeMarshaller implements ObjectMarshaller<JSON> {

    @Resource
    OntologyTermTagsResource tagsResource

    public static final String LINKS_ATTRIBUTE = '_links'
    public static final String RELATIONSHIP_CHILDREN = 'children'

    @Override
    boolean supports(Object object) {
        object != null && TreeNode.isAssignableFrom(object.getClass())
    }

    Map<String, Object> convertToMap(TreeNode obj, boolean hal) {
        def result = [
                name:     obj.name,
                fullName: obj.fullName,
                type:     obj.ontologyTermType.name(),
                visualAttributes: obj.visualAttributes
        ] as Map<String, Object>
        if (obj.dimension) {
            result.dimension = obj.dimension
        }
        if (obj.study?.studyId) {
            result.studyId = obj.study?.studyId
        }
        if (obj.conceptPath != null) {
            result.conceptPath = obj.conceptPath
        }
        if (obj.conceptCode != null) {
            result.conceptCode = obj.conceptCode
        }
        if (obj.tags && obj.tags.size() > 0) {
            result.metadata = obj.tags.collectEntries { [(it.name): it.description ] }
        }
        if (obj.observationCount != null) {
            result.observationCount = obj.observationCount
        }
        if (obj.patientCount != null) {
            result.patientCount = obj.patientCount
        }

        if (!hal && obj.children) {
            result['children'] = obj.children.collect { convertToMap(it, hal) }
        }

        result
    }

    @Override
    void marshalObject(Object object, JSON json) {
        TreeNode node = (TreeNode)object
        def request = WebUtils.retrieveGrailsWebRequest().getCurrentRequest()
        boolean hal = (request.format == 'hal')
        log.debug "Converting ${node.name}..."
        Date t1 = new Date()
        Map<String, Object> mapRepresentation =
                convertToMap(node, hal)
        Date t2 = new Date()
        log.debug "Convert to map took ${t2.time - t1.time} ms."
        if (hal) {
            mapRepresentation[LINKS_ATTRIBUTE] = getLinks(node)
        }

        json.value mapRepresentation
    }

    /**
     * @param term ontology term
     * @return url for given ontology term and request study or concept study
     */
    static String getTreeNodeUrl(TreeNode node) {
        "/${node.apiVersion}/tree_nodes?root=${URLEncoder.encode(node.fullName, 'UTF-8')}"
    }

    Collection<Link> createLinks(TreeNode obj) {
        String url = getTreeNodeUrl(obj)

        List<Link> result = []
        result.add(new Link(RELATIONSHIP_SELF, url))

        if (OntologyTerm.VisualAttributes.LEAF in obj.visualAttributes) {
            Link datalink = new Link('observations',
                    "/${obj.apiVersion}/observations?constraint={type: 'conceptconstraint', path: '${URLEncoder.encode(obj.fullName, 'UTF-8')}'}"
            )
            result.add(datalink)
        }

        for (TreeNode child: obj.children) {
            Link link = new Link(RELATIONSHIP_CHILDREN, getTreeNodeUrl(child))
            link.setTitle(child.name)
            result.add(link)
        }

        if (obj.parent) {
            result.add(new Link("parent", getTreeNodeUrl(obj.parent)))
        }
        result
    }

    static Set<String> getAggregatedLinkRelations() {
        [RELATIONSHIP_CHILDREN] as Set
    }

    /**
     * @param object
     * @return map of relationship to link value. Value is either a Link (simple) or a List<Link> (aggregated)
     */
    private Map<String, Object> getLinks(TreeNode object) {

        Map<String, Object> result = [:]
        Map<String, List<Link>> grouped = createLinks(object).groupBy { it.rel }

        grouped.each {
            key, list ->
                if (aggregatedLinkRelations.contains(key)) {
                    result.put(key, list.collect { convertLink(it) })
                } else {
                    //only the first element will be picked. Its not supposed to have more than one anyway
                    result.put(key, convertLink(list.get(0)))
                }
        }

        result
    }

    private static Map<String, Object> convertLink(Link link) {
        def res = [(HREF_ATTRIBUTE): link.href] as Map<String, Object>
        if (link.hreflang) {
            res[HREFLANG_ATTRIBUTE] = link.hreflang
        }
        if (link.title) {
            res[TITLE_ATTRIBUTE] = link.title
        }
        if (link.contentType) {
            res[TYPE_ATTRIBUTE] = link.contentType
        }
        if (link.templated) {
            res[TEMPLATED_ATTRIBUTE] = Boolean.TRUE
        }
        if (link.deprecated) {
            res[DEPRECATED_ATTRIBUTE] = Boolean.TRUE
        }
        res
    }

}
