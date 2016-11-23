package org.transmartproject.db.metadata

import com.google.common.collect.ImmutableMap
import groovy.transform.InheritConstructors
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.db.dataquery2.ConceptDimension
import org.transmartproject.db.dataquery2.DimensionImpl
import org.transmartproject.db.dataquery2.ModifierDimension
import org.transmartproject.db.dataquery2.PatientDimension
import org.transmartproject.db.dataquery2.StartTimeDimension
import org.transmartproject.db.dataquery2.EndTimeDimension
import org.transmartproject.db.dataquery2.LocationDimension
import org.transmartproject.db.dataquery2.StudyDimension
import org.transmartproject.db.dataquery2.TrialVisitDimension
import org.transmartproject.db.dataquery2.VisitDimension
import org.transmartproject.db.dataquery2.ProviderDimension
import org.transmartproject.db.dataquery2.AssayDimension
import org.transmartproject.db.dataquery2.BioMarkerDimension
import org.transmartproject.db.dataquery2.ProjectionDimension

import org.transmartproject.db.i2b2data.Study

import static org.transmartproject.db.dataquery2.DimensionImpl.Density.SPARSE
import static org.transmartproject.db.dataquery2.DimensionImpl.Density.DENSE
import static org.transmartproject.db.dataquery2.DimensionImpl.Packable.PACKABLE
import static org.transmartproject.db.dataquery2.DimensionImpl.Packable.NOT_PACKABLE
import static org.transmartproject.db.dataquery2.DimensionImpl.Size.LARGE
import static org.transmartproject.db.dataquery2.DimensionImpl.Size.MEDIUM
import static org.transmartproject.db.dataquery2.DimensionImpl.Size.SMALL

class DimensionDescription {
    static final String LEGACY_MARKER = "legacy tabular study marker"

    String name
    String modifierCode
    DimensionImpl.Size size
    DimensionImpl.Density density
    DimensionImpl.Packable packable

    static belongsTo = Study
    static hasMany = [
            studies: Study
    ]

    static constraints = {
        modifierCode    nullable: true
        size            nullable: true
        density         nullable: true
        packable        nullable: true
    }

    static mapping = {
        table schema: 'i2b2metadata'
        version       false

        size    column: 'size_cd'
    }


    static ImmutableMap<String,DimensionImpl> dimensionsMap = ImmutableMap.copyOf([
            "study"      : new StudyDimension(MEDIUM, SPARSE, PACKABLE),
            "concept"    : new ConceptDimension(MEDIUM, DENSE, PACKABLE),
            "patient"    : new PatientDimension(LARGE, DENSE, PACKABLE),
            "visit"      : new VisitDimension(SMALL, DENSE, PACKABLE),
            "start time" : new StartTimeDimension(LARGE, SPARSE, NOT_PACKABLE),
            "end time"   : new EndTimeDimension(LARGE, SPARSE, NOT_PACKABLE),
            "location"   : new LocationDimension(MEDIUM, SPARSE, NOT_PACKABLE),
            "trial visit": new TrialVisitDimension(MEDIUM, DENSE, PACKABLE),
            "provider"   : new ProviderDimension(SMALL, DENSE, PACKABLE),
//            "sample": new SampleDimension(SMALL, DENSE, NOT_PACKABLE),

            "biomarker" : new BioMarkerDimension(LARGE, DENSE, PACKABLE),
            "assay"     : new AssayDimension(LARGE, DENSE, PACKABLE),
            "projection": new ProjectionDimension(SMALL, DENSE, PACKABLE),
    ])

    boolean isLegacyTabular() {
        return name == LEGACY_MARKER
    }

    def afterLoad() {
        check()
    }

    void check() {
        if(name == LEGACY_MARKER) return
        if(dimensionsMap.keySet().contains(name) && [modifierCode, size, density, packable].any { it != null }) {
            throw new DataInconsistencyException("Inconsistent metadata in DimensionDescription: For builtin " +
                    "'$name' dimension all other fields must be set to NULL")
        } else if(!dimensionsMap.keySet().contains(name) && [modifierCode, size, density, packable].any { it == null }) {
            throw new DataInconsistencyException("Inconsistent metadata in DimensionDescription: '$name' dimension" +
                    " is not builtin and some modifier dimension fields are NULL")
        }
    }

    DimensionImpl getDimension() {
        if(name == LEGACY_MARKER) {
            throw new LegacyStudyException("This study is loaded according to the pre-TranSMART 17.1 rules. " +
                    "Retrieving 17.1 dimensions is not possible.")
        }
        if(modifierCode == null) {
            return dimensionsMap[name]
        } else {
            return new ModifierDimension(name, modifierCode, size, density, packable)
        }
    }

}


@InheritConstructors
class LegacyStudyException extends UnsupportedOperationException {}
