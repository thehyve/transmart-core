package org.transmartproject.batch.highdim.metabolomics.platform.model
/**
 * A metabolomics biochemical.
 */
class Biochemical implements CaseInsensitiveNameBasedEqualityTrait {
    Long id

    String name

    String hmdbId

    /* the database model suggests it could be associated with more
     * than one, but the input file doesn't support that. */
    SubPathway subPathway
}
