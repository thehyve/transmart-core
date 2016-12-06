package org.transmartproject.db

import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl


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
     * dimensions can contain Strings or Dimensions
     * @param name
     * @param dimensions
     * @return
     */
    static Study createStudy(String name, Iterable dimensions, boolean isPublic = false) {
        def study = new Study(studyId: name, secureObjectToken: isPublic ? Study.PUBLIC : "EXP:${name}")
        dimensions.each {
            if(it instanceof DimensionDescription) {
                study.addToDimensionDescriptions(it)
            } else if(it instanceof String) {
                def candidates = DimensionDescription.findAllByName(it)
                if(!candidates) {
                    assert DimensionImpl.dimensionsMap.containsKey(it), "Unknown dimension name '$it', " +
                            "modifier dimensions as string are not supported in createStudy()"
                    study.addToDimensionDescriptions(new DimensionDescription(name: it))
                } else if(candidates.size() == 1) {
                    study.addToDimensionDescriptions(candidates[0])
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
