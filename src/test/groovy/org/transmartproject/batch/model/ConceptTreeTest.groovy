package org.transmartproject.batch.model

import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 *
 */
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
        Assert.assertNotNull(root)
        Assert.assertNull(root.parent)
        Assert.assertEquals(1, root.children.size())

        ConceptNode study = tree.study
        Assert.assertEquals('GSE8581', study.name)
        Assert.assertEquals(ROOT_PATH, study.path)
        Assert.assertEquals(root, study.parent)
        Assert.assertEquals(0, study.children.size())
    }

    @Test
    void testFindNode() {
        String leaf = 'Leaf'
        String path = "$ROOT_PATH$leaf$ConceptNode.SEP"

        ConceptNode node = tree.find(path)
        Assert.assertEquals(leaf, node.name)
        Assert.assertEquals(path, node.path)
        Assert.assertEquals(tree.study, node.parent)
        Assert.assertEquals(0, node.children.size())
    }


}
