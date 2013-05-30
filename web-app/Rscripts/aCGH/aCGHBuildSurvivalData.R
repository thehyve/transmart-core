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
#aCGHBuildSurvivalDataFile
#Parse the i2b2 output file and create input files for aCGH Survival Curve.
###########################################################################

aCGHSurvivalData.build <- 
function
(
  input.dataFile='clinical.txt',
  input.acghFile='aCGH.txt',
  output.dataFile="phenodata.tsv",
  output.acghFile="regions.tsv",
  output.survival='Overall survival time',
  output.status='Survival status',
  concept.time,
  concept.region = "",
  concept.eventNo = ""
)
{
	print("-------------------")
	print("aCGHBuildSurvivalData.R")
	print("BUILDING ACGH SURVIVAL DATA")

	# Check parameters
	if( missing(concept.time) || is.null(concept.time) || nchar(concept.time) == 0) stop("||FRIENDLY||No survival time specified. Please check your time variable selection and run again.")

	# Check presence of aCGH data file and clinical data file
	if(!file.exists(input.acghFile)) stop("||FRIENDLY||No aCGH data file found. Please check your region variable selection and run again.")
	if(!file.exists(input.dataFile)) stop("||FRIENDLY||No clinical data file found. Please check your time variable selection and run again.")

	# Copy the aCGH file
	file.copy(input.acghFile, output.acghFile, overwrite=TRUE)

	# Extract patient list from aCGH data column names for which calls (flag) have been observed
	headernames <- gsub("\"", "", strsplit(readLines(input.acghFile,1),' *\t *')[[1]])
	pids <- sub("flag.", "" , headernames[grep('flag.', headernames)] )
	if(!length(pids)>0) stop("||FRIENDLY||No subjects with call data found in aCGH data file.")

	#Read the clinical data file.
	dataFile <- read.table(input.dataFile, header=TRUE, sep='\t', quote='"', strip.white=TRUE, as.is=TRUE, check.names=FALSE)
	
	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")

	#Filter on patients for which aCGH data is available
	filteredData <- matrix(pids)
	colnames(filteredData) <- c("PATIENT_NUM")
	filteredData <- merge(filteredData, dataFile, all.x=TRUE)
		
	#List of available CONCEPT_PATH values to check availability of concepts specified as arguments
	allConcepts <- unique(filteredData$CONCEPT_PATH)

	if (! concept.time %in% allConcepts) stop(paste("||FRIENDLY||No observations found for survival time variable:",concept.time))

	#Split the data by the CONCEPT_PATH.
	splitData <- split(filteredData,filteredData$CONCEPT_PATH);
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(filteredData$PATIENT_NUM))
		
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")
	
	#Add the value for the time to the final data.
	finalData<-merge(finalData,splitData[[concept.time]][c('PATIENT_NUM','VALUE')],by="PATIENT_NUM", all.x=TRUE)

	#If eventNo was not specified, we consider everyone to have had the event.
	if(concept.eventNo=="")
	{
		# No censoring variables, all events happened ((EventHappened=1) == Death)
		finalData<-cbind(finalData,1)
		
	} else {
		# We merge the eventNo ((eventHappened=0 == Alive) in, everything else gets set to NA. We will mark them as uncensored later.
		eventNo<-strsplit(concept.eventNo," *[|] *")
		
		# Check if at least one of the censor concepts is observed
		if (! any(eventNo[[1]] %in% allConcepts)) stop(paste("||FRIENDLY||No observations found for censoring variable:",eventNo[[1]]))

		boundData <- splitData[eventNo[[1]][1]][[1]]

		if(length(eventNo[[1]])>1) {
			# Multiple eventNo concepts
			for (i in 2:length(eventNo[[1]]) )
			{
				boundData<-rbind(boundData,splitData[eventNo[[1]][i]][[1]])
			}
		}
		# For all patients for which the censoring variable has been observed the event didn't happen ((EventHappened=0) == Alive)
		# The censoring variable has been observed for all patients for which the VALUE column contains a value not equal to NA
		boundData$CENSOR[!is.na(boundData$VALUE)] <- 0
		boundData$CENSOR <- factor(boundData$CENSOR , levels = c("0", "1"))

		censorData<-unique(boundData[c('PATIENT_NUM','CENSOR')])

		finalData<-merge(finalData,censorData,by="PATIENT_NUM",all.x=TRUE)	
	}

	# This is the list of columns for the final data object.
	finalColumnNames <- c("PATIENT_NUM","TIME","CENSOR")
	# Rename the columns.
	colnames(finalData) <- finalColumnNames

	#Replace the NA values in the CENSOR column with 1 (Not Censored, (EventHapped=1), Death).
	finalData$'CENSOR'[is.na(finalData$'CENSOR')] <- 1

	finalColumnNames <- c("PATIENT_NUM",output.survival,output.status)
	colnames(finalData) <- finalColumnNames

	###################################	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	write.table(finalData,output.dataFile,sep = "\t", quote=FALSE, row.names=FALSE, col.names=TRUE)

	print("-------------------")
}
