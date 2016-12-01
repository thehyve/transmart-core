package protobuf

class ObservationsMessageProto {
    public final ObservationsProto.Header header
    public final List<ObservationsProto.Observation> cells
    public final ObservationsProto.Footer footer

    ObservationsMessageProto(ObservationsProto.Header header, List<ObservationsProto.Observation> cells, ObservationsProto.Footer footer) {
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
