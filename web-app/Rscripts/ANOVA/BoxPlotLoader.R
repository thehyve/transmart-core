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
#BoxPlot
#This will load our input files into variables so we can run the box plot with ANOVA.
###########################################################################

BoxPlot.loader <- function(
  input.filename,
  output.file="BoxPlot",
  concept.dependent,
  concept.independent,
  flipimage = FALSE,
  concept.dependent.type = "",
  concept.independent.type = "",
  genes.dependent = "",
  genes.independent = "",
  binning.enabled = FALSE,
  binning.variable = "IND",
  binning.manual = FALSE,
  binning.type = '',
  binning.variabletype = ''
  )
 {
 	
	print("-------------------")
	print("BoxPlotLoader.R")
	print("LOADING BOX PLOT")
	
	library(plyr)
	library(ggplot2)
	library(Cairo)
	
	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)

	#Make sure we always use the x column as a column of factors.
	line.data$X <- as.factor(line.data$X)
	
	#Make sure the group columns are regarded as factors.
	if("GROUP" %in% colnames(line.data)) line.data$GROUP <- as.factor(line.data$GROUP)
	if("GROUP.1" %in% colnames(line.data)) line.data$GROUP.1 <- as.factor(line.data$GROUP.1)	
	
	if((binning.type == "EDP" && binning.manual == FALSE) || (binning.type == "ESB" && binning.manual == FALSE) || (binning.manual == TRUE && binning.variabletype == "Continuous"))
	{
	#We need to re-sort the factors. Grab the current levels.
	factorDataFrame <- data.frame(levels(line.data$X))

	#Add a column name to our temp data frame.
	colnames(factorDataFrame) <- c('binname')

	#Empty out a column that will hold the first number in our binning string.
	factorDataFrame$binstart <- NA

	#Add to the new column the first number from the range string.
	factorDataFrame$binstart <- as.numeric(gsub("\\s*<=\\s*X\\s*<(=*)\\s*(.*)", "",factorDataFrame$binname))

	#Order the new data frame on the first number from the range string.
	factorDataFrame <- factorDataFrame[order(factorDataFrame$binstart), ]  
	
	#Reapply the factor to get the items in the right order.
		line.data$X <- factor(line.data$X,factorDataFrame$binname,ordered = TRUE)
	}
	
	#If we are generating statistics per group, we apply the stats function after splitting the groups.
	if(("GROUP" %in% colnames(line.data)) && ("GROUP.1" %in% colnames(line.data)))
	{
		#This is a list of the distinct groups.
		groupList <- matrix(unique(line.data$GROUP));

		#For each of the "GROUPS" we need to call the linear regression on each of the "GROUP.1" values.
		subFunctionForLM <- function(currentGroup, groupedData)
		{
			#Build a list of indexes which represent the records we need to pull for each group.
			currentIndex <- which(line.data$GROUP==currentGroup)
				
			#Pull the records into another object.
			currentGroupingData <- line.data[currentIndex,]
			
			trimmedGroupName <- gsub("^\\s+|\\s+$", "",currentGroup)
			
			#Run the lm function on each grouping.
			lapply(split(currentGroupingData,currentGroupingData$GROUP.1),calculateANOVA,"GROUP.1",trimmedGroupName)
		}
		
		#This calls the first function on each "GROUP" which will call another function on "GROUP.1"
		lapply(groupList,subFunctionForLM,groupedData)
	}
	else if("GROUP.1" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP.1), calculateANOVA,"GROUP.1","")
	}	
	else if("GROUP" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP), calculateANOVA,"GROUP","")
	}
	else
	{
		calculateANOVA(line.data,"","")
	}
	######################################################
	
	######################################################
	#Plotting the box plot.		
	
	#If we have both a group and a group1 column we need to create different graphs.
	if(("GROUP" %in% colnames(line.data)) && ("GROUP.1" %in% colnames(line.data)))
	{
		splitData <- split(line.data,line.data$GROUP);
		groupList <- matrix(unique(line.data$GROUP));
		
		lapply(groupList,graphSubset,splitData,concept.independent.type,concept.independent,genes.independent,concept.dependent.type,concept.dependent,genes.dependent,output.file,flipimage,binning.enabled);	
	}
	else
	{
		graphSubset('',line.data,concept.independent.type,concept.independent,genes.independent,concept.dependent.type,concept.dependent,genes.dependent,output.file,flipimage,binning.enabled);
	}
	######################################################
	
	print("-------------------")
}

calculateANOVA <- function(splitData,splitColumn,fileNameQualifier)
{
	#This is the current group we are generating the statistics for.
	currentGroup <- unique(splitData[[splitColumn]])
	
	#If we have a qualifier we need to throw a "_" after the name of the file.
	if(fileNameQualifier != '') fileNameQualifier <- paste('_',fileNameQualifier,sep="");
	
	#The filename for the summary stats file.
	summaryFileName <- paste("ANOVA_RESULTS",fileNameQualifier,".txt",sep="")
	
	#The filename for the pairwise file.
	pairwiseFileName <- paste("ANOVA_PAIRWISE",fileNameQualifier,".txt",sep="")
	
	#We need to get the p-value for this ANOVA.
	#Run the ANOVA
	dataChunk.aov <- aov(Y~X,data=splitData)

	#Get a summary of the ANOVA
	summaryAnova <- summary(dataChunk.aov)

	#If we have a group column we should write the group name to the file.
	if(splitColumn %in% colnames(splitData)) write(paste("name=",currentGroup,sep=""), file=summaryFileName,append=T)
	
	write("||PVALUES||", file=summaryFileName,append=T)
	
	#Write the p-value to a file.
	write(paste("p=",format(summaryAnova[[1]]$'Pr(>F)'[1],digits=3),sep=""), file=summaryFileName,append=TRUE)
	write(paste("f=",format(summaryAnova[[1]]$'F value'[1],digits=3),sep=""), file=summaryFileName,append=TRUE)	
	
	#Create a table that has a mean for each group.
	aggregateTable <- aggregate(splitData$Y, list(splitData$X), mean)

	#Create a table that has a count for each group.
	countTable <- data.frame(summary(as.factor(splitData$X)))

	#The count table needs a column with the group names.
	countTable$GROUP <- row.names(countTable)

	#Create column names so we can merge the table reliably.
	colnames(aggregateTable) <- c("GROUP","MEAN")
	colnames(countTable) <- c("COUNT","GROUP")

	#Format our means to only 3 decimal places.
	aggregateTable$MEAN = format(aggregateTable$MEAN,digits=3)

	#Merge the tables so we can output the counts and means in one statement.
	finalOutputTable <- merge(aggregateTable,countTable,by="GROUP")
	
	write("||SUMMARY||", file=summaryFileName,append=T)
	
	#Write the summary table to a file.
	write.table(finalOutputTable,summaryFileName,quote=TRUE,col.names=FALSE,append=T)

	#We also need to generate the pairwise p-values matrix.
	pairwiseResults <- pairwise.t.test(splitData$Y, splitData$X, p.adj = "none")[["p.value"]]

	#Write the matrix to a table.
	if(splitColumn %in% colnames(splitData)) write(paste("name=",currentGroup,sep=""), file=pairwiseFileName,append=T)
	write.table(format(pairwiseResults,digits=3),pairwiseFileName,quote=FALSE,col.names=TRUE,sep="\t",append=T)

}

createPlotAesthetics <- function(line.data)
{
	boxPlotAES <- aes(factor(X), Y)
	
	#If we have different probes we need to fill the boxplots from the group column.
	if("GROUP.1" %in% colnames(line.data))
	{
		boxPlotAES <- aes(factor(X), Y, fill=GROUP.1)
	}
	else if("GROUP" %in% colnames(line.data)) 
	{
		boxPlotAES <- aes(factor(X), Y, fill=GROUP)
	}

	return(boxPlotAES)
	
}

createYAxisLabel <- function(concept.type,concept,genes)
{
	yAxisLabel <- ""

	if(concept.type == "CLINICAL") yAxisLabel <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept,perl=TRUE)
	if(concept.type == "MRNA") yAxisLabel <- paste(genes,"Gene Expression (zscore)",sep=" ")
	if(concept.type == "SNP") yAxisLabel <- "SNP"

	return(yAxisLabel)
}

#Create the function that actually does the graphing.
graphSubset <- function(currentGroup,
						dataToGraph,
						concept.independent.type,
						concept.independent,
						genes.independent,
						concept.dependent.type,
						concept.dependent,
						genes.dependent,
						output.file,
						flipimage,
						binning.enabled)
{
	#Get the name of the group.
	trimmedGroupName <- gsub("^\\s+|\\s+$", "",currentGroup)
	
	#If we don't have a group, graph all the data.
	if(trimmedGroupName != '') dataToGraph <- dataToGraph[[currentGroup]]
	
	#Set the aesthetics.
	boxPlotAES <- createPlotAesthetics(dataToGraph)	
	
	#If we are flipping the axis we need to add a coord_flip() to the box plot. We also conditionally set the axis parameters based on graph orientation.
	if(flipimage)
	{
		#Create the y axis label based on the concepts.
		yAxisLabel <- createYAxisLabel(concept.independent.type,concept.independent,genes.independent)
		
		xAxisLabel = ''
		
		#If we are binning the names can get kind of unruly. We display a title here so we can just use "X" in the labels.
		if(binning.enabled == TRUE)	xAxisLabel = sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept.dependent,perl=TRUE)
		
		#This is the base box plot.
		tmp <- ggplot(dataToGraph, boxPlotAES) + geom_boxplot()		
		
		#Set the Y axis label.
		tmp <- tmp + ylab(yAxisLabel)
		
		#Set the Y axis title.
		tmp <- tmp + xlab(xAxisLabel)
		
		#Set the font for the x axis title.
		tmp <- tmp + theme(axis.title.x=theme_text(size = 15,face="bold"))		
		
		#Set the fonts for the axis labels.
		tmp <- tmp + theme(axis.text.y = theme_text(size = 15,face="bold"))
		tmp <- tmp + theme(axis.text.x = theme_text(size = 10,face="bold"))
		
		#Flip the image.
		tmp <- tmp + coord_flip()
		
		#Set the font size for the legend.
		tmp <- tmp + theme(legend.text = theme_text(size = 9,hjust=0))
		
		#Get the device ready for printing and create the image file.
		CairoPNG(file=paste(output.file,"_",trimmedGroupName,".png",sep=""),width=800,height=800)
		print (tmp)
		
		#Close any open devices.
		dev.off()
	}
	else
	{
		#Create the y axis label based on the concepts.
		yAxisLabel <- createYAxisLabel(concept.dependent.type,concept.dependent,genes.dependent)
		
		xAxisLabel = ''
		
		#If we are binning the names can get kind of unruly. We display a title here so we can just use "X" in the labels.
		if(binning.enabled == TRUE) xAxisLabel = sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept.independent,perl=TRUE)
	
		#This is the base box plot.
		tmp <- ggplot(dataToGraph, boxPlotAES) + geom_boxplot()
	
		#Set the Y axis label.
		tmp <- tmp + ylab(yAxisLabel)
		
		#Set the font for the x axis title.
		tmp <- tmp + xlab(xAxisLabel)		
		
		#Set the font for the y axis title.
		tmp <- tmp + theme(axis.title.y=theme_text(size = 15,face="bold", angle=90))
		
		#Set the fonts for the axis labels.
		tmp <- tmp + theme(axis.text.x = theme_text(size = 15,face="bold",angle=15))
		tmp <- tmp + theme(axis.text.y = theme_text(size = 10,face="bold"))
		
		#Set the font size for the legend.
		tmp <- tmp + theme(legend.text = theme_text(size = 9,hjust=0))
		
		#Get the device ready for printing and create the image file.
		CairoPNG(file=paste(output.file,"_",trimmedGroupName,".png",sep=""),width=800,height=800)
		
		print (tmp)
			
		#Close any open devices.
		dev.off()			
	}
}
