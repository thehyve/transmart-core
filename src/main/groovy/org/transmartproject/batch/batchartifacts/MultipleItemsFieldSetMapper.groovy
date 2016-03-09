package org.transmartproject.batch.batchartifacts

import org.springframework.batch.item.file.mapping.FieldSetMapper

/**
 * Interface that is used to map data obtained from a {@link org.springframework.batch.item.file.transform.FieldSet}
 * into an collection of objects.
 */
interface MultipleItemsFieldSetMapper<T> extends FieldSetMapper<Collection<T>> {}
