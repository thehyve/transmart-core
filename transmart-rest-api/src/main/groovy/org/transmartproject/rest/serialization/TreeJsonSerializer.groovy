package org.transmartproject.rest.serialization

import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.core.multidimquery.query.ConstraintSerialiser

@CompileStatic
class TreeJsonSerializer {

    protected JsonWriter writer
    protected boolean writeConstraints
    protected boolean writeTags

    protected void writeConstraint(final Constraint constraint) {
        new ConstraintSerialiser(writer).writeConstraint(constraint)
    }

    protected void writeMetadata(final List<OntologyTermTag> tags) {
        if (tags == null) {
            writer.nullValue()
        } else {
            writer.beginObject()
            for (OntologyTermTag tag: tags) {
                writer.name(tag.name).value(tag.description)
            }
            writer.endObject()
        }
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
        if (this.writeConstraints && node.constraint) {
            writer.name('constraint')
            writeConstraint(node.constraint)
        }
        if (this.writeTags && node.tags) {
            writer.name('metadata')
            writeMetadata(node.tags)
        }
        if (node.children) {
            writer.name('children')
            writeNodes(node.children)
        }
        writer.endObject()
    }

    protected void writeNodes(final List<TreeNode> nodes) {
        writer.beginArray()
        for(TreeNode node: nodes) {
            writeNode(node)
        }
        writer.endArray()
    }

    /**
     * Writes the tree node to JSON.
     *
     * @param args map with arguments to indicate if constraints and tags should be written:
     *  - writeConstraints: indicate if constraints should be written (default: true)
     *  - writeTags: indicate if tags should be written (default: false)
     * @param node the tree node to serialise.
     * @param out the stream to write to.
     */
    void write(Map args, final TreeNode node, OutputStream out) {
        this.writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(out)))
        this.writeConstraints = args?.writeConstraints == null ? true : args?.writeConstraints
        this.writeTags = args?.writeTags ?: false
        writeNode(node)
        writer.flush()
    }

    /**
     * Writes the tree nodes to JSON.
     *
     * @param args map with arguments to indicate if constraints and tags should be written:
     *  - writeConstraints: indicate if constraints should be written (default: true)
     *  - writeTags: indicate if tags should be written (default: false)
     * @param forest the tree nodes to serialise.
     * @param out the stream to write to.
     */
    void write(Map args, final List<TreeNode> forest, OutputStream out) {
        this.writer = new JsonWriter(new PrintWriter(new BufferedOutputStream(out)))
        this.writer.indent = ''
        this.writeConstraints = args?.writeConstraints == null ? true : args?.writeConstraints
        this.writeTags = args?.writeTags ?: false
        writer.beginObject()
        writer.name('tree_nodes')
        writeNodes(forest)
        writer.endObject()
        writer.flush()
    }

}
