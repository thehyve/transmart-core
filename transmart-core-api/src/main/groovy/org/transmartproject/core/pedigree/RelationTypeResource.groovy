package org.transmartproject.core.pedigree

interface RelationTypeResource {

    /**
     * Returns all relation types in the system.
     */
    List<RelationType> getAll()

    /**
     * Returns the relation type with the label, if it exists.
     *
     * @param label the unique label of the relation type.
     * @return the relation type if it exists; null otherwise.
     */
    RelationType getByLabel(String label)

}
