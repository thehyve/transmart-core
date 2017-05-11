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
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter
import org.transmartproject.db.multidimquery.ModifierDimension

import static org.transmartproject.db.multidimquery.query.DimensionFetchType.*

/**
 * Metadata about the fetching method for the dimension.
 * The dimension may be represented by a dimension table (<code>TABLE</code>),
 * a column in the {@link org.transmartproject.db.i2b2data.ObservationFact} table (<code>COLUMN</code>) or as
 * a modifier, which means that the data is stored in another, related, row
 * in the {@link org.transmartproject.db.i2b2data.ObservationFact} table (<code>MODIFIER</code>).
 */
enum DimensionFetchType {
    TABLE,
    COLUMN,
    VALUE,
    MODIFIER,
    STUDY,
    VISIT,
    BIOMARKER,
    ASSAY
}


//Note: information that is not specific to the constraint building and query code should live in the Dimension objects
enum ConstraintDimension {
    Patient(DimensionImpl.PATIENT,         TABLE,  'patient'),
    Concept(DimensionImpl.CONCEPT,         COLUMN, 'conceptCode'),
    Visit(DimensionImpl.VISIT,             VISIT,  ''),
    TrialVisit(DimensionImpl.TRIAL_VISIT,  TABLE,  'trialVisit'),
    Study(DimensionImpl.STUDY,             STUDY,  ''),
    Location(DimensionImpl.LOCATION,       COLUMN, 'locationCd'),
    Provider(DimensionImpl.PROVIDER,       COLUMN, 'providerId'),
    StartTime(DimensionImpl.START_TIME,    COLUMN, 'startDate'),
    EndTime(DimensionImpl.END_TIME,        COLUMN, 'endDate'),
    BioMarker(DimensionImpl.BIOMARKER,     BIOMARKER, ''),
    Assay(DimensionImpl.ASSAY,             ASSAY,  ''),

    // these are pseudo dimensions
    Modifier('modifier',                        MODIFIER, ''),
    Value('value',                              VALUE,  '')

    ConstraintDimension(Dimension dimension, DimensionFetchType type, String fieldName) {
        this(dimension.name, type, fieldName, dimension)
    }

    ConstraintDimension(String name, DimensionFetchType type, String fieldName, Dimension dimension=null) {
        this.dimension = dimension
        this.name = name
        this.type = type
        this.fieldName = fieldName
    }

    final Dimension dimension
    final String name
    final DimensionFetchType type
    final String fieldName;

    static private Map<String, ConstraintDimension> nameMap = values().collectEntries { [it.name, it] }
    static ConstraintDimension forName(String name) { nameMap[name] }

    static ConstraintDimension forDimension(Dimension dimension) {
        if(dimension instanceof ModifierDimension) {
            return Modifier
        } else {
            return nameMap[dimension.name]
        }
    }
}

/**
 * Contains database mapping metadata for the dimensions.
 */
@CompileStatic
class DimensionMetadata {

    protected static final Logger log = LoggerFactory.getLogger(DimensionMetadata.class)

    static final Mapping observationFactMapping = GrailsDomainBinder.getMapping(ObservationFact)

    protected static final Map<ConstraintDimension, DimensionMetadata> dimensionMetadataMap =
            ConstraintDimension.values().collectEntries { [it, new DimensionMetadata(it)] }

    static final DimensionMetadata forDimensionName(String dimensionName) {
        def dim = ConstraintDimension.forName(dimensionName)
        if (dim == null) throw new QueryBuilderException("ConstraintDimension not found: ${dimensionName}")
        dimensionMetadataMap[dim]
    }

    static final DimensionMetadata forDimension(Dimension dimension) {
        forDimension(ConstraintDimension.forDimension(dimension))
    }

    static final DimensionMetadata forDimension(ConstraintDimension dimension) {
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

    DimensionFetchType type
    ConstraintDimension dimension
    Class domainClass
    String fieldName
    String columnName
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

    DimensionMetadata(ConstraintDimension dim) {
        this.dimension = dim
        this.type = dim.type
        this.fieldName = dim.fieldName

        log.info "Registering dimension ${dim.name}..."
        if (!fieldName.empty) {
            this.columnName = observationFactMapping.columns[fieldName]?.column
        }

        if (type == DimensionFetchType.TABLE) {
            def field = ObservationFact.declaredFields.find { !it.synthetic && it.name == fieldName }
            if (field == null) {
                throw new QueryBuilderException("No field with name ${fieldName} found in ${ObservationFact.simpleName}")
            } else {
                this.domainClass = field.type
                this.dimensionMapping = GrailsDomainBinder.getMapping(domainClass)
                this.fields = dimensionMapping.columns.keySet().collect { getMappedField(it) }
            }
        } else if (type == DimensionFetchType.STUDY) {
            this.domainClass = Study
            this.dimensionMapping = GrailsDomainBinder.getMapping(Study)
            this.fields = dimensionMapping.columns.keySet().collect { getMappedField(it) }
        } else if (type == DimensionFetchType.VISIT) {
            this.domainClass = org.transmartproject.db.i2b2data.VisitDimension.class
            this.dimensionMapping = GrailsDomainBinder.getMapping(org.transmartproject.db.i2b2data.VisitDimension)
            this.fields = dimensionMapping.columns.keySet().collect { getMappedField(it) }
        } else if (type == DimensionFetchType.BIOMARKER) {
            this.domainClass = HddTabularResultHypercubeAdapter.BioMarkerAdapter.class
            this.fields = domainClass.declaredFields.findAll { !it.synthetic }*.name.collect { getMappedField(it) }
        } else if (type == DimensionFetchType.ASSAY) {
            this.domainClass = AssayColumnImpl.class
            this.fields = domainClass.declaredFields.findAll { !it.synthetic }*.name.collect { getMappedField(it) }
        } else {
            this.domainClass = ObservationFact.class
            this.dimensionMapping = observationFactMapping
            switch (type) {
                case DimensionFetchType.MODIFIER:
                    this.fields << getMappedField('modifierCd')
                    //fallthrough
                case DimensionFetchType.VALUE:
                    this.fields << getMappedField('valueType')
                    this.fields << getMappedField('textValue')
                    this.fields << getMappedField('numberValue')
                    break
                case DimensionFetchType.COLUMN:
                    this.fields << getMappedField(fieldName)
                    break
                default:
                    throw new QueryBuilderException("Unexpected fetch type for dimension '${dim.name}'")
            }
        }
        log.info "Done for dimension '${dim.name}'."
    }
}
