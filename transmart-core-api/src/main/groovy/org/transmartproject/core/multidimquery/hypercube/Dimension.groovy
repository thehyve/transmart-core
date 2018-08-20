/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery.hypercube

import com.google.common.collect.ImmutableMap

interface Dimension {

    /**
     * Indicates the typical size of a dimension. As this is typical, the actual size in a result set does not always
     * match this range.
     *
     * Small: up to 10 to 15 elements
     * Medium: 10 to 50 elements
     * Large: more than 50 elements
     */
    enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    enum Density {
        DENSE(true),
        SPARSE(false)

        public boolean isDense

        boolean isIsSparse() {
            !isDense
        }

        Density(boolean isDense) {
            this.isDense = isDense
        }
    }

    enum Packable {
        PACKABLE(true),
        NOT_PACKABLE(false)

        public boolean packable

        Packable(boolean packable) {
            this.packable = packable
        }
    }


    String getName()

    Size getSize()

    Density getDensity()

    Packable getPackable()

    List resolveElements(List elementKeys)

    def resolveElement(elementKey)

    /**
     * @return true if the elements of this dimension are serializable, meaning they are of type String, Date, or
     * Number.
     * Note that 'serializable' within this interface is a subset of the java.io.Serializable meaning. Serializable
     * here is limited to types that can be directly serialized by the rest-api, i.e. String, Date, or subclasses of
     * Number.
     */
    boolean getElementsSerializable()

    /**
     * @return if the element type is serializable, return the type (String, Date, or a subclass of Number). If the
     * elements are not serializable, returns null.
     */
    Class<? extends Serializable> getElementType()

    /**
     * @return for dimensions with non-serializable elements, an (ordered) immutable map with as keys the field names
     * that should be used for serialization, and as values instances of Property that can be used to retrieve the
     * property from an element.
     * If the elements of this dimension are serializable (according to getElementsSerializable()), this method returns
     * null.
     */
    ImmutableMap<String, Property> getElementFields()

    /**
     * Get an element's key, i.e. a simple value that uniquely identifies the element.
     * @param element
     * @return a simple object (Number, String, or Date) that is unique for this element and thus identifies it.
     */
    def getKey(element)

    /**
     * Returns a serializable view of an element. If the element is in fact a Number, String, or Date, it is returned
     * as-is. If it is a complex type, this method returns a map with String keys that holds the relevant properties,
     * which themselves are again of serializable types.
     *
     * @param element: an element returned from getElements or resolveElement(s)
     * @return a String, Number, Date or Map<String,something serializable>
     */
    def asSerializable(element)
}

/**
 * A property descriptor returned from getElementFields()
 */
interface Property {

    /** The name to use for this property externally, e.g. for serialization */
    String getName()

    /** The type of this property. This is the return type for `get` */
    Class getType()

    /** Given an element, return the value for this property */
    def get(element)
}
