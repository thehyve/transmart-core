package org.transmartproject.rest

interface TestResource {

    void createTestData()

    void createTestStudy(String studyId, boolean isPublic, List<String> trialVisits)

}
