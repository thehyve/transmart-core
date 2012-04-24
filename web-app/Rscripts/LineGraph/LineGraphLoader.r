###########################################################################
#CoxRegression
#This will load our input files into variables so we can run the cox regression.
###########################################################################

LineGraph.loader <- function(
  input.filename,
  output.file						="LineGraph"
  )
 {
 	
	#We need this package for a str_extract when we take text out of the concept.
	library(stringr)
	library(plyr)
	library(ggplot2)
	library(Cairo)
	
	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	
	#We need to convert the value column from a factor to a numeric.
	#finalData$VALUE <- as.numeric(levels(finalData$VALUE))[as.integer(finalData$VALUE)]

	#Aggregate the data to get rid of patient numbers. We add a standard error column so we can use it in the error bars.
	dataOutput <- ddply(line.data, .(CONCEPT_PATH,GROUP_VAR), 
	  summarise,
	  VALUE = mean(VALUE),
	  SE = sd(VALUE)/sqrt(length(VALUE))
	)

	#Adjust the column names.
	colnames(dataOutput) <- c('TIMEPOINT','GROUP','MEAN','SE')

	#Use a regular expression trim out the timepoint from the concept.
	dataOutput$TIMEPOINT <- str_extract(dataOutput$TIMEPOINT,"Week [0-9]+")
	
	#Convert the timepoint field to a factor.
	dataOutput$TIMEPOINT <- factor(dataOutput$TIMEPOINT)
	
	#Convert the group field to a factor.
	dataOutput$GROUP <- factor(dataOutput$GROUP)
	######################################################
	
	######################################################
	#Plotting the line.
	limits <- aes(ymax = MEAN + SE, ymin = MEAN - SE)

	#I think this starts the process of saving the plot to a file.
	#png(paste(output.file, "_SurvivalCurve.png", sep=""))
		
	CairoPNG(file=paste(output.file,".png",sep=""))	
	tmp <- ggplot(
		data=dataOutput,
		aes(x=TIMEPOINT, 
			y=MEAN,
			group=GROUP, 
			colour=GROUP
			)
		) + geom_line() + geom_errorbar(limits,width=0.2)
	print (tmp)
	#ggsave(file=paste(output.file,".png",sep=""), scale=2.5)
	#Close any open devices.
	#dev.off()	
	######################################################
}
