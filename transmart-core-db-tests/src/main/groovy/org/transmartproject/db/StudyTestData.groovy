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
        study.addToDimensions(legacyDimension)
        study
    }

    /**
     * dimensions can contain Strings or Dimensions
     * @param name
     * @param dimensions
     * @return
     */
    static Study createStudy(String name, Iterable dimensions) {
        def study = new Study(studyId: name, secureObjectToken: "EXP:${name}")
        dimensions.each {
            if(it instanceof DimensionDescription) {
                study.addToDimensions(it)
            } else if(it instanceof String) {
                def candidates = DimensionDescription.findAllByName(it)
                if(!candidates) {
                    assert DimensionDescription.dimensionsMap.containsKey(it), "Unknown dimension name '$it', " +
                            "modifier dimensions as string are not supported in createStudy()"
                    study.addToDimensions(new DimensionDescription(name: it))
                } else if(candidates.size() == 1) {
                    study.addToDimensions(candidates[0])
                } else {
                    assert false, "Multiple DimensionDescriptions with the same name found: '$it'"
                }
            } else {
                assert false, "Dimensions iterable passed to createStudy contains object that is not a String or a " +
                        "DimensionDescription: $it"
            }
        }
        study
    }

}
