package org.transmartproject.db

import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription

class StudyTestData {

    static Study createDefaultTabularStudy() {
        def legacyDimension = DimensionDescription.createCriteria().get {
            eq('name', DimensionDescription.LEGACY_MARKER)
        } ?: new DimensionDescription(
                name: DimensionDescription.LEGACY_MARKER
        )

        def study = new Study(
                studyId: "Default tabular study",
                secureObjectToken: 'EXP:Default tabular study'
        )
        study.addToDimensionDescriptions(legacyDimension)
        study
    }

    /**
     * Creates a public or private study with specified study id and dimensions.
     * @param name
     * @param dimensionNames
     * @return
     */
    static Study createStudy(String name, List<String> dimensionNames = [], boolean isPublic = false) {
        def dimensionDescriptions = DimensionDescription.createCriteria()
            .list {
                'in'('name', dimensionNames)
            } as List<DimensionDescription>
        assert (dimensionNames - dimensionDescriptions*.name).empty : 'Not all dimensions were found'
        def study = new Study(
                studyId: name,
                secureObjectToken: isPublic ? Study.PUBLIC : "EXP:${name}",
                dimensionDescriptions: dimensionDescriptions
        )
        study
    }

}
