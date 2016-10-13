package org.transmartproject.db.metadata

import com.google.common.collect.ImmutableMap
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataRetrievalFailureException
import org.transmartproject.db.dataquery2.ConceptDimension
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.ModifierDimension
import org.transmartproject.db.dataquery2.PatientDimension
import org.transmartproject.db.dataquery2.StartTimeDimension
import org.transmartproject.db.dataquery2.StudyDimension
import org.transmartproject.db.dataquery2.TrialVisitDimension
import org.transmartproject.db.i2b2data.Study

import static org.transmartproject.db.dataquery2.Dimension.Density.SPARSE
import static org.transmartproject.db.dataquery2.Dimension.Density.DENSE
import static org.transmartproject.db.dataquery2.Dimension.Packable.PACKABLE
import static org.transmartproject.db.dataquery2.Dimension.Packable.NOT_PACKABLE
import static org.transmartproject.db.dataquery2.Dimension.Size.LARGE
import static org.transmartproject.db.dataquery2.Dimension.Size.MEDIUM

class DimensionDescription {

    String name
    String modifierCode
    Dimension.Size size
    Dimension.Density density
    Dimension.Packable packable

    static belongsTo = Study
    static hasMany = [
            studies: Study
    ]

    static constraints = {
        //name            inList: dimensionsMap.keySet() as List        // not for modifier dimensions
        modifierCode    nullable: true
        size            nullable: true
        density         nullable: true
        packable        nullable: true
    }


    static dimensionsMap = ImmutableMap.copyOf([
            "study": new StudyDimension(MEDIUM, SPARSE, PACKABLE),
            "concept": new ConceptDimension(MEDIUM, DENSE, PACKABLE),
            "patient": new PatientDimension(LARGE, DENSE, PACKABLE),
//            "visit": new VisitDimension(SMALL, DENSE, PACKABLE),
            "start time": new StartTimeDimension(LARGE, SPARSE, NOT_PACKABLE),
//            "end time": new EndTimeDimension(LARGE, SPARSE, NOT_PACKABLE),
//            "location": new LocationDimension(MEDIUM, SPARSE, NOT_PACKABLE),
            "trial visit": new TrialVisitDimension(MEDIUM, DENSE, PACKABLE),
//            "sample": new SampleDimension(SMALL, DENSE, NOT_PACKABLE),
//            "biomarker": new BioMarkerDimension(LARGE, DENSE, PACKABLE),
//            "projection": new ProjectionDimension(SMALL, DENSE, PACKABLE),
//            "assay": new AssayDimension(LARGE, DENSE, PACKABLE),
    ])

    def afterLoad() {
        check()
    }

    void check() {
        if((dimensionsMap.keySet().contains(name) && [modifierCode, size, density, packable].any { it != null }) ||
            (!dimensionsMap.keySet().contains(name) && [modifierCode, size, density, packable].any { it == null })
        ) {
            throw new DataRetrievalFailureException("DimensionDescription columns modifierCode, size and density " +
                    "must all be NULL or all be not null, " + this)
        }
    }

    Dimension getDimension() {
        if(modifierCode == null) {
            return dimensionsMap[name]
        } else {
            return new ModifierDimension(name, modifierCode, size, density, packable)
        }
    }

}
