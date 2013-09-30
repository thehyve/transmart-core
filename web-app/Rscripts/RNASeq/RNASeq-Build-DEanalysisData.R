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
#RNASeq-Build-DEanalysisData
#Parse the i2b2 output file and create input files for RNASeq-EdgeR-DEanalysis.
###########################################################################

RNASeqDEanalysisData.build <- 
function
(
  input.clinicalFile='clinical.txt',
  input.rnaseqFile='RNASeq.txt',
  concept.readcount,
  concept.group,
  output.column.group='group',
  output.clinicalFile='phenodata.tsv',
  output.rnaseqFile='readcount.tsv'                      
)
{
	print("-------------------")
	print("RNASeq-Build-DEanalysisData.R")
	print("BUILDING RNASeq DEanalysis DATA")

	# Check parameters
	if( missing(concept.group) || is.null(concept.group) || nchar(concept.group) == 0) stop("||FRIENDLY||No grouping specified. Please check your group variable selection and run again.")

	# Check presence of rnaseq data file and clinical data file
	if(!file.exists(input.rnaseqFile)) stop("||FRIENDLY||No RNASeq data file found. Please check your region variable selection and run again.")
	if(!file.exists(input.clinicalFile)) stop("||FRIENDLY||No clinical data file found. Please check your group variable selection and run again.")

	readcountTable <- read.table(input.rnaseqFile, header=TRUE, sep='\t', quote='\"', as.is=TRUE, check.names=FALSE)
	readcountTableColumnNames <- colnames(readcountTable)

	# Use regionname as row names
	rownames(readcountTable) <- readcountTable$regionname

	# Only readcount columns
	filteredReadcountTableColumnNames <- readcountTableColumnNames[ grep("readcount.", readcountTableColumnNames) ]
	if(length(filteredReadcountTableColumnNames)==0)  stop("||FRIENDLY||No read count data found. Please check your read count variable selection and run again.")
	
	readcountTable <- readcountTable[filteredReadcountTableColumnNames]

	readcountTableColumnNames <- colnames(readcountTable)
	readcountTableColumnNames <- gsub("\"", "", readcountTableColumnNames)
	readcountTableSubjectIds <- sub("readcount.", "" , readcountTableColumnNames)
	
	if(!length(readcountTableSubjectIds)>0) stop("||FRIENDLY||No subjects with readcount data found in RNASeq data file.")

	colnames(readcountTable) <- readcountTableSubjectIds
	
	#Read the input file.
	clinicaldataFile <- read.table(input.clinicalFile, header=TRUE, sep='\t', quote='\"', strip.white=TRUE, as.is=TRUE, check.names=FALSE)

	#Set the column names.
	colnames(clinicaldataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH_FULL")

	#Filter on patients for which RNASeq data is available
	filteredData <- matrix(readcountTableSubjectIds)
	colnames(filteredData) <- c("PATIENT_NUM")
	filteredData <- merge(filteredData, clinicaldataFile) # natural (inner) join

	#List of available CONCEPT_PATH_FULL values to check availability of concepts specified as arguments
	allAvailableFullConcepts <- unique(filteredData$CONCEPT_PATH_FULL)
	#Parse the concepts specifying the groups
	specifiedGroupConcepts <- strsplit(concept.group," *[|] *")[[1]]

	# Check if at least one of the group concepts is observed
	if (! any(specifiedGroupConcepts %in% allAvailableFullConcepts)) stop(paste("||FRIENDLY||No observations found for group variable:",specifiedGroupConcepts))

	#Further filter data on specified full concept paths only
	filteredData <- subset(filteredData, CONCEPT_PATH_FULL %in% specifiedGroupConcepts)
	
	allAvailableShortConcepts <- unique(filteredData$CONCEPT_PATH_SHORT)
	if(length(allAvailableShortConcepts)==0) stop(paste("||FRIENDLY||No observations found for group variable:",specifiedGroupConcepts))
	
	#Split the data by the CONCEPT_PATH_SHORT (categorical variable).
	splitData <- split(filteredData,filteredData$CONCEPT_PATH_SHORT)

	groupData <- splitData[allAvailableShortConcepts[1]] [[1]] [,c("PATIENT_NUM","VALUE")]
	colnames(groupData) <- c("PATIENT_NUM",allAvailableShortConcepts[1])
  
	if(length(allAvailableShortConcepts)>1) {
		# Multiple categorical variables
		for (i in 2:length(allAvailableShortConcepts) )
		{
			grpdata <- splitData[allAvailableShortConcepts[i]] [[1]] [,c("PATIENT_NUM","VALUE")]
			colnames(grpdata) <- c("PATIENT_NUM",allAvailableShortConcepts[i])
		 	groupData <- merge(groupData ,grpdata, by="PATIENT_NUM", all=TRUE)
		}
	}
	
	# Select only patients/subjects for which all concepts have been observed
	groupData <- groupData[apply(!is.na(groupData), 1, all), ]

	# Check if still patients are left that match the criteria
	if (nrow(groupData) < 2) stop(paste("||FRIENDLY||Not enough patients/subjects/samples (",nrow(groupData),") to form at least 2 groups",sep=""))

	# Merge the multiple observations into a single combined observation of a combined concept (cross table)
	groupData$combinedConcepts <- do.call(paste, c(groupData[allAvailableShortConcepts], sep = "_"))

	groupData <- groupData[c('PATIENT_NUM','combinedConcepts')]
	
	# Check if at least two groups have been specified
	if ( length(unique(groupData$combinedConcepts)) < 2 ) stop(paste("||FRIENDLY||All patient belong to the same group. Please specifiy at least 2 distinct groups"))
	# Check if patient are uniquely divided over the groups
	if (nrow(groupData) != length(unique(groupData$PATIENT_NUM))) stop(paste("||FRIENDLY||Patients not uniquely divided over the groups"))
	# Check size of groupsize on average > 1 (i.e. not as many groups as patients)
	if (nrow(groupData) == length(unique(groupData$combinedConcepts))) stop(paste("||FRIENDLY||Size of groups too small (as many groups as patients(",nrow(groupData),"))",sep=""))

	groupColumnNames <- c("PATIENT_NUM",output.column.group)
	colnames(groupData) <- groupColumnNames

	# Make row names equal to the patient_num column value
	rownames(groupData) <- groupData[,"PATIENT_NUM"]

	# Reorder the readcount table columns like the groupData rows
	# If the group contains less patients than the readcounts table, subset to the smaller group.
	readcountTable <- readcountTable[,rownames(groupData)]
  
	## Reorder groupData rows to match the order in the readcount data columns
	## Not needed anymore since reordering and subsetting readcounts
	#  groupData <- groupData[readcountTableSubjectIds,,drop=FALSE]

	# Write the readcount file
	write.table(readcountTable,output.rnaseqFile, sep='\t', quote=FALSE, row.names=TRUE, col.names=TRUE)
  
	###################################	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the group data file.
	write.table(groupData,output.clinicalFile,sep = "\t", quote=FALSE, row.names=FALSE, col.names=TRUE)

	print("-------------------")
}
