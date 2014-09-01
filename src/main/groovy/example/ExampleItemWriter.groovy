package example

import groovy.util.logging.Slf4j

import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
@Slf4j
@Component('writer')
class ExampleItemWriter implements ItemWriter<Object> {

	void write(List<? extends Object> data) {
		log.info "Writing chunk: $data"
	}
}

