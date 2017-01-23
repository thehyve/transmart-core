package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.validator.ValidationException
import org.springframework.batch.item.validator.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree

/**
 * Given a {@link Tag}, throws if the concept it refers to doesn't exist.
 * This is a Spring BATCH validator.
 */
@Component
@JobScope
@Slf4j
class TagConceptExistenceValidator implements Validator<Tag> {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Autowired
    ConceptTree tree

    @Override
    void validate(Tag tag) throws ValidationException {
        ConceptPath path = topNode + tag.conceptFragment
        if (tree[path] == null) {
            def message = "No notice of existence of concept ${path}"
            log.warn message
            throw new NoSuchConceptException(message)
        }
    }
}
