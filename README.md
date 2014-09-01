
tranSMART Batch
============================

tranSMART pipeline alternative to ETL, using Spring Batch. 

Initial version was based on [Groovy-Spring-Batch-Template] (https://github.com/critsk/Groovy-Spring-Batch-Template)


Stack
------------------

<table>
<tr> <td>Language</td>			<td>Groovy</td> 	</tr>
<tr> <td>Build system</td>		<td>Gradle</td>		</tr>
<tr> <td>Test framework</td>	<td>Spock</td>		</tr>
<tr> <td>Logging </td>			<td>SLF4J + Logback</td>		</tr>
<tr> <td>Default database</td>	<td>H2</td>			</tr>
</tr>
</table>


Installation & Usage
--------------------

	./gradlew run 		# execute the sample job
	./gradlew check 	# run the tests and generate reports
	./gradlew distZip 	# package the project for distribution
	./runJob.groovy <jobPath> <jobId> # runs a specific job, given its path and id
