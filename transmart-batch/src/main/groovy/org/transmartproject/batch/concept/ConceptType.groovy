package org.transmartproject.batch.concept

/**
 * The type of the concept.
 *
 * Actually, with the two values right now, this is more a VariableType, i.e.,
 * describes the type of a column mapping in clinical data. Concept types are
 * richer than this (high dimensional, simple folders and so on). Right now
 * it's for both, more with the meaning numerical vs. non-numerical.
 */
enum ConceptType {
    NUMERICAL,
    CATEGORICAL,
    HIGH_DIMENSIONAL,
    UNKNOWN,
}
