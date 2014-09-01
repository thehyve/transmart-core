package example

import spock.lang.Specification

class ExampleItemReaderSpec extends Specification {

	def read() {
		
		given:
			def reader = new ExampleItemReader(inputs: ['hello', 'world'])
			
		expect:	"hard-coded inputs are returned in expected sequence, followed by null"
			reader.read() == 'hello'
			reader.read() == 'world'
			reader.read() == null
			reader.read() == null
			
	}

}
