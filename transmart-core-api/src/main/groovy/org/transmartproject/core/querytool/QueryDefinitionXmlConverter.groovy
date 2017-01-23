package org.transmartproject.core.querytool

import org.transmartproject.core.exceptions.InvalidRequestException

/**
 * Defines an interface to for serializing and unserializing QueryDefinition
 * objects to and from XML.
 *
 * The reason this interface exists, instead of being an implementation detail
 * of {@link QueriesResource} or a the control backed by it is that it is
 * useful to have both 1) {@link QueriesResource}, which is part of this API,
 * using this interface to serialize the definition to the database and 2) the
 * controller backed by {@link QueriesResource}, which is in WAR application,
 * using this interface to exchange messages with the browser. Therefore, this
 * interface is useful to both the implementation and the clients of this API.
 */
interface QueryDefinitionXmlConverter {

    QueryDefinition fromXml(Reader reader) throws InvalidRequestException

    String toXml(QueryDefinition definition)
}
