package org.transmartproject.batch.batchartifacts

import org.springframework.batch.item.file.mapping.FieldSetMapper

/**
 * Interface that is used to map data obtained from a {@link org.springframework.batch.item.file.transform.FieldSet}
 * into a list of objects.
 */
interface MultipleItemsFieldSetMapper<T> extends FieldSetMapper<List<T>> {}
