package example

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes=[ExampleConfiguration])
class ExampleJobConfigurationSpec extends Specification {
	
	@Autowired
	private JobLauncher jobLauncher

	@Autowired
	private Job job
	
	void 'simple properties injection'() {
		
		expect: 
			jobLauncher != null
	}
	
	void 'launch job'() {
		
		when: "job is run"
			def jobExecution = jobLauncher.run(job, new JobParameters())
			
		then: "it completes successfully (execution is synchronous, so we don't have to wait for completion)"
			jobExecution.exitStatus == ExitStatus.COMPLETED
	}
	
}
