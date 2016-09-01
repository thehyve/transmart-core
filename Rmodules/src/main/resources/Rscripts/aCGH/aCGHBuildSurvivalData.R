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

	# Extract patient list from aCGH data column names for which calls (flag) have been observed
	headernames <- gsub("\"", "", strsplit(readLines(input.acghFile,1),' *\t *')[[1]])
	aCGHpids <- sub("flag.", "" , headernames[grep('flag.', headernames)] )
	if(!length(aCGHpids)>0) stop("||FRIENDLY||No subjects with call data found in aCGH data file.")

	#Read the clinical data file.
	dataFile <- read.table(input.dataFile, header=TRUE, sep='\t', quote='"', strip.white=TRUE, as.is=TRUE, check.names=FALSE)
	
	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")

	#Filter on patients for which aCGH data is available
	filteredData <- matrix(aCGHpids)
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

	#Add the value for time observation to the final data.
	#Merge is inner join (default option value is all=FALSE): subject lines for which no time observation exists are removed
	finalData<-merge(finalData,splitData[[concept.time]][c('PATIENT_NUM','VALUE')],by="PATIENT_NUM")

	# aCGH Survival analysis only makes sense if both aCGH data and survival time data is available for a subject.
	# The finalData variable contains only subjects which have both (aCGH and time) observations.
	# In case no time value is available for an aCGH subject, the aCGH data for this subject should be removed
	# from the aCGH file to prevent errors in the analysis algorithm.
	bothpids <- as.character(unique(finalData[,"PATIENT_NUM"]))
	
	# Check if list of subjects which have both observations is not empty
	if(length(bothpids) < 2) stop("||FRIENDLY||For at least two subjects both aCGH and survival time observations need to be available! Please check your region and time variable selection and run again.")
	
	# Create output acgh file with flag data only (no chip, probabilitiyof*) of subjects that have also a survival time observation.
	allData <- read.table(input.acghFile, header=TRUE, sep='\t', as.is=TRUE, check.names=FALSE)
	# Search first data column
	col.first.data <- min(grep('^chip\\.', colnames(allData)), grep('^flag\\.', colnames(allData)))
	# Compose flag column names for subjects that have both observations
	col.flags <- paste('flag.', bothpids, sep='')
	flagData <- cbind(allData[,c(1:(col.first.data-1))],allData[,col.flags,drop=FALSE])
	
	write.table(flagData, output.acghFile, sep = "\t", quote=TRUE, row.names=TRUE, col.names=TRUE)
	# Update list of aCGHpids
	aCGHpids <- bothpids

	#Check if concept.eventNo is specified and has observations in current patient cohort
	validEventNo = TRUE
	if( missing(concept.eventNo) || is.null(concept.eventNo) || nchar(concept.eventNo) == 0)
	{
	  validEventNo = FALSE
	}
	else
	{ # concept.eventNo is at least a non-zero length string. Further process it
		specifiedEventNoConcepts <- strsplit(concept.eventNo," *[|] *")[[1]]
		if (! any(specifiedEventNoConcepts %in% allConcepts))
		{
			# No observations found for censoring variable
			validEventNo = FALSE
		}
	}
	
	#If eventNo was not specified or no observations for it have been found, we consider everyone to have had the event.
	if(!validEventNo)
	{
		# No censoring variables, all events happened ((EventHappened=1) == Death)
		finalData<-cbind(finalData,1)
		
	} else {
		# We merge the eventNo ((eventHappened=0 == Alive) in, everything else gets set to NA. We will mark them as uncensored later.
		boundData <- splitData[specifiedEventNoConcepts[1]] [[1]]
		
		if(length(specifiedEventNoConcepts)>1) {
			# Multiple eventNo concepts
			for (i in 2:length(specifiedEventNoConcepts) )
			{
				boundData<-rbind(boundData,splitData[specifiedEventNoConcepts[i]] [[1]])
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

	# Make row names equal to the patient_num column value
	rownames(finalData) <- finalData[,"PATIENT_NUM"]
	# Reorder finalData rows to match the order in the aCGH data columns
	finalData <- finalData[aCGHpids,,drop=FALSE]
	###################################	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	write.table(finalData,output.dataFile,sep = "\t", quote=FALSE, row.names=FALSE, col.names=TRUE)

	print("-------------------")
}
