###########################################################################
#CoxRegression
#This will load our input files into variables so we can run the cox regression.
###########################################################################

CoxRegression.loader <- function(
  input.filename,
  output.file						="CoxRegression_result",
  time								="TIME",
  status							="CENSOR",
  variable.continuous				="CATEGORY",
  variable.category					="NA",
  variable.interaction.terms		="NA",
  strata							="NA",
  input.subgroup					="NA", 		
  variable.selection				="none"    	
  )
 {
  	print("-------------------")
	print("CoxRegressionLoader.r")
	print("DOING COX REGRESSION")
	
	######################################################
	#Load packages for doing the regression.
	library(survival)
	######################################################	
	
	######################################################
	#Read the survival data.
	surv.data<-read.delim(input.filename,header=T)
	
	#If we have a group column, make sure it's regarded as a factor.
	if("GROUP" %in% colnames(surv.data)) 
	{
		surv.data$GROUP <- as.factor(surv.data$GROUP)
		lapply(split(surv.data, surv.data$GROUP), CoxRegression.loader.individual,output.file,variable.selection)
	}
	else
	{
		CoxRegression.loader.individual(surv.data,output.file,variable.selection)
	}
	######################################################
	print("-------------------")
}

CoxRegression.loader.individual <- function(dataChunk,output.file,variable.selection)
{
	#Modify output file name if there is a group column.
	if("GROUP" %in% colnames(dataChunk)) 
	{
		currentGroup <- unique(dataChunk$GROUP)
		currentGroup <- gsub("^\\s+|\\s+$", "",currentGroup)
		
		#Change the output file name to have the group in it.
		output.file <- paste(output.file,'_',currentGroup,sep='')
	}

	######################################################
	#Hardcode some variables.
	tie.data.handling="efron"
	robust.variance="F"
	######################################################

	######################################################
	#Clean up the variables, Removing leading/trailing spaces and replacing other spaces with an underscore.
	classList <- as.vector(gsub("\\s","_",gsub("^\\s+|\\s+$", "",(dataChunk$'CATEGORY'))))
	######################################################
	
	######################################################
	#Running the actual Cox analysis.

	#Only include Tilde if we had multiple groups.
	if(length(unique(classList)) < 2)
	{
		return()
		#coxph.fit<-coxph(Surv(TIME,CENSOR)~1,data=surv.data,method="efron",robust="F")
	}
	else
	{
		coxph.fit<-coxph(Surv(TIME,CENSOR)~classList,data=dataChunk,method="efron",robust="F")
	}

	fit.vector<-unlist(coxph.fit)  
	######################################################
	
	######################################################
	#If we need to further analyze the data based on variable selection, do it here.
	if (variable.selection!="none")
	{
		if (robust.variance=="T")
		{
			stop("### Robust variance cannot be used in variable selection! ###")
		}
		
		coxph.fit<-stepAIC(coxph.fit,direction=variable.selection,trace=0)
		fit.vector<-unlist(coxph.fit)
		
		# null model?
		if (names(fit.vector[1])=="loglik")
		{
			stop("### Null model! No variable is selected! ###")
		}  # end null model?
	}
	######################################################
	
	######################################################
	#Number of plots
	num.plot<-0
	
	for (n in 1:length(fit.vector))
	{
		if (regexpr("^coefficients.",names(fit.vector[n]))!=-1)
		{
			num.plot<-num.plot+1
		}
	}

	if (num.plot==1)
	{
		num.row.col.plot<-1
	}else{
		num.row.col.plot<-as.integer(sqrt(num.plot)+1)
	}
	######################################################
	
	######################################################
	# Text output
	
	#Set the output file name.
	output.file <- paste(output.file,".txt",sep="")
	options(width=10000)
	#Determine how we display the tie method.
	if (tie.data.handling=="efron"){method<-"tie data handling:  Efron approximation"}
	if (tie.data.handling=="breslow"){method<-"tie data handling:  Breslow approximation"}
	if (tie.data.handling=="exact"){method<-"tie data handling:  Exact method"}

	#Determine how to display the selection.
	if (variable.selection=="none"){selection<-"variable selection:  none"}
	if (variable.selection=="both"){selection<-"variable selection:  Stepwise selection"}
	if (variable.selection=="forward"){selection<-"variable selection:  Forward selection"}
	if (variable.selection=="backward"){selection<-"variable selection:  Backward selection"}

	#Write the tables with the analysis data.
	write.table("# Cox regression results",output.file,quote=F,sep="\t",row.names=F,col.names=F)
	write.table("",output.file,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	write.table(method,output.file,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	write.table(selection,output.file,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	write.table("",output.file,quote=F,sep="\t",row.names=F,col.names=F,append=T)

	write.table(capture.output(summary(coxph.fit)),output.file,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	options(width=80)
	######################################################
}
