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
#aCGHBuildGroupTestDataFile
#Parse the i2b2 output file and create input files for Group Test.
###########################################################################

aCGHGroupTestData.build <- 
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
	print("aCGHBuildGroupTestData.R")
	print("BUILDING ACGH GROUP TEST DATA")

	# Check parameters
	if( missing(concept.group) || is.null(concept.group) || nchar(concept.group) == 0) stop("||FRIENDLY||No grouping specified. Please check your group variable selection and run again.")

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
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")

	#Filter on patients for which aCGH data is available
	filteredData <- matrix(pids)
	colnames(filteredData) <- c("PATIENT_NUM")
	filteredData <- merge(filteredData, dataFile)

	#Split the data by the CONCEPT_CD.
	splitData <- split(filteredData,filteredData$CONCEPT_PATH)
	
	#List of available CONCEPT_PATH values to check availability of concepts specified as arguments
	allConcepts <- unique(filteredData$CONCEPT_PATH)

	#Add the value for the group to the group data table.
	group <- strsplit(concept.group," *[|] *")
	
	# Check if at least one of the censor concepts is observed
	if (! any(group[[1]] %in% allConcepts)) stop(paste("||FRIENDLY||No observations found for group variable:",group[[1]]))

	groupData <- splitData[group[[1]][1]][[1]]
	if(length(group[[1]])>1) {
		#Multiple groups
		for (i in 2:length(group[[1]]) )
		{
			groupData<-rbind(groupData,splitData[group[[1]][i]][[1]])
		}
	}

	groupData <- groupData[c('PATIENT_NUM','VALUE')]
	
	# Check if patient are uniquely divided over the groups
	if (nrow(groupData) != length(unique(groupData$PATIENT_NUM))) stop(paste("||FRIENDLY||Patients not uniquely divided over the groups"))
	# Check size of groupsize on average > 1 (i.e. not as many groups as patients)
	if (nrow(groupData) == length(unique(groupData$VALUE))) stop(paste("||FRIENDLY||Size of groups too small (as many groups as patients)"))

	groupColumnNames <- c("PATIENT_NUM",output.column.group)
	colnames(groupData) <- groupColumnNames

	###################################	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the group data file.
	write.table(groupData,output.dataFile,sep = "\t", quote=FALSE, row.names=FALSE, col.names=TRUE)

	print("-------------------")
}
