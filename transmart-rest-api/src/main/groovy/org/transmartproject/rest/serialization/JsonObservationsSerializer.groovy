package org.transmartproject.rest.serialization

import grails.converters.JSON
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.multidimquery.EndTimeDimension
import org.transmartproject.db.multidimquery.LocationDimension
import org.transmartproject.db.multidimquery.ModifierDimension
import org.transmartproject.db.multidimquery.ProjectionDimension
import org.transmartproject.db.multidimquery.ProviderDimension
import org.transmartproject.db.multidimquery.StartTimeDimension
import org.transmartproject.db.multidimquery.query.DimensionMetadata

/**
 * <code>
 *                            ...,.,:,;lokxcl:;
 *                       .;ll:lOk,..    ..  .l0k:
 *                     .cd,cdo;;;c          .d:OO.
 *                     lc:::,..,.           ox;.
 *                    :l'ldc.',             xk.
 *                    o',x:.  ...           .,o:cl
 *                   ll.'l::'                    :c
 *                   x';,,;,. .'               .,;k'
 *                  'x  .'.   ,     ...  ,ll;lk0ck0K    .;::c.
 *                  ld  .   .,:   ;dx0o;ookXKkcc;,x0. .ooooxd;
 *                  l,     .lN:   :KXK00X0dc.lckOOkKlldxOc'.
 *                  oO.     cO0.  'kdcdKK,     'c.'odOd;
 *                   k,     ;Ooo .:x00Kc        ;;;Ox
 *                   .:o.   .x';;:Kxxdc.        lKk;
 *                     .x'   'o .kkOx; .'       cKl
 *                      :c.   0. l,'::  .,,. ..,'x.
 *                      :l,  .Nc;kclk;    .l,'kk.
 *                      ;l. .;O,0ON0d.       .;o
 *                      :, .'ddoXdol'       ...d.
 *                      .:.l,''ckX;d.          c:.....'c:;ccc:;:,.
 *                       oco:. ,kdc.           .,,,.....        .ol
 *                      .kkc;c0O.                                 .x.
 *                   .co:.;o0Oc,..                                  k.
 *                  .0c.,dkkd.    .''.                              :l
 *                .cc:lO:x0c         ..''                    .'.    :d
 *           cxold; oo' ;Oo.            .::;.               .llx:   ld
 *          o:.:d0oko   ,o., ,             .';,.          '.:: .k   lk
 *        'l: ..o0cdo.  .c.;;..                ,:.       ..    .x    0;
 *       dKocxdkxO,  ,   .dx..                   .,'.        . .d.   lk
 *     .kx::OXk0k,  .:     ;  '.                    ';,.    ,: ck.    X'
 *      klkKo0l    ;':.    ..                         .;,   ;' ,l     kc
 * </code>
 *
 * (original by Bertel Thorvaldsen - Image:ThorvaldsensJason.jpg, Public Domain,
 * https://commons.wikimedia.org/w/index.php?curid=4659064.)
 *
 * Serializes a {@link Hypercube} to JSON format.
 */
@CompileStatic
class JsonObservationsSerializer extends AbstractObservationsSerializer {

    /**
     * Type of dimensions and fields to serialize.
     */
    static enum Type {
        DOUBLE('Double'),
        INTEGER('Integer'),
        STRING('String'),
        DATE('Date'),
        OBJECT('Object'),
        ID('Id')

        String typeName

        Type(String typeName) {
            this.typeName = typeName
        }

        @Override
        String toString() {
            typeName
        }
    }

    /**
     * Determines the type of a field based on its declared class.
     *
     * Numeric types {@link Float} and {@link Double} are mapped to {@link Type#DOUBLE};
     * other values of type {@link Number} to {@link Type#INTEGER.
     * {@link Date} maps to {@link Type#DATE}, String to {@link Type#STRING}.
     * Others map to {@link Type#ID}, which means that the <code>id</code> field (assumed to
     * be numeric) will be used as serialization.
     *
     * @param type the declared type of the field in the class where it is declared.
     * @return the serialization type.
     */
    static Type getFieldType(Class type) {
        if (Float.isAssignableFrom(type)) {
            return Type.DOUBLE
        } else if (Double.isAssignableFrom(type)) {
            return Type.DOUBLE
        } else if (Number.isAssignableFrom(type)) {
            return Type.INTEGER
        } else if (Date.isAssignableFrom(type)) {
            return Type.DATE
        } else if (String.isAssignableFrom(type)) {
            return Type.STRING
        } else {
            // refer to objects by their identifier
            return Type.ID
        }
    }

    /**
     * Contains information about a field of a dimension
     */
    static class Field {
        String name
        Type type

        final Map<String, Object> toMap() {
            [
                    name: name,
                    type: type
            ] as Map<String, Object>
        }
    }

    /**
     * Contains information about a dimension
     */
    static class DimensionProperties {
        String name
        Type type
        List<Field> fields
        boolean inline = false

        final Map<String, Object> toMap() {
            def map = [
                    name: name,
                    type: type
            ] as Map<String, Object>
            if (fields) {
                map['fields'] = fields.collect { it.toMap() }
            }
            if (inline) {
                map['inline'] = Boolean.TRUE
            }
            map
        }
    }

    /**
     * Contains information about an observation.
     */
    static class Cell {
        /**
         * The list of inlined values of this observation.
         */
        List<Object> inlineDimensions = []
        /**
         * The list of indexes of indexed values of this observation.
         */
        List<Long> dimensionIndexes = []
        /**
         * The numeric value of this observation if it is numeric.
         */
        Number numericValue
        /**
         * The string value of this observation if it is of type text.
         */
        String stringValue

        final Map<String, Object> toMap() {
            def map = [
                    inlineDimensions: inlineDimensions,
                    dimensionIndexes: dimensionIndexes
            ] as Map<String, Object>
            if (numericValue != null) {
                map['numericValue'] = numericValue
            } else if (stringValue) {
                map['stringValue'] = stringValue
            }
            map
        }
    }

    protected Writer writer
    protected Map<Dimension, DimensionProperties> dimensionDeclarations = [:]

    JsonObservationsSerializer(Hypercube cube) {
        super(cube)
    }

    @Override
    protected void begin(OutputStream out) {
        writer = new PrintWriter(new BufferedOutputStream(out))
        writer.print('{\n')
    }

    @Override
    protected void end(OutputStream out) {
        writer.print('\n}\n')
        writer.flush()
    }

    @Override
    protected void writeEmptyMessage(OutputStream out) {
        // skip
    }

    /**
     * Build a value object based on the dimension or field type and its value.
     * Checks if the value is of a supported type.
     * Uses the <code>id</code> field if the
     * @return
     */
    static Object buildValue(Type type, Object value) {
        switch (type) {
            case Type.DATE:
                if (value == null) {
                    return null
                } else if (value instanceof Date) {
                    return value
                } else if (value instanceof Number) {
                    return new Date(value.longValue())
                } else {
                    return null
                }
            case Type.DOUBLE:
                if (value instanceof Float) {
                    return value
                } else if (value instanceof Double) {
                    return value
                } else {
                    throw new Exception("Type not supported: ${value?.class?.simpleName}.")
                }
            case Type.INTEGER:
                if (value == null) {
                    return null
                } else if (value instanceof Number) {
                    return value
                } else {
                    Long id = value?.getAt('id') as Long
                    return id
                }
            case Type.ID:
                Long id = value?.getAt('id') as Long
                return id
            case Type.STRING:
                return value?.toString()
            default:
                throw new Exception("Type not supported: ${type.name()}.")
        }
    }

    /**
     * Build an dimensional object to serialize using the field descriptions of the dimension.
     * @param dim the dimension to serialize the object for.
     * @param dimElement the value to serialize.
     * @return an object to use for writing.
     */
    protected Object buildDimensionElement(Dimension dim, Object dimElement) {
        def dimensionProperties = dimensionDeclarations[dim]
        if (dimensionProperties.type == Type.OBJECT) {
            if (dimElement == null) {
                return null
            }
            def value = [:] as Map<String, Object>
            dimensionProperties.fields?.each { field ->
                value[field.name] = buildValue(field.type, dimElement[field.name])
            }
            return value
        } else {
            return buildValue(dimensionProperties.type, dimElement)
        }
    }

    /**
     * Create a cell with either numeric or string value
     * and inlined values for the inlined dimensions and indexes for the other dimensions.
     * @param value the hypercube value.
     * @return the cell representing the serialised value.
     */
    protected Cell createCell(HypercubeValue value) {
        def cell = new Cell()
        if (value.value != null) {
            if (value.value instanceof Number) {
                cell.numericValue = value.value as Double
            } else {
                cell.stringValue = value.value as String
            }
        }
        for (Dimension dim : cube.dimensions) {
            Object dimElement = value[dim]
            if (dim.density == Dimension.Density.SPARSE) {
                // Add the value element inline
                cell.inlineDimensions << buildDimensionElement(dim, dimElement)
            } else {
                // Add index to footer element inline
                def dimValue = buildDimensionElement(dim, dimElement)
                cell.dimensionIndexes << determineFooterIndex(dim, dimValue)
            }
        }
        cell
    }

    @Override
    protected void writeCells(OutputStream out, Iterator<HypercubeValue> it) {
        writer.print('"cells": [')
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            def message = createCell(value)
            writer.print(message.toMap() as JSON)
            def last = !it.hasNext()
            if (!last) {
                writer.print(',')
            }
        }
        writer.print('],\n')
    }

    /**
     * Build the list of {@link DimensionProperties} to be serialized in the header.
     * Also, the {@link #dimensionDeclarations} map is populated, which is used during
     * serialization of the cells.
     * @return a list of dimension declarations.
     */
    protected List<DimensionProperties> buildDimensionDeclarations() {
        def declarations = cube.dimensions.collect { dim ->
            def dimensionProperties = new DimensionProperties()
            if (dim instanceof ModifierDimension) {
                dimensionProperties.name = dim.name
            } else {
                dimensionProperties.name = dim.toString()
            }
            if (dim.density == Dimension.Density.SPARSE) {
                // Sparse dimensions are inlined, dense dimensions are referred to by indexes
                // (referring to objects in the footer message).
                dimensionProperties.inline = true
            }
            def publicFacingFields = SerializableProperties.SERIALIZABLES.get(dimensionProperties.name)
            switch(dim.class) {
                case ModifierDimension:
                    def modifierDim = (ModifierDimension)dim
                    switch (modifierDim.elementType) {
                        case Double:
                            dimensionProperties.type = Type.DOUBLE
                            break
                        case String:
                            dimensionProperties.type = Type.STRING
                            break
                        default:
                            throw new Exception("Unsupported value type for dimension ${dimensionProperties.name}: ${modifierDim.elementType}.")
                    }
                    break
                case StartTimeDimension:
                case EndTimeDimension:
                    dimensionProperties.type = Type.DATE
                    break
                case ProviderDimension:
                case LocationDimension:
                case ProjectionDimension:
                    dimensionProperties.type = Type.STRING
                    break
                default:
                    dimensionProperties.type = Type.OBJECT
                    def metadata = DimensionMetadata.forDimension(dim.class)
                    dimensionProperties.fields = metadata.fields.findAll { field ->
                        field.fieldName in publicFacingFields
                    }.collect { field ->
                        Class valueType = metadata.fieldTypes[field.fieldName]
                        new Field(
                                name: field.fieldName,
                                type: getFieldType(valueType)
                        )
                    }
                    break
            }
            dimensionDeclarations[dim] = dimensionProperties
            dimensionProperties
        }
        declarations
    }

    @Override
    protected void writeHeader(OutputStream out) {
        writer.print('"header": ')
        def declarations = buildDimensionDeclarations()
        def headerContents = [
                dimensionDeclarations: declarations.collect { it.toMap() }
        ]
        writer.print(headerContents as JSON)
        writer.print(',\n')
    }

    @Override
    protected void writeFooter(OutputStream out) {
        writer.print('"footer": ')
        def dimensionElementsList = cube.dimensions.findAll({
            it.density != Dimension.Density.SPARSE
        }).collect { dim ->
            dimensionElements[dim]
        }
        def footerContents = [
                dimensions: dimensionElementsList
        ]
        writer.print(footerContents as JSON)
    }
}
