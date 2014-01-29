###########################################################################
#CoxRegression
#This will load our input files into variables so we can run the cox regression.
###########################################################################

LineGraph.loader <- function(
	input.filename,
	output.file="LineGraph",
	graphType="MERR",
    aggregate.probes = FALSE
	plot.individuals=FALSE,
	HDD.data.type = NULL
)
{
 	######################################################
	#We need this package for a str_extract when we take text out of the concept.
	library(stringr)
	library(plyr)
	library(ggplot2)
	library(Cairo)
	######################################################
	
	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	
	#We need to convert the value column from a factor to a numeric.
	#finalData$VALUE <- as.numeric(levels(finalData$VALUE))[as.integer(finalData$VALUE)]

	if (plot.individuals) {
		print("PLOT INDIVIDUALS")
	  #Change column order to match internal standard and adjust the column names.
	  dataOutput <- line.data[,c("PATIENT_NUM","GROUP","GROUP_VAR","VALUE")]
	  colnames(dataOutput) <- c('PATIENT_NUM','TIMEPOINT','GROUP','VALUE')
	} else {
		print("PLOT NON INDIVIDUALS")
	  #Aggregate the data to get rid of patient numbers. We add a standard error column so we can use it in the error bars.
	  dataOutput <- ddply(line.data, .(GROUP,GROUP_VAR),
	                      summarise,
	                      MEAN   	= mean(VALUE),
	                      SD 		= sd(VALUE),
	                      SE 		= sd(VALUE)/sqrt(length(VALUE)),
	                      MEDIAN 	= median(VALUE)
	  )
	  #Adjust the column names.
	  colnames(dataOutput) <- c('TIMEPOINT','GROUP','MEAN','SD','SE','MEDIAN')
	}

	#Use a regular expression trim out the timepoint from the concept.
	#dataOutput$TIMEPOINT <- str_extract(dataOutput$TIMEPOINT,"Week [0-9]+")
	TIMEPOINT_reducedConceptPath <- str_extract(dataOutput$TIMEPOINT,"(\\\\.+\\\\.+\\\\)+?$")
    validReplacements <- which(!is.na(TIMEPOINT_reducedConceptPath))
    dataOutput$TIMEPOINT[validReplacements] <- TIMEPOINT_reducedConceptPath[validReplacements]
	
	#Convert the timepoint field to a factor.
	dataOutput$TIMEPOINT <- factor(dataOutput$TIMEPOINT)
		
	#Convert the group field to a factor.
	dataOutput$GROUP <- factor(dataOutput$GROUP)
	######################################################

	######################################################
	#Plotting the line.

	#Determine Y-axis label.
    if (is.null(HDD.data.type)) yLabel <- dataOutput$TIMEPOINT[1] else yLabel <- HDD.data.type

	#Depending on whether we wish to plot individual data, and otherwise the specific graph type, we create different graphs.
	if (plot.individuals) {
	  limits <- aes(ymax = VALUE, ymin = VALUE);
	  layerData <- aes(x=TIMEPOINT, y=VALUE, group=PATIENT_NUM, colour=GROUP)
	} else if (graphType=="MERR") {
	  limits <- aes(ymax = MEAN + SE, ymin = MEAN - SE)
	  layerData <- aes(x=TIMEPOINT, y=MEAN, group=GROUP, colour=GROUP)
	  yLabel <- paste(yLabel,"(mean + se)")
	} else if (graphType=="MSTD") {
	  limits <- aes(ymax = MEAN + SD, ymin = MEAN - SD)
	  layerData <- aes(x=TIMEPOINT, y=MEAN, group=GROUP, colour=GROUP)
	  yLabel <- paste(yLabel,"(mean + sd)")
	} else if (graphType=="MEDER") {
	  limits <- aes(ymax = MEDIAN + SE, ymin = MEDIAN - SE)
	  layerData <- aes(x=TIMEPOINT, y=MEDIAN, group=GROUP, colour=GROUP)
	  yLabel <- paste(yLabel,"(median + se)")
	}
	p <- ggplot(data=dataOutput,layerData) + ylab(yLabel)
	
	p <- p + geom_line(size=1.5) + scale_colour_brewer() 
	if (!plot.individuals) p <- p + geom_errorbar(limits,width=0.2)
  
	#Defines a continuous x-axis with proper break-locations, labels, and axis-name
	#p <- p + scale_x_continuous(name = "TIMEPOINT", breaks = dataOutput$TIME_VALUE, labels = dataOutput$TIMEPOINT)
  
	#This sets the color theme of the background/grid.
	p <- p + theme_bw();
	
	#Set the text options for the axis.
	p <- p + theme(axis.text.x = theme_text(size = 17,face="bold",angle=5));
	p <- p + theme(axis.text.y = theme_text(size = 17,face="bold"));
	
	#Set the text options for the title.
	p <- p + theme(axis.title.x = theme_text(vjust = -.5,size = 20,face="bold"));
	p <- p + theme(axis.title.y = theme_text(vjust = .35,size = 20,face="bold",angle=90));
	
	#Set the legend attributes.
	p <- p + theme(legend.title = theme_text(size = 20,face="bold"));
	p <- p + theme(legend.text = theme_text(size = 15,face="bold"));
	p <- p + theme(legend.title=theme_blank())

	p <- p + geom_point(size=4);
	
	#This is the name of the output image file.
	imageFileName <- paste(output.file,".png",sep="")
	
	#This initializes our image capture object.
	CairoPNG(file=imageFileName, width=1200, height=600,units = "px")	
	
	#Printing actually puts the plot in the image.
	print(p)
	
	#Turn of the graphics device to save the image.
	dev.off()
	######################################################
}
