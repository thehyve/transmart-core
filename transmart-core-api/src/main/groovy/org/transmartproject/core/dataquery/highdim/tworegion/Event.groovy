package org.transmartproject.core.dataquery.highdim.tworegion

/**
 * Represents metadata about one event
 */
interface Event {

    /**
     * deletion, inversion, duplication,... Type from <a href="http://cgatools.sourceforge.net/docs/1.8.0/cgatools-command-line-reference.html#junctions2events">CGA</a>
     */
    String getCgaType()

    /**
     * inter/intra chromosomal inversion/translocation: <a href="http://sourceforge.net/p/soapfuse/wiki/classification-of-fusions.for.SOAPfuse/">SOAP</a>
     */
    String getSoapClass()

    /**
     * Set of genes affected by this event
     */
    Set<EventGene> getEventGenes()
}
