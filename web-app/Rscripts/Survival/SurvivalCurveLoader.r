###########################################################################
#SurvivalCurveLoader
#This will load our input files into variables so we can run the survival curve.
###########################################################################

SurvivalCurve.loader <- function(
	input.filename,
	output.name="SurvivalCurve",
	time.field="TIME",
	censor.field="CENSOR",
	concept.time=NA,
	time.conversion=1,
	binning.type = '',
	binning.manual = FALSE,
	binning.variabletype = ''
  )
 {
 	print("-------------------")
	print("SurvivalCurveLoader.r")
	print("LOADING SURVIVAL CURVE")
 
	######################################################
	#Clean parameters.
	#Convert the time conversion parameter to a number.
	time.conversion<-as.numeric(time.conversion)
	######################################################
	
	######################################################
	#Input files/Variables
	surv.data<-read.delim(input.filename,header=T)
	
	#If we have a group column, make sure it's regarded as a factor.
	if("GROUP" %in% colnames(surv.data)) 
	{
		surv.data$GROUP <- as.factor(surv.data$GROUP)
		lapply(split(surv.data, surv.data$GROUP), SurvivalCurve.loader.individual,output.name,time.field,censor.field,time.conversion,concept.time,binning.type,binning.manual,binning.variabletype)
	}
	else
	{
		SurvivalCurve.loader.individual(surv.data,output.name,time.field,censor.field,time.conversion,concept.time,binning.type,binning.manual,binning.variabletype)
	}
	######################################################
	
	print("-------------------")
}

SurvivalCurve.loader.individual <- function(dataChunk,output.name,time.field,censor.field,time.conversion,concept.time,binning.type,binning.manual,binning.variabletype)
{

	currentDataSubset <- data.frame(dataChunk)
	
	currentDataSubset$CATEGORY <- factor(currentDataSubset$CATEGORY)

	######################################################
	#If we did a binning routine, we need to make sure the levels are in order here.
	if((binning.type == "EDP" && binning.manual == FALSE) || (binning.type == "ESB" && binning.manual == FALSE) || (binning.manual == TRUE && binning.variabletype == "Continuous"))
	{
		#We need to re-sort the factors. Grab the current levels.
		factorDataFrame <- data.frame(levels(currentDataSubset$CATEGORY))

		#Add a column name to our temp data frame.
		colnames(factorDataFrame) <- c('binname')

		#Empty out a column that will hold the first number in our binning string.
		factorDataFrame$binstart <- NA

		#Add to the new column the first number from the range string.
		factorDataFrame$binstart <- as.numeric(gsub("\\s*<=\\s*CATEGORY\\s*<(=*)\\s*(.*)", "",factorDataFrame$binname))
		
		#Order the new data frame on the first number from the range string.
		factorDataFrame <- factorDataFrame[order(factorDataFrame$binstart), ]  
		
		#Reapply the factor to get the items in the right order.
		currentDataSubset$CATEGORY <- factor(currentDataSubset$CATEGORY,factorDataFrame$binname,ordered = TRUE)

	}
	
	######################################################
	
	######################################################
	#Determine title for outputs.
	title<-"#  Kaplan-Meier estimator"
	######################################################	
	
	######################################################
	#We need to create a label for the legend based on the categories.
	classList <- as.vector(gsub("\\s","_",gsub("^\\s+|\\s+$", "",(currentDataSubset$'CATEGORY'))))
	legendLabels <- as.vector(unique(gsub("\\s","_",gsub("^\\s+|\\s+$", "",currentDataSubset$'CATEGORY'))))

	#Pull the time and status fields out of the survival data.
	time <- currentDataSubset[[time.field]]
	
	#If the time field is not numeric try to strip a comma (Possible cause for it being read as a character) and cast it as numeric.
	if(!is.numeric(time))
	{
		time <- gsub(",","",time)
		time <- as.numeric(time)
	}
	
	status <- currentDataSubset[[censor.field]]
	
	#This is the current group we are generating the statistics for.
	if("GROUP" %in% colnames(currentDataSubset)) 
	{
		currentGroup <- unique(currentDataSubset$GROUP)
		currentGroup <- gsub("^\\s+|\\s+$", "",currentGroup)
		
		#Change the output file name to have the group in it.
		output.name <- paste(output.name,'_',currentGroup,sep='')
		
		#Include the group name in the plot.
		title <- paste("#  Kaplan-Meier estimator (",currentGroup,")",sep="")
	}
	
	######################################################
	
	######################################################
	#Load package
	require(splines,quietly=T)
	require(survival,quietly=T)
	library(Cairo)
	######################################################

	######################################################
	#Run survival curve, and survival fit.
	#If we have less than 2 classes, we need special syntax for the Survival formula.
	if(length(legendLabels)<2)
	{
		fitted = survfit(
							Surv(time,status)~1,
							data=currentDataSubset,
							type="kaplan-meier",
							error="greenwood",
							conf.int=.95,
							conf.type="log"
						)
	}
	else
	{
		fitted = survfit(
							Surv(time,status)~classList,
							data=currentDataSubset,
							type="kaplan-meier",
							error="greenwood",
							conf.int=.95,
							conf.type="log"
						)
	}	
	######################################################

	######################################################
	#Print the summary results.

	options(width=10000)
	fit.name <- paste(output.name,"_FitSummary.txt",sep="")
	table.name <- paste(output.name,"_Table.txt",sep="")

	write.table(title,fit.name,quote=F,sep="\t",row.names=F,col.names=F)
	write.table(title,table.name,quote=F,sep="\t",row.names=F,col.names=F)
	write.table("",fit.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	write.table("",table.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)

	write.table(capture.output(fitted),fit.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	
	write.table(capture.output(summary(fitted)),table.name,quote=F,sep="\t",row.names=F,col.names=F,append=T)
	options(width=80)
	######################################################

	######################################################
	#Plotting survival curve
	#X Axis range
	x.max<-max(time)/time.conversion

	#This is the default X axis label.
	x.axis.label="Time"
	
	#The x axis can be a trimmed version of the concept path.
	if(!is.na(concept.time)) x.axis.label <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept.time,perl=TRUE)
	
	y.axis.label="Fraction of Patients"

	#Set the limits for the axis.
	xlim <- c(0, x.max)
	ylim <- c(0,1)

	#This initializes our image capture object.
	CairoPNG(file=paste(output.name,".png",sep=""), width=800, height=800,units = "px")	
	
	#Plot the graph.
	suppressWarnings(plot(
							fitted, 
							lty=1, 
							col=c("blue","red","black","green","orange","purple"), 
							lwd=1, 
							firstx=0, 
							mark.time=TRUE, 
							mark=3, 
							xscale=time.conversion, 
							xlim=xlim, 
							ylim=ylim, 
							xlab=x.axis.label, 
							ylab=y.axis.label, 
							fun="log", 
							main=title, 
							cex.axis=1
							))
							
	#Draw a box around the plot.
	box(lwd=1)
	
	#Add legend if required.
	legend(x="topright", legend=levels(currentDataSubset$'CATEGORY'), lty=1, col=c("blue","red","black","green","orange","purple"), lwd=1, inset=0.02)

	#Close any open devices.
	dev.off()
	######################################################
}
