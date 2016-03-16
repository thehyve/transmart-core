package org.transmartproject.batch.tag

import org.springframework.batch.item.file.mapping.FieldSetMapper
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.validation.BindException

/**
 * Mapper class for tag types.
 */
class TagTypeMapper implements FieldSetMapper<TagType> {

    DelimitedLineTokenizer valuesTokenizer = new DelimitedLineTokenizer()

    @Override
    TagType mapFieldSet(FieldSet fieldSet) throws BindException {
        def tagType = new TagType(
                nodeType: fieldSet.readString('node_type'),
                title: fieldSet.readString('title'),
                solrFieldName: fieldSet.readString('solr_field_name'),
                valueType: fieldSet.readString('value_type'),
                values: valuesTokenizer.tokenize(fieldSet.readString('values')).values*.trim().toList(),
                index: { String i -> i.isEmpty() ? null : i.toInteger() }(
                        fieldSet.readString('index')
                )
        )
        tagType.shownIfEmpty = {
            def value = fieldSet.readString('shown_if_empty')
            if (value == 'Y') {
                return true
            } else if (value == 'N') {
                return false
            }
            def e = new BindException(tagType, "tagType")
            e.rejectValue('shownIfEmpty', 'invalid', "Invalid input for field 'shown_if_empty'")
            throw e
        }()
        tagType
    }
}
