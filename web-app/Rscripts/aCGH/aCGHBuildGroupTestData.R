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
	  
	#Read the input file.
	dataFile <- data.frame(read.delim(input.dataFile));
	
	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")

	#List of available CONCEPT_PATH values to check availability of concepts specified as arguments
	allConcepts <- unique(dataFile$CONCEPT_PATH)
	
	#Split the data by the CONCEPT_CD.
	splitData <- split(dataFile,dataFile$CONCEPT_PATH)
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(dataFile$PATIENT_NUM))
	
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")
	
	#Calculate number of patients
	npatients <- nrow(finalData)
	
	#Add the value for the group to the final data.
	group<-strsplit(concept.group," *[|] *")
	# Check if at least one of the censor concepts is observed
	if (! any(group[[1]] %in% allConcepts)) stop(paste("||FRIENDLY||No observations found for group variable:",group[[1]]))

	groupData <- splitData[group[[1]][1]][[1]]

	if(length(group[[1]])>1) {
		#Multiple groups
		for (i in 2:length(group[[1]]) )
		{
			groupData<-rbind(groupData,splitData[group[[1]][i]][[1]])
		}
	} else {
		#Single group
		stop(paste("||FRIENDLY||Only a single group specified. Please check your group variable selection and run again."))
	}
	finalData<-merge(finalData,groupData[c('PATIENT_NUM','VALUE')],by="PATIENT_NUM",all.x=TRUE)	
	
	if (nrow(finalData)>npatients) stop(paste("||FRIENDLY||Patients not uniquely divided over the groups"))

	finalColumnNames <- c("PATIENT_NUM",output.column.group)
	colnames(finalData) <- finalColumnNames
	  
	###################################	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	write.matrix(finalData,output.dataFile,sep = "\t")
	print("-------------------")
}
