package org.transmartproject.batch.tag

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents a Tag Type, as defined in tag type file
 */
@ToString
@EqualsAndHashCode(excludes = ['id'])
class TagType implements Serializable {

    private static final long serialVersionUID = 1L

    Integer id

    String nodeType // ALL, STUDY, FOLDER, CATEGORICAL, NUMERICAL, HIGHDIM, [any high dim data type]

    String title

    String solrFieldName

    String valueType // DATE, NON_ANALYZED_STRING, ANALYZED_STRING, INTEGER, FLOAT

    Boolean shownIfEmpty

    Collection<String> values

    Integer index

}

