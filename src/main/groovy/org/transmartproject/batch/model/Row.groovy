package org.transmartproject.batch.model

import groovy.transform.ToString

/**
 *
 */
@ToString(includes=['filename','index'])
class Row {
    String filename
    int index
    List<String> values
}
