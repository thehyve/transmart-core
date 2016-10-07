package org.transmartproject.db.dataquery2.query

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.grails.orm.hibernate.cfg.Identity
import org.grails.orm.hibernate.cfg.Mapping
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.grails.orm.hibernate.cfg.Table
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.transmartproject.db.dataquery2.ConceptDimension
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.ModifierDimension
import org.transmartproject.db.dataquery2.PatientDimension
import org.transmartproject.db.dataquery2.StartTimeDimension
import org.transmartproject.db.dataquery2.TrialVisitDimension
import org.transmartproject.db.i2b2data.ObservationFact

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
    MODIFIER
}

@InheritConstructors
abstract class ValueDimension extends Dimension {}

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
            [dimensionClass: TrialVisitDimension.class, type: DimensionFetchType.TABLE,     fieldName: 'trialVisit'],
            [dimensionClass: StartTimeDimension.class,  type: DimensionFetchType.COLUMN,    fieldName: 'startDate'],
            [dimensionClass: ModifierDimension.class,   type: DimensionFetchType.MODIFIER,  fieldName: ''],
            [dimensionClass: ValueDimension.class,      type: DimensionFetchType.VALUE,     fieldName: '']
    ].collectEntries {
        [(((Class<? extends Dimension>)it.dimensionClass).simpleName): new DimensionMetadata(
                (Class) it.dimensionClass,
                (DimensionFetchType) it.type,
                (String) it.fieldName)
        ]
    } as Map<String, DimensionMetadata>

    static final DimensionMetadata forDimensionClassName(String dimensionClassName) {
        def metadata = dimensionMetadataMap[dimensionClassName]
        if (metadata == null) {
            throw new QueryBuilderException("Dimension class not found: ${dimensionClassName}")
        }
        metadata
    }

    static final DimensionMetadata forDimension(Class<? extends Dimension> dimensionClass) {
        forDimensionClassName(dimensionClass?.simpleName)
    }

    static final String getColumnForField(Field field) {
        log.info "Get column for field: ${field.dimension.simpleName}.${field.fieldName}"
        def metadata = forDimension(field.dimension)
        PropertyConfig columnMetadata = metadata.dimensionMapping.columns[field.fieldName]
        if (columnMetadata == null) {
            throw new QueryBuilderException("Field not found in dimension '${field.dimension.simpleName}': ${field.fieldName}")
        }
        columnMetadata.column
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
        def fields = []
        dimensionMetadataMap.each { name, metadata ->
            fields.addAll(metadata.fields)
        }
        fields
    }

    DimensionFetchType type
    Class<? extends Dimension> dimension
    Class domainClass
    String fieldName
    String columnName
    String dimensionTableName
    String dimensionIdColumn
    protected Mapping dimensionMapping
    List<Field> fields = []
    Map<String, Class> fieldTypes = [:]

    protected final List<Field> getMappedFields() {
        def mappedFields = []
        dimensionMapping.columns.keySet().each { fieldName ->
            mappedFields << getMappedField(fieldName)
        }
        mappedFields
    }

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
                Table table = dimensionMapping.table
                this.dimensionTableName = "${table.schema}.${table.name}"
                Identity dimensionId = (Identity) dimensionMapping.identity
                this.dimensionIdColumn = dimensionMapping.columns[dimensionId.column]?.column
                this.fields = mappedFields
            }
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
