/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.metadata

import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.ModifierDimension

import static org.transmartproject.db.i2b2data.ObservationFact.TYPE_DATE
import static org.transmartproject.db.i2b2data.ObservationFact.TYPE_NUMBER
import static org.transmartproject.db.i2b2data.ObservationFact.TYPE_RAW_TEXT
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
        table   schema: 'i2b2metadata'
        version false
        name    unique: true

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
            } else if(!ObservationFact.ALL_TYPES.contains(valueType)) {
                throw new DataInconsistencyException("Inconsistent metadata in DimensionDescription: '$name' " +
                        "dimension contains an unrecognized valueType '$valueType'.")
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
            modifierDimension = ModifierDimension.get(name, modifierCode, valueType, size, density, packable)
        }
        return modifierDimension
    }

    static Class classForType(String valueType) {
        switch(valueType) {
            case TYPE_TEXT:
            case TYPE_RAW_TEXT:
                return String
            case TYPE_NUMBER:
                return Double
            case TYPE_DATE:
                return Date
            default:
                throw new RuntimeException("Unsupported value type: ${valueType}. Should be one of [${TYPE_NUMBER}, ${TYPE_TEXT}, ${TYPE_RAW_TEXT}].")
        }
    }

    static DimensionDescription getDimensionByModifierCode(String modifierCode) {
        findByModifierCode(modifierCode)
    }

    /**
     * Retrieve all dimensions that are available in this database for all studies
     * @return a list of all dimensions
     */
    static List<Dimension> getAllDimensions() {
        findAll { name != DimensionDescription.LEGACY_MARKER }*.dimension
    }
}
