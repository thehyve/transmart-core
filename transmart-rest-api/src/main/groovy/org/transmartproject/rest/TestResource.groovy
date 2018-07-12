package org.transmartproject.rest

import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.multidimquery.TrialVisit

interface TestResource {

    void clearTestData()

    void createTestData()

    List<TrialVisit> createTestStudy(String studyId, boolean isPublic, List<String> trialVisitLabels)

    Concept createTestConcept(String conceptCode)

    Patient createTestPatient(String subjectId)

    void createTestCategoricalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String,String>> values, Date startDate)

    void createTestNumericalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String,BigDecimal>> values, Date startDate)

}
