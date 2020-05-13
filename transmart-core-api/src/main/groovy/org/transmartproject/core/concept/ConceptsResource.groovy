package org.transmartproject.core.concept

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.User

interface ConceptsResource {

    /**
     * Returns all concepts the user has access to.
     *
     * @return the concepts.
     */
    List<Concept> getConcepts(User user)

    /**
     * Returns the concept with the given concept code.
     *
     * @param conceptCode the (unique) concept code of the concept
     * @return the concept with the concept code, if it exists and the user has access to the concept.
     * @throws NoSuchResourceException iff no concept can be found with the concept code or the user does not have
     * access to the concept.
     */
    Concept getConceptByConceptCodeForUser(String conceptCode, User user) throws NoSuchResourceException

    /**
     * Returns the concept with the given concept code.
     *
     * @param conceptCode the (unique) concept code of the concept
     * @return the concept with the concept code, if it exists.
     * @throws org.transmartproject.core.exceptions.NoSuchResourceException iff no concept can be found with the concept code.
     */
    Concept getConceptByConceptCode(String conceptCode) throws NoSuchResourceException

    /**
     * Returns the concept with the given concept path.
     *
     * @param conceptPath the (unique) concept path of the concept
     * @return the concept with the concept path, if it exists.
     * @throws NoSuchResourceException iff no concept can be found with the concept path.
     */
    Concept getConceptByConceptPath(String conceptPath) throws NoSuchResourceException

    /**
     * Returns the concept code of the concept with the given concept path.
     *
     * @param conceptPath the (unique) concept path of the concept
     * @return the concept code of the concept with the concept path, if it exists.
     * @throws NoSuchResourceException iff no concept can be found with the concept path.
     */
    String getConceptCodeByConceptPath(String conceptPath) throws NoSuchResourceException

    List<String> getModifiersByConceptCode(String conceptCode) throws NoSuchResourceException
}
