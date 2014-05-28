package org.transmartproject.core.dataquery.highdim

import org.transmartproject.core.dataquery.highdim.chromoregion.Region

/**
 * A platform record. A platform may be a platform proper (identifying an
 * array or sequencer) or it may group together several{@link Region}s.
 *
 * Array based platforms will have an associated array template (data about
 * its probes); region platforms will have an associated set of regions.
 */
public interface Platform {

    /**
     * A string that uniquely identifies the platform.
     *
     * @return the platform id
     */
    String getId()

    /**
     * A human-readable name for the platform.
     *
     * @return human-readable platform name.
     */
    String getTitle()

    /**
     * The scientific name for the organism, like 'Homo Sapiens'.
     *
     * @return the organism associated with this platform
     */
    String getOrganism()

    /**
     * A genome build release identifier.
     *
     * @return the genome build release identifier
     */
    String getGenomeReleaseId()

    /**
     * The time when this platform was imported to the database. Maybe.
     *
     * @return the platform annotation date
     */
    Date getAnnotationDate()

    /**
     * Indicates the type of platform or pseudo-platform,
     * like 'Chromosomal Region' or 'Gene Expression'. Each platform
     * is associated with a specific type of high dimensional data.
     *
     * @return the platform marker type
     */
    String getMarkerType()

    /**
     * Returns an iterable with the platform template entries. The type of
     * this entries depends on the marker type and is not constrained by this
     * API.
     *
     * If the marker type does not have an associated template or it's of
     * unknown type, then null is returned.
     *
     * @return the template entries for this platform or null
     */
    Iterable<?> getTemplate()

}
