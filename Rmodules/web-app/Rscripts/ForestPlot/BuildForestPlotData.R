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
#BuildForestPlotData
#Parse the i2b2 output file and create input files for the Forest Table analysis.
###########################################################################

ForestPlotData.build <- 
function
(
input.dataFile,
output.dataFile="outputfile",
concept.dependent,
concept.independent,
concept.stratification,
concept.reference,
nonEventCheckbox = FALSE,
binning.Dep.enabled=FALSE,
binning.Indep.enabled=FALSE,
binning.Stratification.enabled=FALSE,
binning.Reference.enabled=FALSE,
binning.Dep.bins='',
binning.Indep.bins='',
binning.Stratification.bins='',
binning.Reference.bins='',
binning.Dep.type='',
binning.Indep.type='',
binning.Stratification.type='',
binning.Reference.type='',
binning.Dep.manual=FALSE,
binning.Indep.manual=FALSE,
binning.Stratification.manual=FALSE,
binning.Reference.manual=FALSE,
binning.Dep.binrangestring='',
binning.Indep.binrangestring='',
binning.Stratification.binrangestring='',
binning.Reference.binrangestring='',
binning.Dep.variabletype='',
binning.Indep.variabletype='',
binning.Stratification.variabletype='',
binning.Reference.variabletype='',
input.gexFile = '',
input.snpFile = '',
concept.dependent.type = "CLINICAL",
concept.independent.type = "CLINICAL",
concept.reference.type = "CLINICAL",
concept.stratification.type = "CLINICAL",
genes.dependent = '',
genes.dependent.aggregate = FALSE,
genes.independent = '',
genes.independent.aggregate = FALSE,
genes.Stratification = '',
genes.Stratification.aggregate = FALSE,
genes.Reference = '',
genes.Reference.aggregate = FALSE,
sample.dependent = '',
sample.independent = '',
sample.Stratification = '',
sample.Reference = '',
time.dependent = '',
time.independent = '',
time.Stratification = '',
time.Reference = '',
tissues.dependent = '',
tissues.independent = '',
tissues.Stratification = '',
tissues.Reference = '',
snptype.dependent = '',
snptype.independent = '',
snptype.Stratification = '',
snptype.Reference = ''
)
{
	print("-------------------")
	print("BuildForestData.R")
	print("BUILDING FOREST DATA")
	library(plyr)
	#Read the input file.
	dataFile <- data.frame(read.delim(input.dataFile));
	
	#Set the column names.
	colnames(dataFile) <- defaultColumnList()
	
	#Split the data by the CONCEPT_CD.
	splitData <- split(dataFile,dataFile$CONCEPT_PATH);
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(dataFile$PATIENT_NUM));
	
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")
	
	##########################################
	#Since we always want the full path in a column, whether it's continuous or categorical, we figure that out here.
	#There can't be a continuous variable without binning passed in.
	#If it's continuous we want !fullConcept && conceptColumn
	#If it's categorical we want fullConcept && conceptColumn
	#Concept column is always true, full concept depends if we are binning and it's continous.

	fullconcept.indep <- TRUE
	fullconcept.dep <- TRUE
	fullconcept.stratification <- TRUE
	fullconcept.reference <- TRUE
	
	if(binning.Dep.enabled == TRUE & binning.Dep.variabletype == "Continuous") fullconcept.dep <- FALSE
	if(binning.Indep.enabled == TRUE & binning.Indep.variabletype == "Continuous") fullconcept.indep <- FALSE
	if(binning.Stratification.enabled == TRUE & binning.Stratification.variabletype == "Continuous") fullconcept.stratification <- FALSE
	if(binning.Reference.enabled == TRUE & binning.Reference.variabletype == "Continuous") fullconcept.reference <- FALSE

	yValueMatrix <- dataBuilder( 	splitData = splitData,
								concept = concept.dependent,
								concept.type = concept.dependent.type,
								sampleType = sample.dependent,
								timepointType = time.dependent,
								tissueType = tissues.dependent,
								GEXFile = input.gexFile,
								gene.list = genes.dependent,
								gene.aggregate = genes.dependent.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype.dependent,
								fullConcept = fullconcept.dep,
								conceptColumn = TRUE,
								encounterColumn = TRUE);
				
	xValueMatrix <- dataBuilder(	splitData = splitData,
								concept = concept.independent,
								concept.type = concept.independent.type,
								sampleType = sample.independent,
								timepointType = time.independent,
								tissueType = tissues.independent,
								GEXFile = input.gexFile,
								gene.list = genes.independent,
								gene.aggregate = genes.independent.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype.independent,
								fullConcept = fullconcept.indep,
								conceptColumn = TRUE,
								encounterColumn = TRUE);

	referenceValueMatrix <- dataBuilder(	splitData = splitData,
								concept = concept.reference,
								concept.type = concept.reference.type,
								sampleType = sample.Reference,
								timepointType = time.Reference,
								tissueType = tissues.Reference,
								GEXFile = input.gexFile,
								gene.list = genes.Reference,
								gene.aggregate = genes.Reference.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype.Reference,
								fullConcept = fullconcept.reference,
								conceptColumn = TRUE,
								encounterColumn = TRUE);								

	zValueMatrix <- matrix(numeric(0), 0,0) 
	
  if(concept.stratification != "")
  {
	zValueMatrix <- dataBuilder( 	splitData = splitData,
	                              concept = concept.stratification,
	                              concept.type = concept.stratification.type,
	                              sampleType = sample.Stratification,
	                              timepointType = time.Stratification,
	                              tissueType = tissues.Stratification,
	                              GEXFile = input.gexFile,
	                              gene.list = genes.Stratification,
	                              gene.aggregate = genes.Stratification.aggregate,
	                              SNPFile = input.snpFile,
	                              SNPType = snptype.Stratification,
	                              fullConcept = fullconcept.stratification,
								  conceptColumn = TRUE,
								  encounterColumn = TRUE);
  }
	##########################################	
	
	##########################################
	
	#If any column has more than 1 LINK_TYPE, throw an exception.
	if(length(unique(yValueMatrix$LINK_TYPE)) > 1) stop("||FRIENDLY||There are multiple levels of concepts in the dependent input box. Please check your criteria and try again.")
	if(length(zValueMatrix)>0  && length(unique(zValueMatrix$LINK_TYPE)) > 1) stop("||FRIENDLY||There are multiple levels of concepts in the stratification input box. Please check your criteria and try again.")

	#Final Column Names
	initialFinalColumnNames <- c('PATIENT_NUM')
	
	
	#These are the columns we pull from the temp matrix.
	xMatrixColumns 			<- c('PATIENT_NUM','VALUE','CONCEPT_PATH')
	referenceMatrixColumns 	<- c('PATIENT_NUM','VALUE','CONCEPT_PATH')
	yMatrixColumns 			<- c('PATIENT_NUM','VALUE','CONCEPT_PATH')
	zMatrixColumns 			<- c('PATIENT_NUM','VALUE','CONCEPT_PATH')
	
	#These are the columns we'll merge on. The level of the concept dictates what we merge.
	mergeColumns <- c('PATIENT_NUM')

	#This was causing problems with our code merge. These columns aren't implemented in tranSMART App yet.
	#if(TRUE && unique(yValueMatrix$LINK_TYPE) == 'E' && length(zValueMatrix)>0 && unique(zValueMatrix$LINK_TYPE) == 'E')
	#{
	#xMatrixColumns 			<- c('PATIENT_NUM','VALUE','CONCEPT_PATH')
	#referenceMatrixColumns 	<- c('PATIENT_NUM','VALUE','CONCEPT_PATH')
	#yMatrixColumns 			<- c('PATIENT_NUM','ENCOUNTER_NUM','VALUE','CONCEPT_PATH')
	#zMatrixColumns 			<- c('PATIENT_NUM','ENCOUNTER_NUM','VALUE','CONCEPT_PATH')
	
  #mergeColumns <- c('PATIENT_NUM','ENCOUNTER_NUM')
		
	#initialFinalColumnNames <- c('PATIENT_NUM','ENCOUNTER_NUM')
	#}

	#Create a matrix with unique patient_nums.
	finalData <- data.frame(unique(dataFile[initialFinalColumnNames]));

	#Name the column.
	colnames(finalData) <- initialFinalColumnNames
	
	#We reset the column names after merging.
	xFinalColumns <- c('X','CONCEPT_PATH.x')
	referenceFinalColumns <- c('REFERENCE','CONCEPT_PATH.reference')
	yFinalColumns <- c('Y','OUTCOME')
	zFinalColumns <- c('Z','CONCEPT_PATH.z')
	
	#Merge our X and patient nums. For this and the reference frame we include all the patients in both joins. We'll combine the columns later.
	#finalData<-merge(finalData,xValueMatrix[xMatrixColumns],by=mergeColumns,  all.x = TRUE)
	finalData<-join(finalData,xValueMatrix[xMatrixColumns], type="left")
	colnames(finalData) <- c(initialFinalColumnNames,xFinalColumns)

	#Merge our reference frame.
	#finalData<-merge(finalData,referenceValueMatrix[referenceMatrixColumns],  all.x = TRUE)
	finalData<-join(finalData,referenceValueMatrix[referenceMatrixColumns], type="left")
	colnames(finalData) <- c(initialFinalColumnNames,xFinalColumns, referenceFinalColumns)
	
	#Merge our Y and final data.
	#finalData<-merge(finalData,yValueMatrix[yMatrixColumns], by=mergeColumns,  all.x = TRUE)
	finalData<-join(finalData,yValueMatrix[yMatrixColumns], type="inner")
	colnames(finalData) <- c(initialFinalColumnNames,xFinalColumns, referenceFinalColumns, yFinalColumns)

	if(length(zValueMatrix)>0)	{
		#Merge our Z and final data.
		#finalData<-merge(finalData,zValueMatrix[zMatrixColumns],by=zMergeColumns,all.x = TRUE)
		finalData<-join(finalData,zValueMatrix[zMatrixColumns],by=mergeColumns, type="inner")
	}	else	{
		finalData$Z <- NA
		finalData$CONCEPT_PATH.z <- NA
	}
	
	colnames(finalData) <- c(initialFinalColumnNames, xFinalColumns, referenceFinalColumns, yFinalColumns, zFinalColumns)

  
	#We need to call the binning function if it was enabled.
	if(binning.Dep.enabled)
	{
		finalData = BinningFunction(finalData,'Y',binning.Dep.bins,binning.Dep.type,binning.Dep.manual,binning.Dep.binrangestring,binning.Dep.variabletype,concept.dependent, binning.removeNAs = FALSE, binning.variableName = 'Dependent')
	}

	#We need to call the binning function if it was enabled.
	if(binning.Indep.enabled)
	{
		finalData = BinningFunction(finalData,'X',binning.Indep.bins,binning.Indep.type,binning.Indep.manual,binning.Indep.binrangestring,binning.Indep.variabletype,concept.independent, binning.removeNAs = FALSE, binning.variableName = 'Independent')
	}	
	
  	#We need to call the binning function if it was enabled.
	if(binning.Stratification.enabled == "TRUE" & concept.stratification != "")
	{
		finalData = BinningFunction(finalData,'Z',binning.Stratification.bins,binning.Stratification.type,binning.Stratification.manual,binning.Stratification.binrangestring,binning.Stratification.variabletype,concept.stratification, binning.removeNAs = FALSE, binning.variableName = 'Stratification')
	}
  
    #We need to call the binning function if it was enabled.
	if(binning.Reference.enabled)
	{
		finalData = BinningFunction(finalData,'REFERENCE',binning.Reference.bins,binning.Reference.type,binning.Reference.manual,binning.Reference.binrangestring,binning.Reference.variabletype,concept.reference, binning.removeNAs = FALSE, binning.variableName = 'Reference')
	}

	#Convert the X/Reference column to a character so we don't have to fight factor levels.
	finalData$X <- as.character(finalData$X)
	finalData$REFERENCE <- as.character(finalData$REFERENCE)	
	
	#If we found data where there are patients in both the control and reference group, stop execution here.
	validationDataFrame <- finalData[!is.na(finalData$X) & !is.na(finalData$REFERENCE),]
	if(nrow(validationDataFrame)>0) stop("||FRIENDLY||There were patients in both the control and reference groups. Please check your input parameters.")	
	
	#Make the NA's "" so we can paste into the column later.
	finalData$X[is.na(finalData$X)]   					<- ""
	finalData$REFERENCE[is.na(finalData$REFERENCE)]   	<- ""

	#Merge the X and Reference Columns.
	finalData$X <- paste(finalData$X,finalData$REFERENCE, sep="")
  
	#Now we want to repurpose the reference column to indicate whether the patient was in the reference group or not.
	finalData$REFERENCE[finalData$REFERENCE == ""]   	<- 0
	finalData$REFERENCE[finalData$REFERENCE != 0]   	<- 1
  
	#Remove patients from the frame who don't have an "X" Value.
	finalData <- finalData[finalData$X != "",]

	#Make the outcome column a character one so we don't have to fight with factors.
	finalData$OUTCOME <- as.character(finalData$OUTCOME)

	#We want to add another flag column to keep track of which outcome was the top outcome. We handle this differentely depending on if we binned the box or not.
	if(binning.Dep.enabled)
	{
		#Grab a unique list of the bins that we made.
		binList <- unique(finalData$Y)
		
		lowerList <- c()
		nameList <- c()

		#Loop through all the bins and extract the lower range and the bin name.
		for (bin in binList)
		{
			#Split the name of the bin.
			binSplit <- strsplit(as.character(bin), " ")
			
			#Grab the first element of the bin name.
			binLower <- as.numeric(binSplit[[1]][1])
			
			lowerList <- c(lowerList,binLower)
			nameList <- c(nameList,bin)
			
		}
		
		#Create a data frame from the lists of lower bounds and names.
		finalDataFrame <- data.frame(binLower = lowerList, binName = nameList)
		
		#Reorder the data frame by the binLower column.
		finalDataFrame <- finalDataFrame[with(finalDataFrame, order(binLower)), ]
		
		#Pull the name of the lower bin
		topBinName <- finalDataFrame$binName[1]
		
		#Any patient with this concept is considered the "Top Outcome".
		finalData$OUTCOME[finalData$Y == topBinName] <- 1
		finalData$OUTCOME[finalData$OUTCOME != 1] <- 0
	}	else if(nonEventCheckbox == "false")	{
		#First we split the values in the dependent variable box.
		depenedent.concept.list <- strsplit(concept.dependent,"\\|")
		depenedent.concept.list <- unlist(depenedent.concept.list);
		
		#Any patient with this concept is considered the "Top Outcome".
		finalData$OUTCOME[finalData$OUTCOME == depenedent.concept.list[1]] <- 1
		
		finalData$OUTCOME[finalData$OUTCOME != 1] <- 0
	}	else if(nonEventCheckbox == "true")	{
		#Make the outcome column a character one so we don't have to fight with factors.
		finalData$OUTCOME <- as.character(finalData$OUTCOME)
		finalData$Y <- as.character(finalData$Y)
		
		#Any patient who does not have an NA for the Y component is considered the "Top Outcome".
		finalData$OUTCOME[!is.na(finalData$Y)] <- 1
		finalData$OUTCOME[is.na(finalData$Y)] <- 0

		finalData$Y[is.na(finalData$Y)] <- "Other"		
	}
	#We need to remove records that have NA in either X,Y,Z, or Reference.
	finalData <- finalData[!is.na(finalData$X),]
	finalData <- finalData[!is.na(finalData$Y),]
	finalData <- finalData[!is.na(finalData$REFERENCE),]

	
	#Verify that we have data after merging the concepts.
	if(NROW(finalData)==0) stop("||FRIENDLY||R found no data after joining the selected concepts. Please verify that subjects exist that meet your input criteria.")
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	#write.matrix(finalData,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
	print("-------------------")
	##########################################
}


