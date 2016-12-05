package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.orm.hibernate.cfg.Mapping
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.db.dataquery.highdim.AssayColumnImpl
import org.transmartproject.db.multidimquery.AssayDimension
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.db.multidimquery.ConceptDimension
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.EndTimeDimension
import org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter
import org.transmartproject.db.multidimquery.LocationDimension
import org.transmartproject.db.multidimquery.ModifierDimension
import org.transmartproject.db.multidimquery.PatientDimension
import org.transmartproject.db.multidimquery.ProviderDimension
import org.transmartproject.db.multidimquery.StartTimeDimension
import org.transmartproject.db.multidimquery.StudyDimension
import org.transmartproject.db.multidimquery.TrialVisitDimension
import org.transmartproject.db.multidimquery.VisitDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study

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

@InheritConstructors
abstract class ValueDimension extends DimensionImpl {}

/**
 * Contains database mapping metadata for the dimensions.
 */
@CompileStatic
class DimensionMetadata {

    protected static final Logger log = LoggerFactory.getLogger(DimensionMetadata.class)

    static final Mapping observationFactMapping = GrailsDomainBinder.getMapping(ObservationFact)

    protected static final Map<String, DimensionMetadata> dimensionMetadataMap = [
            [dimensionClass: PatientDimension.class,    type: DimensionFetchType.TABLE,     fieldName: 'patient'],
            [dimensionClass: ConceptDimension.class,    type: DimensionFetchType.COLUMN,    fieldName: 'conceptCode'],
            [dimensionClass: VisitDimension.class,      type: DimensionFetchType.VISIT,     fieldName: ''],
            [dimensionClass: TrialVisitDimension.class, type: DimensionFetchType.TABLE,     fieldName: 'trialVisit'],
            [dimensionClass: StudyDimension.class,      type: DimensionFetchType.STUDY,     fieldName: ''],
            [dimensionClass: LocationDimension.class,   type: DimensionFetchType.COLUMN,    fieldName: 'locationCd'],
            [dimensionClass: ProviderDimension.class,   type: DimensionFetchType.COLUMN,    fieldName: 'providerId'],
            [dimensionClass: StartTimeDimension.class,  type: DimensionFetchType.COLUMN,    fieldName: 'startDate'],
            [dimensionClass: EndTimeDimension.class,    type: DimensionFetchType.COLUMN,    fieldName: 'endDate'],
            [dimensionClass: ModifierDimension.class,   type: DimensionFetchType.MODIFIER,  fieldName: ''],
            [dimensionClass: ValueDimension.class,      type: DimensionFetchType.VALUE,     fieldName: ''],
            [dimensionClass: BioMarkerDimension.class,  type: DimensionFetchType.BIOMARKER, fieldName: ''],
            [dimensionClass: AssayDimension.class,      type: DimensionFetchType.ASSAY,     fieldName: ''],
    ].collectEntries {
        [(((Class) it.dimensionClass).simpleName.toLowerCase()): new DimensionMetadata(
                (Class) it.dimensionClass,
                (DimensionFetchType) it.type,
                (String) it.fieldName)
        ]
    }

    static final DimensionMetadata forDimensionClassName(String dimensionClassName) {
        def metadata = dimensionMetadataMap[dimensionClassName.toLowerCase()]
        if (metadata == null) {
            throw new QueryBuilderException("Dimension class not found: ${dimensionClassName}")
        }
        metadata
    }

    static final DimensionMetadata forDimension(Class<? extends Dimension> dimensionClass) {
        forDimensionClassName(dimensionClass?.simpleName)
    }

    static final Field getField(Class<? extends Dimension> dimensionClass, String fieldName) {
        def metadata = forDimension(dimensionClass)
        def field = metadata.fields.find { it.fieldName == fieldName }
        if (field == null) {
            throw new QueryBuilderException("Field '${fieldName}' not found in class ${metadata.domainClass.simpleName}")
        }
        field
    }

    static final List<Field> getSupportedFields() {
        dimensionMetadataMap.values().collectMany {
            (it.type in [DimensionFetchType.COLUMN, DimensionFetchType.TABLE]) ? it.fields : [] as List<Field> }
    }

    DimensionFetchType type
    Class<? extends Dimension> dimension
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

    DimensionMetadata(Class<? extends Dimension> dimensionClass, DimensionFetchType type, String fieldName) {
        log.info "Registering dimension ${dimensionClass.simpleName}..."
        this.dimension = dimensionClass
        this.type = type
        this.fieldName = fieldName
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
            this.domainClass = Study.class
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
                case DimensionFetchType.VALUE:
                    this.fields << getMappedField('valueType')
                    this.fields << getMappedField('textValue')
                    this.fields << getMappedField('numberValue')
                    break
                case DimensionFetchType.COLUMN:
                    this.fields << getMappedField(fieldName)
                    break
                default:
                    throw new QueryBuilderException("Unexpected fetch type for dimension '${dimension.simpleName}'")
            }
        }
        log.info "Done for dimension '${dimension.simpleName}'."
    }
}
