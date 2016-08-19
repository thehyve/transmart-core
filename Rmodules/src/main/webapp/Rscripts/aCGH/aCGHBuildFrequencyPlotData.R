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
#aCGHBuildFrequencyPlotDataFile
#Parse the i2b2 output file and create input files for Frequency Plot.
###########################################################################

aCGHFrequencyPlotData.build <- 
function
(
  input.dataFile='clinical.txt',
  input.acghFile='aCGH.txt',
  concept.region,
  concept.group,
  output.column.group='group',
  output.dataFile='phenodata.tsv',
  output.acghFile='regions.tsv'                      
)
{
	print("-------------------")
	print("aCGHBuildFrequencyPlotData.R")
	print("BUILDING ACGH FREQUENCY PLOT DATA")

	# Check parameters

	# Check presence of aCGH data file and clinical data file
	if(!file.exists(input.acghFile)) stop("||FRIENDLY||No aCGH data file found. Please check your region variable selection and run again.")
	if(!file.exists(input.dataFile)) stop("||FRIENDLY||No clinical data file found. Please check your group variable selection and run again.")

	# Copy the aCGH file
	file.copy(input.acghFile,output.acghFile,overwrite = TRUE)
	
	# Extract patient list from aCGH data column names for which calls (flag) have been observed
	headernames <- gsub("\"", "", strsplit(readLines(input.acghFile,1),' *\t *')[[1]])
	pids <- sub("flag.", "" , headernames[grep('flag.', headernames)] )
	if(!length(pids)>0) stop("||FRIENDLY||No subjects with call data found in aCGH data file.")

	#Read the input file.
	dataFile <- read.table(input.dataFile, header=TRUE, sep='\t', quote='\"', strip.white=TRUE, as.is=TRUE, check.names=FALSE)

	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH_FULL")

	#Filter on patients for which aCGH data is available
	filteredData <- matrix(pids)
	colnames(filteredData) <- c("PATIENT_NUM")
	filteredData <- merge(filteredData, dataFile) # natural (inner) join

	#List of available cohort values
	allAvailableCohorts <- unique(filteredData$SUBSET)

	#List of available CONCEPT_PATH_FULL values to check availability of concepts specified as arguments
	allAvailableFullConcepts <- unique(filteredData$CONCEPT_PATH_FULL)

	if( missing(concept.group) || is.null(concept.group) || nchar(concept.group) == 0)
	{
		specifiedGroupConcepts = c()
	} else {
		#Parse the concepts specifying the groups
		specifiedGroupConcepts <- strsplit(concept.group," *[|] *")[[1]]
	}

	allAvailableShortConcepts = c()
  if( length(specifiedGroupConcepts) > 0)
  {
		# Check if at least one of the specified group concepts is observed
		if (! any(specifiedGroupConcepts %in% allAvailableFullConcepts)) stop(paste("||FRIENDLY||No observations found for group variable:",specifiedGroupConcepts))

		#Further filter data on specified full concept paths only
		filteredData <- subset(filteredData, CONCEPT_PATH_FULL %in% specifiedGroupConcepts)

		allAvailableShortConcepts <- unique(filteredData$CONCEPT_PATH_SHORT)
		if(length(allAvailableShortConcepts) > 0)
		{
			#Split the data by the CONCEPT_PATH_SHORT.
			splitDataOnConcept <- split(filteredData,filteredData$CONCEPT_PATH_SHORT)

			grpdata1 <- splitDataOnConcept[allAvailableShortConcepts[1]] [[1]] [,c("PATIENT_NUM","VALUE")]
			colnames(grpdata1) <- c("PATIENT_NUM",allAvailableShortConcepts[1])
		}
	}

	allAvailableShortConceptsOrCohort <- allAvailableShortConcepts
	
	#Include the grouping by cohorts
	if(length(allAvailableCohorts)> 1 || length(specifiedGroupConcepts) == 0)
	{
		groupData <- filteredData[,c("PATIENT_NUM","SUBSET")]
		groupData <- groupData[!duplicated(groupData),]
		colnames(groupData) <- c("PATIENT_NUM","COHORT")

		allAvailableShortConceptsOrCohort <- c("COHORT",allAvailableShortConcepts)

		if(length(allAvailableShortConcepts)>0) {
			groupData <- merge(groupData ,grpdata1, by="PATIENT_NUM", all=TRUE)
		}
	}
	else {
		groupData <- grpdata1
	}
  
	if( length(allAvailableShortConcepts) > 1) {
		#Multiple categorical variables
		for (i in 2:length(allAvailableShortConcepts) )
		{
			grpdata <- splitDataOnConcept[allAvailableShortConcepts[i]] [[1]] [,c("PATIENT_NUM","VALUE")]
			colnames(grpdata) <- c("PATIENT_NUM",allAvailableShortConcepts[i])
		 	groupData <- merge(groupData ,grpdata, by="PATIENT_NUM", all=TRUE)
		}
	}
	
	# Select only patients/subjects for which all concepts have been observed
	groupData <- groupData[apply(!is.na(groupData), 1, all), ]

	# Check if still patients are left that match the criteria
	if (nrow(groupData) < 2) stop(paste("||FRIENDLY||Not enough patients/subjects/samples (",nrow(groupData),") to form at least 2 groups",sep=""))

	# Merge the observations of multiple variables into a combined observation of a single variable (cross table)
	groupData$combinedConcepts <- do.call(paste, c(groupData[allAvailableShortConceptsOrCohort], sep = "_"))

	groupData <- groupData[c('PATIENT_NUM','combinedConcepts')]
	
	# Check if patient are uniquely divided over the groups
	if (nrow(groupData) != length(unique(groupData$PATIENT_NUM))) stop(paste("||FRIENDLY||Patients not uniquely divided over the groups"))
	# Check size of groupsize on average > 1 (i.e. not as many groups as patients)
	if (nrow(groupData) == length(unique(groupData$combinedConcepts))) stop(paste("||FRIENDLY||Size of groups too small (as many groups as patients(",nrow(groupData),"))",sep=""))	

	groupColumnNames <- c("PATIENT_NUM",output.column.group)
	colnames(groupData) <- groupColumnNames

	# In case aCGH data contains more patients/samples columns than groupData rows, groupData will have a number of rows with NA values.
	# These patients/samples/rows will be neglect in the acgh group test script.
	# TODO: It would however be cleaner to remove those patients/samples from the acgh data (columns) completely 

	# Make row names equal to the patient_num column value
	rownames(groupData) <- groupData[,"PATIENT_NUM"]
	# Reorder groupData rows to match the order in the aCGH data columns
	groupData <- groupData[pids,,drop=FALSE]

	###################################	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the group data file.
	write.table(groupData,output.dataFile,sep = "\t", quote=FALSE, row.names=FALSE, col.names=TRUE)

	print("-------------------")
}
