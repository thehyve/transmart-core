package org.transmartproject.rest.serialization

import com.google.gson.stream.JsonWriter
import grails.converters.JSON
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.tree.TreeNode

@CompileStatic
class TreeJsonSerializer {

    protected JsonWriter writer
    protected boolean writeConstaints

    protected void writeConstraint(final MultiDimConstraint constraint) {
        writer.jsonValue((constraint as JSON).toString(false))
    }

    protected void writeNode(final TreeNode node) {
        writer.beginObject()
        writer.name('name').value(node.name)
        writer.name('fullName').value(node.fullName)
        if (node.study?.studyId) {
            writer.name('studyId').value(node.study.studyId)
        }
        if (node.conceptCode) {
            writer.name('conceptCode').value(node.conceptCode)
        }
        if (node.conceptPath) {
            writer.name('conceptPath').value(node.conceptPath)
        }
        writer.name('name').value(node.name)
        writer.name('type').value(node.ontologyTermType.name())
        writer.name('visualAttributes')
        writer.beginArray()
        node.visualAttributes.each {
            writer.value(it.name())
        }
        writer.endArray()
        if (node.observationCount) {
            writer.name('observationCount').value(node.observationCount)
        }
        if (node.patientCount) {
            writer.name('patientCount').value(node.patientCount)
        }
        if (this.writeConstaints && node.constraint) {
            writer.name('constraint')
            writeConstraint(node.constraint)
        }
        if (node.children) {
            writer.name('children')
            writeNodes(node.children)
        }
        writer.endObject()
    }

    protected void writeNodes(final List<TreeNode> nodes) {
        writer.beginArray()
        nodes.each {
            writeNode(it)
        }
        writer.endArray()
    }

    /**
     * Writes the tree node to JSON.
     *
     * @param out the stream to write to.
     */
    void write(Map args, final TreeNode node, OutputStream out) {
        this.writer = new JsonWriter(new PrintWriter(new BufferedOutputStream(out)))
        this.writeConstaints = args?.writeConstraints ?: false
        writeNode(node)
    }

    /**
     * Writes the tree nodes to JSON.
     *
     * @param out the stream to write to.
     */
    void write(Map args, final List<TreeNode> forest, OutputStream out) {
        this.writer = new JsonWriter(new PrintWriter(new BufferedOutputStream(out)))
        this.writer.indent = ''
        this.writeConstaints = args?.writeConstraints == null ? true : args?.writeConstraints
        writer.beginObject()
        writer.name('tree_nodes')
        writeNodes(forest)
        writer.endObject()
        writer.flush()
    }

}
