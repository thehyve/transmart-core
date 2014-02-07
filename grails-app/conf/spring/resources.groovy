import org.transmartproject.webservices.TransmartMarshallerRegistrar
import org.transmartproject.webservices.StudyJsonMarshaller
import org.transmartproject.webservices.ObservationJsonMarshaller
import org.transmartproject.webservices.SubjectJsonMarshaller
import org.transmartproject.webservices.ConceptDimensionJsonMarshaller

// Place your Spring DSL code here
beans = {
	transmartMarshallerRegistrar(TransmartMarshallerRegistrar) {
		marshallers = [
			new StudyJsonMarshaller(),
			new ObservationJsonMarshaller(),
			new SubjectJsonMarshaller(),
			new ConceptDimensionJsonMarshaller(),
		]
	}
}
