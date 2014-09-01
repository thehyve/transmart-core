package example

import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

/**
 * Returns the inputs set to the {@link #inputs} property.
 * 
 * @author Robert Kasanicky
 */
@Component('reader')
class ExampleItemReader implements ItemReader<Object> {

	List inputs = ["hello", "world"]
	
	private int index = 0
	
	/**
	 * Reads next record from input
	 */
	Object read() {
		index < inputs.size() ? inputs[index++] : null
	}

}
