package org.transmartproject.core.dataquery.highdim.tworegion

/**
 * Represents metadata about one junction of a two region variant.
 * Junction can be contained in 0 or more events
 */
interface Junction {
    /**
     * end of up stream fusion partner
     */
    Long getUpEnd()

    /**
     * chromosome of up stream fusion partner
     */
    String getUpChromosome()

    /**
     * location of up stream fusion partner''s junction point
     */
    Long getUpPos()

    /**
     * strand of up stream junction, 1 for +, 0 for -
     */
    Character getUpStrand()

    /**
     * end of down stream junction
     */
    Long getDownEnd()

    /**
     * chromosome of down stream fusion partner
     */
    String getDownChromosome()

    /**
     * location of down stream junction point
     */
    Long getDownPos()

    /**
     * strand of down stream junction, 1 for +, 0 for -
     */
    Character getDownStrand()

    /**
     * whether junction is frame-shift (false) or in-frame-shift (true)
     */
    Boolean isInFrame()

    /**
     * Set of events into which the junction belongs
     */
    Set<JunctionEvent> getJunctionEvents()
}
