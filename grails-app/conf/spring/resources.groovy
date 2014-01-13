import org.transmartproject.webservices.TransmartMarshallerRegistrar
import org.transmartproject.webservices.StudyJsonMarshaller
import org.transmartproject.webservices.ObservationJsonMarshaller

// Place your Spring DSL code here
beans = {
	transmartMarshallerRegistrar(TransmartMarshallerRegistrar) {
		marshallers = [
			new StudyJsonMarshaller(),
			new ObservationJsonMarshaller()
		]
	}
}
