package org.transmartproject.core.ontology

/**
 * An i2b2 ontology metadata entry.
 */
public interface OntologyTerm {

    /**
     * The hierarchical level of the term. The term at the highest level of a
     * hierarchy has a value of 0, the next level has a value of 1 and so on.
     *
     * @return non-negative integer representing the depth of the term
     */
    Integer getLevel()

    /**
     * Returns the term key; this is a string starting with \\,
     * followed by the the table code and the concept full name
     * @return \\<table code><full name>
     */
    String getKey()

    /**
     * The hierarchical path that leads to the term,
     * like <code>\i2b2\Diagnoses\Musculoskeletal and connective tissue (710)
     * \Arthropaties (710-19)\(714) Rheumatoid arthritis and other
     * arthropaties\(714-0) Rheumatoid arthritis</code>.
     *
     * @return the full path of the term; path with \ as a separator
     */
    String getFullName()

    /**
     * The user friendly name of the term.
     *
     * @return a term name to be displayed to the user
     */
    String getName()

    /**
     * A tooltip to appear in the user interface for this term.
     *
     * @return the term's tooltip
     */
    String getTooltip()

    /**
     * A set of up to three visual attributes that describe how the field
     * should look in the user interface.
     *
     * @return set of {@link VisualAttributes} enum instances
     */
    EnumSet<VisualAttributes> getVisualAttributes()

    /**
     * The term metadata, or null if none. The format of the metadata is not
     * specified.
     *
     * @return metadata associated with the term
     */
    Object getMetadata()


    /**
     * Returns the terms one level below that have this term as a parent.
     * Synonyms and hidden terms are not shown.
     *
     * @return (direct) children of this term, ordered by name
     */
    List<OntologyTerm> getChildren()

    /**
     * Returns the terms one level below that have this term as a parent.
     *
     * @param showHidden whether to return items with the hidden visual
     * attribute
     * @para showSynonyms whether to show synonyms
     * @return (direct) children of this term, ordered by name
     */
    List<OntologyTerm> getChildren(boolean showHidden, boolean showSynonyms)

    enum VisualAttributes {

        /**
         * Non-terminal term that can be used as a query item.
         */
        FOLDER              ('F' as Character),

        /**
         * Non-terminal term that cannot be used as a query item.
         */
        CONTAINER           ('C' as Character),

        /**
         * The term represents several terms, which are collapsed on it. An
         * example is under Gender in the Demographics folder &#2013; the
         * term 'Unknown' having this type indicates that there are at least
         * two terms that are considered to be 'Unknown Gender' and both are
         * mapped to that one.
         */
        MULTIPLE            ('M' as Character),

        /**
         * A terminal term.
         */
        LEAF                ('L' as Character),
        MODIFIER_CONTAINER  ('O' as Character),
        MODIFIER_FOLDER     ('D' as Character),
        MODIFIER_LEAF       ('R' as Character),


        /**
         * A term to be displayed normally.
         */
        ACTIVE              ('A' as Character),

        /**
         * A term that cannot be used.
         */
        INACTIVE            ('I' as Character),

        /**
         * The term is hidden from the user.
         */
        HIDDEN              ('H' as Character),


        /**
         * If present, the term can have children added to it and it can also
         * be deleted.
         */
        EDITABLE            ('E' as Character)

        char keyChar;

        protected VisualAttributes(char keyChar) {
            this.keyChar = keyChar
        }

        static VisualAttributes forKeyChar(char c) {
            values().find { it.keyChar == c }
        }
    }
}
