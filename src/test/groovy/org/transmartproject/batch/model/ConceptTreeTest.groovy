package org.transmartproject.batch.model

import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptTree

/**
 *
 */
@Ignore // class rewritten
class ConceptTreeTest {

    static final String ROOT_PATH = '\\Public Studies\\GSE8581\\'

    private ConceptTree tree

    @Before
    void setUp() {
        tree = new ConceptTree(ROOT_PATH)
    }

    @Test
    void testCreateTree() {
        ConceptNode root = tree.root
        assert root != null
        assert root.parent == null
        assert root.children.size() == 1

        ConceptNode study = tree.study
        assert 'GSE8581' == study.name
        assert ROOT_PATH == study.path
        assert root == study.parent
        assert study.children.size() == 0
    }

    @Test
    void testFindNode() {
        String leaf = 'Leaf'
        String path = "$ROOT_PATH$leaf$ConceptNode.SEP"

        ConceptNode node = tree.find(path)
        assert leaf == node.name
        assert path == node.path
        assert tree.study == node.parent
        assert node.children.size() == 0
    }


}
