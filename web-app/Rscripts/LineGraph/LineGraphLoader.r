
LineGraph.loader <- function(
	input.filename,
  scaling.filename = NULL,
	output.file="LineGraph",
	graphType="MERR",
    aggregate.probes = FALSE,
  HDD.data.type = NULL
) {
	library(stringr)
	library(plyr)
	library(ggplot2)
	library(Cairo)
	require(RColorBrewer)
	
	line.data<-read.delim(input.filename, header=T, stringsAsFactors = FALSE)
	if (graphType=="IND") {
	 plot.individuals = TRUE
	} else {
	 plot.individuals = FALSE
	}

  #Read the scaling data (location of each group (concept path) on X-axis)
  if (!is.null(scaling.filename)) {
    scaling.data <- read.delim(scaling.filename, header=T, stringsAsFactors = FALSE)
  } else { # if scaling file is not available, each level of group (concept path) will be plotted at the number of that level
    scaling.data <- data.frame(GROUP = unique(line.data$GROUP), VALUE = 1:length(unique(line.data$GROUP)))
  }
  # assign the X-axis position to each row
  line.data$TIME_VALUE <- sapply(line.data$GROUP,FUN = function(groupValue) { scaling.data$VALUE[which(groupValue==scaling.data$GROUP)] })
  
  # Either plot a single LineGraph (if there are no plot_group values)
  # or, for each group-value, retrieve rows for that value and plot LineGraph
  plotGroupValues <- unique(line.data$PLOT_GROUP)
  if (is.null(plotGroupValues) || is.na(plotGroupValues)) {
    imageFileName <- paste(output.file,".png",sep="")
    CairoPNG(file = imageFileName, width=1200, height=600,units = "px")
    if (nrow(line.data) == 0) {
      Plot.error.message("Dataset is empty. Cannot plot LineGraph.");
    }
    else {
      p <- LineGraph.plotter(line.data, graphType, plot.individuals, HDD.data.type)
      print(p)
      dev.off()
    }
  } else {
    fileIter <- 1
    for (plotGroup in plotGroupValues) {
      imageFileName <- paste(output.file,fileIter,".png",sep="")
      CairoPNG(file=imageFileName, width=1200, height=600,units = "px")
      if (length(which(line.data$PLOT_GROUP==plotGroup)) == 0) {
        Plot.error.message("Dataset is empty. Cannot plot LineGraph.");
      }
      else {
        groupData <- line.data[which(line.data$PLOT_GROUP==plotGroup),]
        p <- LineGraph.plotter(groupData, graphType, plot.individuals, HDD.data.type)
        p <- p + labs(title = as.character(plotGroup))
        fileIter <- fileIter + 1
        print(p)
        dev.off()
      }
    }
  }
}


LineGraph.plotter <- function(
  data.to.plot,
  graphType,
  plot.individuals,
  HDD.data.type
)
{

	if (plot.individuals) {
	  #Change column order to match internal standard and adjust the column names.
    dataOutput <- data.to.plot[,c("PATIENT_NUM","GROUP","TIME_VALUE","GROUP_VAR","VALUE")]
    colnames(dataOutput) <- c('PATIENT_NUM','TIMEPOINT','TIME_VALUE','GROUP','VALUE')
	} else {
	  #Aggregate the data to get rid of patient numbers. We add a standard error column so we can use it in the error bars.
    dataOutput <- ddply(data.to.plot, .(GROUP,TIME_VALUE,GROUP_VAR),
	                      summarise,
	                      MEAN   	= mean(VALUE),
	                      SD 		= sd(VALUE),
	                      SE 		= sd(VALUE)/sqrt(length(VALUE)),
	                      MEDIAN 	= median(VALUE)
	  )
	  #Adjust the column names.
    colnames(dataOutput) <- c('TIMEPOINT','TIME_VALUE','GROUP','MEAN','SD','SE','MEDIAN')
	}

	#Use a regular expression trim out the timepoint from the concept.
	#dataOutput$TIMEPOINT <- str_extract(dataOutput$TIMEPOINT,"Week [0-9]+")
  dataOutput$TIMEPOINT <- as.character(dataOutput$TIMEPOINT)
  TIMEPOINT_reducedConceptPath <- str_extract(dataOutput$TIMEPOINT,"(\\\\.+\\\\.+\\\\)+?$")
  validReplacements <- which(!is.na(TIMEPOINT_reducedConceptPath))
  dataOutput$TIMEPOINT[validReplacements] <- TIMEPOINT_reducedConceptPath[validReplacements]
  dataOutput$TIMEPOINT <- as.factor(dataOutput$TIMEPOINT)
	
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
    layerData <- aes(x=TIME_VALUE, y=VALUE, group=PATIENT_NUM, colour=GROUP)
	} else if (graphType=="MERR") {
	  limits <- aes(ymax = MEAN + SE, ymin = MEAN - SE)
    layerData <- aes(x=TIME_VALUE, y=MEAN, group=GROUP, colour=GROUP)
    yLabel <- paste(yLabel,"(mean + se)")
	} else if (graphType=="MSTD") {
	  limits <- aes(ymax = MEAN + SD, ymin = MEAN - SD)
    layerData <- aes(x=TIME_VALUE, y=MEAN, group=GROUP, colour=GROUP)
    yLabel <- paste(yLabel,"(mean + sd)")
	} else if (graphType=="MEDER") {
	  limits <- aes(ymax = MEDIAN + SE, ymin = MEDIAN - SE)
    layerData <- aes(x=TIME_VALUE, y=MEDIAN, group=GROUP, colour=GROUP)
    yLabel <- paste(yLabel,"(median + se)")
	}
  p <- ggplot(data=dataOutput,layerData) + ylab(yLabel)
	
	p <- p + geom_line(size=1.5)
	if (!plot.individuals) p <- p + geom_errorbar(limits,width=0.2)
  
	#Defines a continuous x-axis with proper break-locations, labels, and axis-name
    p <- p + scale_x_continuous(name = "TIMEPOINT", breaks = dataOutput$TIME_VALUE, labels = dataOutput$TIMEPOINT)
  
	#This sets the color theme of the background/grid.
	p <- p + theme_bw();
	noPoints <- nrow(dataOutput)
	noColors <- 9
    shapesToUse <- c(15:19, 1:5)
	p <- p + aes(shape = GROUP) + scale_shape_manual(values = rep_len(shapesToUse, length.out = noPoints))
	p <- p + aes(colour = GROUP) + scale_colour_manual(values = rep_len(brewer.pal(noColors, "Set1"), length.out = noPoints))
	
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
	
  p
}


Plot.error.message <- function(errorMessage) {
    # TODO: This error handling hack is a temporary permissible quick fix:
    # It deals with getting error messages through an already used medium (the plot image) to the end-user in certain relevant cases.
    # It should be replaced by a system wide re-design of consistent error handling that is currently not in place. See ticket HYVE-12.
    print(paste("Error encountered. Caught by Plot.error.message(). Details:", errorMessage))
    tmp <- frame()
    tmp2 <- mtext(errorMessage,cex=2)
    print(tmp)
    print(tmp2)
    dev.off()
}
