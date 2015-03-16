package org.transmartproject.batch.i2b2.dimensions

import groovy.transform.Immutable

/**
 * Represents all the data known in the {@link DimensionsStore} about a
 * particular dimensions object
 */
@Immutable
class DimensionsStoreEntry {
    String externalId
    String internalId
    boolean sawData
    boolean knownAsExisting
    String extraData
}
