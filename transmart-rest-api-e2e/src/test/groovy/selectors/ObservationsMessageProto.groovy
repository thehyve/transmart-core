/* Copyright © 2017 The Hyve B.V. */
package selectors

import org.transmartproject.rest.hypercubeProto.ObservationsProto

class ObservationsMessageProto {
    public final ObservationsProto.Header header
    public final List<ObservationsProto.Cell> cells
    public final ObservationsProto.Footer footer

    ObservationsMessageProto(ObservationsProto.Header header, List<ObservationsProto.Cell> cells, ObservationsProto.Footer footer) {
        this.header = header
        this.cells = cells
        this.footer = footer
    }

    ObservationsMessageProto() {
        this.header = null
        this.cells = null
        this.footer = null
    }
}
