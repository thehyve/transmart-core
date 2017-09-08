package config

import org.transmartproject.rest.hypercubeProto.ObservationsProto
import selectors.ObservationsMessageProto

class ProtobufHelper {
    static def parse(s_in) {
        def header = ObservationsProto.Header.parseDelimitedFrom(s_in)
        if (header.error) throw new RuntimeException("Error in protobuf header message: " + header.error)
        if (header.dimensionDeclarationsCount == 0) {
            return new ObservationsMessageProto()
        }
        if (Config.DEBUG) {
            println('proto header = ' + header)
        }
        boolean last = header.last
        def cells = []
        int count = 0
        while (!last) {
            count++
            def cell = ObservationsProto.Cell.parseDelimitedFrom(s_in)
            assert cell != null, "null cell found"
            if (cell.error) throw new RuntimeException("Error in protobuf cell message: " + cell.error)
            last = cell.last
            cells << cell
        }
        if (Config.DEBUG) {
            println('proto cells = ' + cells)
        }
        def footer = ObservationsProto.Footer.parseDelimitedFrom(s_in)
        if (footer.error) throw new RuntimeException("Error in protobuf footer message: " + footer.error)
        if (Config.DEBUG) {
            println('proto footer = ' + footer)
        }

        return new ObservationsMessageProto(header, cells, footer)
    }
}
