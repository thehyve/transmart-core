package org.transmartproject.core.biomarker

/**
 * The type returned by
 * {@link BioMarkersResource#retrieveBioMarkers(List<BioMarkerConstraint>)}.
 *
 * In general, the iterator can be fetched only once.
 */
interface BioMarkersResult extends Closeable, Iterable<BioMarker> {
}
