package org.transmartproject.rest.marshallers

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Marks a json marshaller for auto registration. These classes should have a
 * static property called `staticType` with type they're targeting for
 * marshalling and a convert method taking an object of the targeted  type and
 * returning another representation for the object (typically a map), which can
 * itself be marshaled.
 *
 * The registration is the responsibility of the {@link JsonMarshaller} class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsonMarshaller {}
