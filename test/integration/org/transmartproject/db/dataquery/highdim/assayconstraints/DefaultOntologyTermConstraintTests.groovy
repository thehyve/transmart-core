package org.transmartproject.db.dataquery.highdim.assayconstraints

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData
import org.transmartproject.db.ontology.ConceptsResourceService

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DefaultOntologyTermConstraintTests {

    ConceptsResourceService conceptsResourceService

    AssayTestData testData = new AssayTestData()

    @Before
    void setUp() {
        testData.saveAll()
    }

    @Test
    void basicTest() {
        AssayQuery assayQuery = new AssayQuery([
                new DefaultOntologyTermConstraint(
                        term: conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\bar')
                )
        ])

        List<AssayColumn> assays = assayQuery.retrieveAssays()

        /* We should have gotten the assays in the -200 range.
         * Those in the other ranges are assigned to another concept
         */
        assertThat assays, containsInAnyOrder(
                hasProperty('id', equalTo(-201L)),
                hasProperty('id', equalTo(-202L)),
                hasProperty('id', equalTo(-203L)),
        )
    }


}
