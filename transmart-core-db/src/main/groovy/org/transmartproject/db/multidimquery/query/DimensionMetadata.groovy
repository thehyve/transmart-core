/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.orm.hibernate.cfg.Mapping
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.dataquery.highdim.AssayColumnImpl
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter
import org.transmartproject.db.multidimquery.I2b2Dimension

import static org.transmartproject.db.multidimquery.DimensionImpl.ImplementationType.*


/**
 * Contains database mapping metadata for the dimensions.
 */
@CompileStatic
class DimensionMetadata {

    protected static final Logger log = LoggerFactory.getLogger(DimensionMetadata.class)

    static final Mapping observationFactMapping = GrailsDomainBinder.getMapping(ObservationFact)

    protected static final Map<Dimension, DimensionMetadata> dimensionMetadataMap = DimensionDescription.allDimensions.
            collectEntries([((Dimension) DimensionImpl.VALUE): new DimensionMetadata(DimensionImpl.VALUE)]) {
                [it, new DimensionMetadata(it)]
            }

    static final DimensionMetadata forDimensionName(String dimensionName) {
        def dim = DimensionImpl.fromName(dimensionName)
        if (dim == null) throw new QueryBuilderException("Dimension not found: ${dimensionName}")
        forDimension(dim)
    }

    static final DimensionMetadata forDimension(Dimension dimension) {
        dimensionMetadataMap[dimension]
    }

    static final Field getField(String dimensionName, String fieldName) {
        def metadata = forDimensionName(dimensionName)
        def field = metadata.fields.find { it.fieldName == fieldName }
        if (field == null) {
            throw new QueryBuilderException("Field '${fieldName}' not found in dimension ${metadata.dimension.name}")
        }
        field
    }

    static final List<Field> getSupportedFields() {
        dimensionMetadataMap.values().collectMany {
            (it.type in [COLUMN, TABLE, VISIT]) ? it.fields : [] as List<Field> }
    }

    DimensionImpl.ImplementationType getType() { dimension.implementationType }
    DimensionImpl dimension
    Class domainClass
    String getFieldName() {
        if(!(dimension instanceof I2b2Dimension)) return null
        def colName = ((I2b2Dimension) dimension).columnName
        if(colName.endsWith('.id')) {
            colName = colName[0..<-3]
        }
        colName
    }
    protected Mapping dimensionMapping
    List<Field> fields = []
    Map<String, Class> fieldTypes = [:]

    protected final Field getMappedField(String fieldName) {
        def field = domainClass.declaredFields.find { !it.synthetic && it.name == fieldName }
        if (field == null) {
            throw new QueryBuilderException("No field '${fieldName}' found in ${domainClass.simpleName}")
        }
        fieldTypes[fieldName] = field.type
        Type type = Type.OBJECT
        if (fieldName == 'id') {
            type = Type.ID
        } else if (Number.class.isAssignableFrom(field.type)) {
            type = Type.NUMERIC
        } else if (Date.class.isAssignableFrom(field.type)) {
            type = Type.DATE
        } else if (String.class.isAssignableFrom(field.type)) {
            type = Type.STRING
        }
        new Field(dimension: this.dimension, type: type, fieldName: field.name)
    }

    DimensionMetadata(Dimension dim) {
        this.dimension = (DimensionImpl) dim

        log.info "Registering dimension ${dim.name}..."

        if (type == TABLE) {
            def field = ObservationFact.declaredFields.find { !it.synthetic && it.name == fieldName }
            if (field == null) {
                throw new QueryBuilderException("No field with name ${fieldName} found in ${ObservationFact.simpleName}")
            } else {
                this.domainClass = field.type
                this.dimensionMapping = GrailsDomainBinder.getMapping(domainClass)
                this.fields = dimensionMapping.columns.keySet().collect { getMappedField(it) }
            }
        } else if (type == STUDY) {
            this.domainClass = Study
            this.dimensionMapping = GrailsDomainBinder.getMapping(Study)
            this.fields = dimensionMapping.columns.keySet().collect { getMappedField(it) }
        } else if (type == VISIT) {
            this.domainClass = org.transmartproject.db.i2b2data.VisitDimension
            this.dimensionMapping = GrailsDomainBinder.getMapping(org.transmartproject.db.i2b2data.VisitDimension)
            this.fields = dimensionMapping.columns.keySet().collect { getMappedField(it) }
        } else if (type == BIOMARKER) {
            this.domainClass = HddTabularResultHypercubeAdapter.BioMarkerAdapter
            this.fields = domainClass.declaredFields.findAll { !it.synthetic }*.name.collect { getMappedField(it) }
        } else if (type == ASSAY) {
            this.domainClass = AssayColumnImpl
            this.fields = domainClass.declaredFields.findAll { !it.synthetic }*.name.collect { getMappedField(it) }
        } else if (type == PROJECTION) {
            this.domainClass = Object
        } else {
            this.domainClass = ObservationFact
            this.dimensionMapping = observationFactMapping
            switch (type) {
                case MODIFIER:
                    this.fields << getMappedField('modifierCd')
                    //fallthrough
                case VALUE:
                    this.fields << getMappedField('valueType')
                    this.fields << getMappedField('textValue')
                    this.fields << getMappedField('numberValue')
                    break
                case COLUMN:
                    this.fields << getMappedField(fieldName)
                    break
                default:
                    throw new QueryBuilderException("Unexpected fetch type for dimension '${dim.name}'")
            }
        }
        log.info "Done for dimension '${dim.name}'."
    }
}
