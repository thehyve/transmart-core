/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.metadata

import groovy.transform.InheritConstructors
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.ModifierDimension

import org.transmartproject.db.i2b2data.Study

import static org.transmartproject.db.i2b2data.ObservationFact.TYPE_NUMBER
import static org.transmartproject.db.i2b2data.ObservationFact.TYPE_TEXT

class DimensionDescription {
    static final String LEGACY_MARKER = "legacy tabular study marker"

    String name
    String modifierCode
    String valueType
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
        valueType       nullable: true
        size            nullable: true
        density         nullable: true
        packable        nullable: true
    }

    static mapping = {
        table schema: 'i2b2metadata'
        version       false

        size    column: 'size_cd'
    }


    boolean isLegacyTabular() {
        return name == LEGACY_MARKER
    }

    def afterLoad() {
        check()
    }

    void check() {
        if(name == LEGACY_MARKER) return
        if(DimensionImpl.isBuiltinDimension(name) && [modifierCode, valueType, size, density, packable].any { it != null }) {
            throw new DataInconsistencyException("Inconsistent metadata in DimensionDescription: For builtin " +
                    "'$name' dimension all other fields must be set to NULL")
        } else if(!DimensionImpl.isBuiltinDimension(name)) {
            if([modifierCode, valueType, size, density, packable].any { it == null }) {
                throw new DataInconsistencyException("Inconsistent metadata in DimensionDescription: '$name' dimension" +
                        " is not builtin and some modifier dimension fields are NULL")
            } else if(!(valueType in [TYPE_NUMBER, TYPE_TEXT])) {
                throw new DataInconsistencyException("Inconsistent metadata in DimensionDescription: '$name' " +
                        "dimension contains an unrecognized valueType '$valueType', expected " +
                        "'$TYPE_TEXT' or '$TYPE_NUMBER'")
            }
        }
    }

    DimensionImpl getDimension() {
        if(name == LEGACY_MARKER) {
            throw new LegacyStudyException("This study is loaded according to the pre-TranSMART 17.1 rules. " +
                    "Retrieving 17.1 dimensions is not possible.")
        }
        if(modifierCode == null) {
            return DimensionImpl.getBuiltinDimension(name)
        }
        if(modifierDimension == null) {
            modifierDimension = ModifierDimension.get(name, modifierCode, classForType(), size, density, packable)
        }
        return modifierDimension
    }

    private Class classForType() {
        switch(valueType) {
            case TYPE_TEXT:
                return String
            case TYPE_NUMBER:
                return Double
            default:
                throw new RuntimeException("Unsupported value type: ${valueType}. Should be one of [${TYPE_NUMBER}, ${TYPE_TEXT}].")
        }
    }

    /**
     * Retrieve all dimensions that are available in this database for all studies
     * @return a list of all dimensions
     */
    static List<Dimension> getAllDimensions() {
        findAll { name != LEGACY_MARKER }*.dimension
    }
}


@InheritConstructors
class LegacyStudyException extends UnsupportedOperationException {}
