package org.transmartproject.core.biomarker

interface BioMarker {

    Long getId()

    String getType()

    String getPrimaryExternalId()

    String getPrimarySourceCode()

    String getName()

    String getDescription()

    String getOrganism()
}


