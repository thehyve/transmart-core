###########################################################################
# Copyright 2008-2012 Janssen Research & Development, LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###########################################################################

###########################################################################
#ForestPlotLoader
#This will load our input files into variables so we can run the box plot with ANOVA.
###########################################################################

ForestPlot.loader <- function(
  input.filename,
  statistic = "OR",
  concept.dependent,
  concept.independent,
  concept.reference,
  output.file	="ForestPlot"
  )
 {
  ######################################################
  #We need this package for a str_extract when we take text out of the concept.
  library(stringr)
  library(plyr)
  library(ggplot2)
  library(Cairo)
  library("rmeta")
  ######################################################

 	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	######################################################
	
	if(length(unique(line.data$X)) < 2) stop("||FRIENDLY||The Forest Plot test requires at least two groups for each variable. The intersection of the groups you selected for the independent variable with the data available in the dependent variable yielded only one group with a none zero number of subjects in the independent variable. Please verify your input and try again.")
	if(length(unique(line.data$Y)) < 2) stop("||FRIENDLY||The Forest Plot requires at least two groups for each variable. The intersection of the groups you selected for the dependant variable with the data available in the independent variable yielded only one group with a none zero number of subjects in the dependant variable. Please verify your input and try again.")
	
	#Make the concept paths pretty by taking out all but the last 2 levels. If the entry doesn't have any slashes, leave it be. It could be a "bin1234" or "Other".
	
	line.data$Z <- as.character(line.data$Z)
	line.data$Z[grep("\\\\", line.data$Z)] <- str_extract(line.data$Z[grep("\\\\", line.data$Z)],"(\\\\.+\\\\.+\\\\\\s*)+?$")

	line.data$Y <- as.character(line.data$Y)
	line.data$Y[grep("\\\\", line.data$Y)] <- str_extract(line.data$Y[grep("\\\\", line.data$Y)],"(\\\\.+\\\\.+\\\\\\s*)+?$")

	line.data$X <- as.character(line.data$X)
	line.data$X[grep("\\\\", line.data$X)] <- str_extract(line.data$X[grep("\\\\", line.data$X)],"(\\\\.+\\\\.+\\\\\\s*)+?$")
	
	######################################################
	if(("GROUP" %in% colnames(line.data)) && ("GROUP.1" %in% colnames(line.data)))
	{
		#This is a list of the distinct groups.
		groupList <- matrix(unique(line.data$GROUP));

		#For each of the "GROUPS" we need to call the ForestPlot test on each of the "GROUP.1" values.
		subFunctionForForestPlot <- function(currentGroup, groupedData)
		{
			#Build a list of indexes which represent the records we need to pull for each group.
			currentIndex <- which(line.data$GROUP==currentGroup)
				
			#Pull the records into another object.
			currentGroupingData <- line.data[currentIndex,]
			
			trimmedGroupName <- gsub("^\\s+|\\s+$", "",currentGroup)
			
			#Run the lm function on each grouping.
			lapply(split(currentGroupingData,currentGroupingData$GROUP.1),ForestPlot.loader.single,"GROUP.1",trimmedGroupName)
		}
		
		#This calls the first function on each "GROUP" which will call another function on "GROUP.1"
		lapply(groupList,subFunctionForForestPlot,groupedData)	
	}
	else if("GROUP.1" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP.1), ForestPlot.loader.single,"GROUP.1","")
	}
	else if("GROUP" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP), ForestPlot.loader.single,"GROUP","")
	}
	else
	{
		ForestPlot.loader.single(
			line.data,
			'',
			'',
			output.file=output.file,
			statistic=statistic,
			concept.dependent=concept.dependent,
			concept.independent=concept.independent, 
			concept.reference = concept.reference)
	}
	
	######################################################
}

ForestPlot.loader.single <- 
function(
	dataChunk,
	splitColumn,
	fileNameQualifier,
	output.file,
	statistic=statistic,
	concept.dependent=concept.dependent,
	concept.independent=concept.independent, 
	concept.reference
	)
{
	#This is the name of the output file for the statistical tests.
	statisticalTestsResultsFile <- paste("statisticalTests",fileNameQualifier,".txt",sep="")
	
	#This is the name of the output file for the count table.
	countsFile <- paste("Count",fileNameQualifier,".txt",sep="")

	#This is the current group we are generating the statistics for.
	currentGroup <- unique(dataChunk[[splitColumn]])
	
	#If we have a group column we should write the group name to the file.
	if(splitColumn %in% colnames(dataChunk)) write(paste("name=",currentGroup,sep=""), file=statisticalTestsResultsFile,append=T)
	if(splitColumn %in% colnames(dataChunk)) write(paste("name=",currentGroup,sep=""), file=countsFile,append=T)
	
	#Remove any unwanted columns.
	# X is the dependent concept column
	# Y is the independent concept column
	# Z is the stratificaion concept column
	dataChunk <- dataChunk[c('X','REFERENCE','Y','OUTCOME','Z')]
  
	#extract the list of all distinct startification entires
	strata <- unique(dataChunk[,c('Z')])
  
	if(all(is.na(strata)))
	{
		dataChunk$Z <- 'ALL'
		strata <- c('ALL')
	}
  
	#number of stratification concepts
	numstrata <- length(strata)
	
	#create a sub matrix of dependent & independent variables
	#by seperating the dataChuck by each stratification concept 
	strata.df <- invisible(lapply(split(dataChunk, dataChunk$Z), function(x) { assign(paste0("Z", x$Z[1]), x, pos=.GlobalEnv) }))
	
	#Variable to hold the list of all the countTables,a countTable is generated for each stratification
	countTable.list		= list()
	
	#Variable to hold the counts.
	TreatmentEvents 	= list()
	ControlEvents 		= list()
	TreatmentNoEvents 	= list()
	ControlNoEvents 	= list()
	
	#row & col names
	studylabels<-getStudyLabels(paste(concept.independent,concept.reference,sep="|"))
	eventlabels<-getEventLabels(concept.dependent)

	#for each stratafication entry, create a tableCount
	for (i in strata)
	{
	
		#At the same time that we count for the Fisher test we will count for the Forest Plot. These counts are done seperately so we can keep track of the Control and "Top Event".
		TreatmentEvents[[i]] 	<- nrow(subset(strata.df[[i]], (REFERENCE == 0) & (OUTCOME == 1)))
		ControlEvents[[i]] 		<- nrow(subset(strata.df[[i]], (REFERENCE == 1) & (OUTCOME == 1)))	
		
		TreatmentNoEvents[[i]]	<- nrow(subset(strata.df[[i]], (REFERENCE == 0) & (OUTCOME == 0)))
		ControlNoEvents[[i]] 	<- nrow(subset(strata.df[[i]], (REFERENCE == 1) & (OUTCOME == 0)))		
	
		if(any(TreatmentEvents[[i]]) | any(ControlEvents[[i]]) | any(TreatmentNoEvents[[i]]) | any(ControlNoEvents[[i]]))
		{
			TreatmentEvents[[i]] <- TreatmentEvents[[i]] + .5
			ControlEvents[[i]] <- ControlEvents[[i]] + .5
			TreatmentNoEvents[[i]] <- TreatmentNoEvents[[i]] + .5
			ControlNoEvents[[i]] <- ControlNoEvents[[i]] + .5
		}
	
		#Remove unwanted column.
		data <- strata.df[[i]][c('X','Y')]

		#Recreate the factors to take out the levels we don't use in this group.
		data$X <- factor(data$X)
		data$Y <- factor(data$Y)
		
		#Generate count table.
		countTable.list[[i]] <- table(data)
		
		#We can't run the Fisher test if there aren't two groups in either the x or y columns.
		#Generate a data frame to check the groupings. Syntax is easier if it's in a data.frame first.
		tempFrame <- data.frame(countTable.list[[i]])
		
		#Check the number of unique X and Y values.
		if(length(unique(tempFrame$X)) < 2 || length(unique(tempFrame$Y)) < 2)
		{
			#Since we don't have enough groups we can't run the Fisher test.
			fisherResults <- NA
			chiResults <- NA
			
			fisherResults$p.value <- NA
			chiResults$p.value <- NA
			chiResults$statistic <- NA
		}
		else
		{

			#Get fisher test statistics.
			fisherResults <- fisher.test(countTable.list[[i]],simulate.p.value=FALSE)		
			
			#Get chi^2 test statistics.
			chiResults <- chisq.test(countTable.list[[i]]) 
		} 
		
		#The name of the header reflects the name of the function, we'll call the header Total so it appears "pretty" in the table.
		Total <- sum
		
		#Add marginal frequencies for X and Y
		countTable.list[[i]] <- addmargins(countTable.list[[i]], FUN = Total)
		
		#Start writing the data to the file. Start with the stratification name.
		write(paste("stratificationName=",i,sep=""), file=countsFile,append=T)

		#Next is the table of counts.
		write.table(countTable.list[[i]], countsFile,quote= F, sep= "\t",row.names=T,col.names=T, append=T)
		
		#Write the stats to the file.
		write(paste("fisherResults.p.Value", format(fisherResults$p.value,digits=3),NA,NA,sep="\t"), countsFile, append=T)
		write(paste("chiResults.p.value", format(chiResults$p.value,digits=3),NA,NA,sep="\t"), countsFile, append=T)
		write(paste("chiResults.statistic", format(chiResults$statistic,digits=3),NA,NA,sep="\t"), countsFile, append=T)

	}

	#Plot lists of counts 
	makeForestPlotForRCTs(
		TreatmentEvents,
		ControlEvents,
		TreatmentNoEvents,
		ControlNoEvents,
		strata,
		statistic=statistic,
		2,
		output.file=output.file,
		concept.dependent=concept.dependent,
		concept.independent=concept.independent, 
		concept.reference = concept.reference)    	
}
###############################################################################
#  The below method was adopted and modified  from the
#  http://a-little-book-of-r-for-biomedical-statistics.readthedocs.org/en/latest/_sources/src/biomedicalstats.txt
# and the rmeta pkg documentation
##############################################################################
makeForestPlotForRCTs <- function(
	TreatmentEvents,
	ControlEvents,
	TreatmentNoEvents,
	ControlNoEvents, 
	strataList,
	statistic="OR", 
	fileNameQualifier="",
	output.file,
	concept.dependent,
	concept.independent, 
	concept.reference)
{
	require("rmeta")
	library(Cairo)
  
	#If we have a qualifier we need to throw a "_" after the name of the file.
	if(fileNameQualifier != '') fileNameQualifier <- paste('_',fileNameQualifier,sep="");
 
	#This is the name of the output file for the statistical tests.
	forestPlotResultsFile <- paste("forestPlotTextTable",fileNameQualifier,".txt",sep="");
  
	numstrata <- length(strataList)
  
	ntrt.vec <- vector()  #trt_total.vec
	nctrl.vec <- vector() #ctrl_total.vec

	ptrt.vec <- vector()  #ptrt.vec
	pctrl.vec <- vector() #pctrl.vec

	#Loop through each of the strata to get the counts for the Exposed and unexposed groups.
	for (i in strataList)
	{	
		#Make an array "nctrl.vec" of the Number of subjects in control group, in each stratum
		nctrl.vec[i] <- ControlEvents[[i]] + ControlNoEvents[[i]]
		
		#Make an array "ntrt.vec" of the Number of subjects in treated/exposed group, in each stratum
		ntrt.vec[i] <- TreatmentEvents[[i]] + TreatmentNoEvents[[i]]
		
		#Make an array "pctrl.vec" of the Number of events in control group, in each stratum
		pctrl.vec[i] <- ControlEvents[[i]]
		
		#Make an array "ptrt.vec" of the Number of events in treated/exposed group, in each stratum
		ptrt.vec[i] <- TreatmentEvents[[i]]

	}

	#Do a text replace on the strata name if applicable.
	if(length(strataList) ==1  && is.na(match("NA", strataList))) strataList=c("All")
	
	#Build a (Mantel-Haenszel) meta-analysis.
	myMH <- meta.MH(ntrt.vec, nctrl.vec, ptrt.vec, pctrl.vec, conf.level=0.95, names=strataList,statistic=statistic)
 
	#Build vectors to hold statistics.
	mean <-vector()
	lower<-vector()
	upper<-vector()

	#Point estimates from studies
	logRatio <- NA
	
	#Standard errors of Point estimates
	selogRatio <- NA
	
	#Precision: box ares is proportional to this. 1/se^2 is the default
	
	#Run the selected statistical test.
	if(statistic =="OR")
	{
		#This is the label that tells us what type of test was run.
		stat_label <- "Odds Ratio"
			
		mean<- c(myMH$logOR)

		mean[is.infinite(mean)] <- NA
		
		lower<- mean-c(myMH$selogOR)*2
		upper<- mean+c(myMH$selogOR)*2
		
		logRatio <- myMH$logOR
		selogRatio <- myMH$selogOR
		
	} 
	else 
	{
		stat_label<-"Relative Risk"
		
		mean<- c(myMH$logRR)
		
		mean[is.infinite(mean)] <- NA
		
		lower<- mean-c(myMH$selogRR)*2
		upper<- mean+c(myMH$selogRR)*2
		
		logRatio <- myMH$logRR
		selogRatio <- myMH$selogRR
	}
	
	#If the odds ratio/relative risk cannot be calculated, replace the Inf with NA.
	upper[is.infinite(upper)] <- NA
	lower[is.infinite(lower)] <- NA
	
	logRatio[is.infinite(logRatio)] <- NA
	selogRatio[is.infinite(selogRatio)] <- NA
	

	stratNames <- as.character(strataList)
	stratLabel <- "Stratification"

	tabletext<-cbind(c(stratLabel,stratNames,"Summary"),
				   c(stat_label,format(exp(mean),digits=3),format(exp(myMH$logMH),digits=3)),
				   c("Est. ( 95% CI )",paste("(",format(exp(lower),digits=3),"-",format(exp(upper),digits=3),")",sep=" "),""))

	write.table(tabletext, "statisticByStratificationTable.txt", row.names=F,col.names=F, sep="\t", quote= F)
				   
	#This is the name of the output image file.
	imageFileName <- paste(output.file,".png",sep="")

	#This initializes our image capture object.
	CairoPNG(file=imageFileName,width=800,height=600)
	
	#Increase the font size of the plot a small amount.
	par(cex=1.3)

	if(all(is.na(logRatio)) | all(is.na(selogRatio)))
	{
		stop("||FRIENDLY||The Forest Plot failed to calculate ratios for all of your stratifications, The plot cannot be created.")
	}
	
	myMH$names[grep("\\\\", myMH$names)] <- str_extract(myMH$names[grep("\\\\", myMH$names)],"(\\\\.+\\\\\\s*)+?$")
	
	#Plot the forest plot.
	metaplot(	mn = logRatio, 
				se = selogRatio, 
				labels = myMH$names,
				summn = myMH$logMH, 
				sumse = myMH$selogMH, 
				sumnn = myMH$selogMH^-2,
				ylab = "Stratification Variable",
				xlab = stat_label,
				logeffect = TRUE)

	dev.off()
  
}

getStudyLabels <-function(concept.independent){
  ######################################################
  #Label the table
  
  splitConcept <- strsplit(concept.independent,"\\|");
  entry <- unlist(splitConcept);
  
  #Make the column name pretty.
  entry <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=entry,perl=TRUE)
  entry <- gsub("^\\s+|\\s+$", "",entry)
  entry <- gsub("^\\\\|\\\\$", "",entry)
  studylabels <- gsub("\\\\", "-",entry)

  return (studylabels)
  
}

getEventLabels<-function(concept.dependent){
  splitConcept <- strsplit(concept.dependent,"\\|");
  entry <- unlist(splitConcept);
  
  #Make the column name pretty.
  entry <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=entry,perl=TRUE)
  entry <- gsub("^\\s+|\\s+$", "",entry)
  entry <- gsub("^\\\\|\\\\$", "",entry)
  eventlabels <- gsub("\\\\", "-",entry)

  return (eventlabels)
}
getFixedLengthLabel<-function(label,length,brackets=F){
  #getlast occurance of "-"
    loc = sapply(gregexpr("\\-", label), tail, 1)
    if(loc!= -1){
      label<-str_sub(label,loc+1)#add one
    }  
    
    if(str_length(label) > length){
      label<-str_sub(label,1,length)
      label<-str_c(label,"...")
    } else{
      label<-str_sub(label,1,length)
    }
    if(brackets){
      label<-str_c("(",label,")")
    }
  return (str_pad(label, length+4, "both"))
}                                                                            
