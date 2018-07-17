package org.transmartproject.rest

import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.multidimquery.TrialVisit
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.tree.TreeNode

interface TestResource {

    void clearTestData()

    void createTestData()

    List<TrialVisit> createTestStudy(String studyId, boolean isPublic, List<String> trialVisitLabels)

    Concept createTestConcept(String conceptCode)

    TreeNode createTestTreeNode(
            String parentPath, String name, String conceptPath, OntologyTerm.VisualAttributes conceptType, MDStudy study)

    Patient createTestPatient(String subjectId)

    void createTestCategoricalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String,String>> values, Date startDate)

    void createTestNumericalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String,BigDecimal>> values, Date startDate)

}
