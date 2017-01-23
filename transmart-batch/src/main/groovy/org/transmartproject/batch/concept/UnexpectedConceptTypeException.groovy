package org.transmartproject.batch.concept

/**
 * To be thrown by {@link ConceptTree} when there is a concept type mismatch.
 */
class UnexpectedConceptTypeException extends RuntimeException {

    UnexpectedConceptTypeException(ConceptType expected,
                                   ConceptType gotten,
                                   ConceptPath path) {
        super("Asked for node $path with type $expected, " +
                "but the one I know has type $gotten")
    }
}
