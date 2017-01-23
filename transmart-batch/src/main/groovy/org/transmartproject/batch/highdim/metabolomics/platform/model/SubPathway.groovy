package org.transmartproject.batch.highdim.metabolomics.platform.model
/**
 * A metabolomics sub pathway.
 */
class SubPathway implements CaseInsensitiveNameBasedEqualityTrait {
    Long id
    String name
    SuperPathway superPathway
}
