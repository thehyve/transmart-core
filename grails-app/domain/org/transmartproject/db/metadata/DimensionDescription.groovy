package org.transmartproject.db.metadata

import com.google.common.collect.ImmutableMap
import groovy.transform.InheritConstructors
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.db.multidimquery.ConceptDimension
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.ModifierDimension
import org.transmartproject.db.multidimquery.PatientDimension
import org.transmartproject.db.multidimquery.StartTimeDimension
import org.transmartproject.db.multidimquery.EndTimeDimension
import org.transmartproject.db.multidimquery.LocationDimension
import org.transmartproject.db.multidimquery.StudyDimension
import org.transmartproject.db.multidimquery.TrialVisitDimension
import org.transmartproject.db.multidimquery.VisitDimension
import org.transmartproject.db.multidimquery.ProviderDimension
import org.transmartproject.db.multidimquery.AssayDimension
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.db.multidimquery.ProjectionDimension

import org.transmartproject.db.i2b2data.Study

import static org.transmartproject.core.multidimquery.Dimension.Size.*
import static org.transmartproject.core.multidimquery.Dimension.Density.*
import static org.transmartproject.core.multidimquery.Dimension.Packable.*

class DimensionDescription {
    static final String LEGACY_MARKER = "legacy tabular study marker"

    String name
    String modifierCode
    Dimension.Size size
    Dimension.Density density
    Dimension.Packable packable

    transient ModifierDimension modifierDimension = null

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
        }
        if(modifierDimension != null) {
            return modifierDimension
        }
        return modifierDimension = ModifierDimension.get(name, modifierCode, size, density, packable)
    }

}


@InheritConstructors
class LegacyStudyException extends UnsupportedOperationException {}
