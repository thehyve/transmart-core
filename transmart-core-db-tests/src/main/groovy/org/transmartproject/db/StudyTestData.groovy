package org.transmartproject.db

import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription

class StudyTestData {

    static Study createDefaultTabularStudy() {
        def legacyDimension = new DimensionDescription(
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
     * @param dimensions
     * @return
     */
    static Study createStudy(String name, List<String> dimensions = [], boolean isPublic = false) {
        new Study(
            studyId: name,
            secureObjectToken: isPublic ? Study.PUBLIC : "EXP:${name}",
            dimensionDescriptions: DimensionDescription.findAllByNameInList(dimensions)
        )
    }

}
