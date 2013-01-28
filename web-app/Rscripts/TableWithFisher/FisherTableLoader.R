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
#FisherTableLoader
#This will load our input files into variables so we can run the box plot with ANOVA.
###########################################################################

FisherTable.loader <- function(
  input.filename,
  output.file						="FisherTable"
  )
 {
 	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	######################################################
	
	if(length(unique(line.data$X)) < 2) stop("||FRIENDLY||The Fisher test requires at least two groups for each variable. The intersection of the groups you selected for the independent variable with the data available in the dependent variable yielded only one group with a none zero number of subjects in the independent variable. Please verify your input and try again.")
	if(length(unique(line.data$Y)) < 2) stop("||FRIENDLY||The Fisher test requires at least two groups for each variable. The intersection of the groups you selected for the dependant variable with the data available in the independent variable yielded only one group with a none zero number of subjects in the dependant variable. Please verify your input and try again.")
	
	######################################################
	if(("GROUP" %in% colnames(line.data)) && ("GROUP.1" %in% colnames(line.data)))
	{
		#This is a list of the distinct groups.
		groupList <- matrix(unique(line.data$GROUP));

		#For each of the "GROUPS" we need to call the Fisher test on each of the "GROUP.1" values.
		subFunctionForFisher <- function(currentGroup, groupedData)
		{
			#Build a list of indexes which represent the records we need to pull for each group.
			currentIndex <- which(line.data$GROUP==currentGroup)
				
			#Pull the records into another object.
			currentGroupingData <- line.data[currentIndex,]
			
			trimmedGroupName <- gsub("^\\s+|\\s+$", "",currentGroup)
			
			#Run the lm function on each grouping.
			lapply(split(currentGroupingData,currentGroupingData$GROUP.1),FisherTable.loader.single,"GROUP.1",trimmedGroupName)
		}
		
		#This calls the first function on each "GROUP" which will call another function on "GROUP.1"
		lapply(groupList,subFunctionForFisher,groupedData)	
	}
	else if("GROUP.1" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP.1), FisherTable.loader.single,"GROUP.1","")
	}
	else if("GROUP" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP), FisherTable.loader.single,"GROUP","")
	}
	else
	{
		FisherTable.loader.single(line.data,'','')
	}
	
	######################################################
}

FisherTable.loader.single <- function(dataChunk,splitColumn,fileNameQualifier)
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
	
	#Remove unwanted column.
	dataChunk <- dataChunk[c('X','Y')]
	
	#Recreate the factors to take out the levels we don't use in this group.
	dataChunk$X <- factor(dataChunk$X)
	dataChunk$Y <- factor(dataChunk$Y)
	
	#Generate count table.
	countTable <- table(dataChunk)
	
	#Get fisher test statistics.
	fisherResults <- fisher.test(countTable,simulate.p.value=TRUE)
		
	#Get chi^2 test statistics.
	chiResults <- chisq.test(countTable)	
	
	#Write the results of the tests to a file.
	write(paste("fishp=",format(fisherResults$p.value,digits=3),sep=""), file=statisticalTestsResultsFile,append=T)
	write(paste("chis=",format(chiResults$statistic,digits=3),sep=""), file=statisticalTestsResultsFile,append=T)
	write(paste("chip=",format(chiResults$p.value,digits=3),sep=""), file=statisticalTestsResultsFile,append=T)
	
	#Print count table to file.
	write.table(countTable,countsFile,quote=F,sep="\t",row.names=T,col.names=T,append=T)
}