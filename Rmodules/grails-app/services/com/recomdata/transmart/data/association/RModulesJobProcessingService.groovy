package com.recomdata.transmart.data.association

import grails.util.Holders
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException
import org.rosuda.Rserve.*

/**
 * This class contains methods for interacting with the R environment.
 *  
*/
class RModulesJobProcessingService {
	static transactional = true
	static scope = 'request'
	
	def grailsApplication
	
	def runRScript(workingDirectory, scriptName, commandToRun, rScriptDirectory)
	{
		//Establish a connection to R Server.
		RConnection c = new RConnection(Holders.config.RModules.host, Holders.config.RModules.port);
		c.stringEncoding = 'utf8'
		
		//Set the working directory to be our temporary location.
		log.debug("Attempting following R Command : " + "setwd('${workingDirectory}')".replace("\\","\\\\"))
		println("Attempting following R Command : " + "setwd('${workingDirectory}')".replace("\\","\\\\"))
		String workingDirectoryCommand = "setwd('${workingDirectory}')".replace("\\","\\\\")
		
		//Run the R command to set the working directory to our temp directory.
		REXP x = c.eval(workingDirectoryCommand);
		
		//Run the R command to source in the script.
		def sourceCommand = "source('${rScriptDirectory}/" + scriptName + "');".replace("\\","\\\\")
		
		log.debug("Attempting following R Command : " + sourceCommand)
		println("Attempting following R Command : " + sourceCommand)
		
		x = c.eval(sourceCommand);
		
		log.debug("Attempting following R Command : " + commandToRun)
		println("Attempting following R Command : " + commandToRun)
		
		REXP r = c.parseAndEval("try("+commandToRun+",silent=TRUE)");
		
		if (r.inherits("try-error"))
		{
			//Grab the error R gave us.
			String rError = r.asString()
			
			//This is the error we will eventually throw.
			RserveException newError = null
			
			//If it is a friendly error, use that, otherwise throw the default message.
			if(rError ==~ /(?ms).*\|\|FRIENDLY\|\|.*/)
			{
				rError = rError.replaceFirst(/(?ms).*\|\|FRIENDLY\|\|/,"")
				newError = new RserveException(c,rError);
			}
			else
			{
				log.error("RserveException thrown executing job: " + rError)
				log.debug("RserveException thrown executing job: " + rError)
				newError = new RserveException(c,"There was an error running the R script for your job. Please contact an administrator.");
			}
			
			throw newError;

		}
	}
	
}
