package org.transmartproject.batch.model

import groovy.transform.ToString

/**
 * Structure to hold all the concepts found in the database and data files related to a study
 */
class ConceptTree {

    final ConceptNode root
    final ConceptNode study

    ConceptTree(String studyPath) {
        String path = ConceptNode.getPath(studyPath)
        List<String> parts = ConceptNode.splitPaths(path)
        if (parts.size() != 2) {
            throw new IllegalArgumentException("Not a proper study concept path: $studyPath")
        }
        this.root = ConceptNode.createRoot(parts[0])
        parts.remove(0)
        this.study = root.find(parts)
    }


    ConceptNode find(String ... pathParts) {
        String path = ConceptNode.getJoinedPath(pathParts)
        if (!path) {
            throw new IllegalArgumentException('Path cannot be empty')
        }
        List<String> parts = ConceptNode.splitPaths(path)
        if (parts[0] != root.name) {
            throw new IllegalArgumentException("$path is out of tree: can only have one root")
        }
        parts.remove(0) //remove root

        if (parts.isEmpty()) {
            return root
        }

        if (parts[0] != study.name) {
            throw new IllegalArgumentException("$path is out of tree: can only have one study")
        }

        parts.remove(0) //remove study

        this.study.find(parts)
    }
}

/**
 * Represents a concept, with parent, children and other relevant fields
 */
@ToString(includes = ['code', 'path'])
class ConceptNode {

    static final char SEP = (char)'\\'

    ConceptNode parent
    String name
    String path
    Long code
    Long i2b2RecordId
    int level
    Variable variable

    boolean isNew = true //new by default, until explicitly set otherwise

    List<ConceptNode> children = []

    ConceptNode find(String ... pathParts) {
        String path = getJoinedPath(pathParts)
        if (!path) {
            throw new IllegalArgumentException('Path cannot be empty')
        }
        find(splitPaths(path))
    }

    protected ConceptNode find(List<String> pathList) {
        if (pathList.isEmpty()) {
            return this //lets not waste any more time
        }

        String first = pathList[0]
        ConceptNode child = children.find { it.name == first }
        if (!child) {
            //creating a new node
            child = new ConceptNode(parent: this, name: first, path: "${this.path}$first$SEP")
            child.level = level + 1
            children.add(child)
        }

        pathList.remove(0)

        if (pathList.isEmpty()) {
            return child //shortcut
        }
        return child.find(pathList)
    }

    protected Set<String> getDeepSubjectsSet() {
        Set<String> result = new HashSet<>(this.subjects)
        children.each {
            result.add(it.deepSubjectsSet)
        }
        result
    }

    List<ConceptNode> getAllChildren() {
        List<ConceptNode> result = new ArrayList<>(children)
        children.each {
            result.addAll(it.getAllChildren())
        }
        result
    }

    VariableType getType() {
        if (variable) {
            return variable.type
        } else if (parent) {
            return parent.type
        } else {
            return null
        }
    }

    static ConceptNode createRoot(String name) {
        ConceptNode result = new ConceptNode(name: name, path: "$SEP$name$SEP")
        result.level = 0
        result
    }

    static String getJoinedPath(String ... parts) {
        if (parts.length == 0) {
            throw new IllegalArgumentException('No paths defined')
        }
        List<String> list = parts.toList()
        StringBuilder sb = new StringBuilder(getPath(list[0]))

        for (int i=1; i<list.size(); i++) {
            if (sb.charAt(sb.length() - 1) != SEP) {
                sb.append(SEP)
            }
            sb.append(getPath(list[i]))
        }
        sb.toString()
    }

    static String getPath(String source) {
        source.replace((char)'+', SEP)
    }

    static List<String> splitPaths(String source) {
        //use a linked list, so its cheap to remove elements
        List<String> result = new LinkedList<>(source.split('\\\\').toList())
        if (!result.get(0)) {
            result.remove(0) //1st element is empty string
        }
        result

    }

}


