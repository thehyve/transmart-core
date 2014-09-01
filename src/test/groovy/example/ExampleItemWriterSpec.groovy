package example

import spock.lang.Specification

class ExampleItemWriterSpec extends Specification {

	void 'write outputs'()  {
		
		given:
			def writer = new ExampleItemWriter()
			
		expect: "nothing bad happens when writing arbitrary objects"
			writer.write(["hello", "world"])
			writer.write([42, new Object()])
			writer.write(null)
	}
}
