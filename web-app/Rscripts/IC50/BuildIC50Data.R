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
#BuildIC50DataFile
#
###########################################################################

IC50Data.build <- 
function
(
	input.dataFile,
	concept.celllines = '',
	concept.dosage = '',
	concept.response = '',
	output.dataFile = 'outputfile'
)
{
	print("-------------------")
	print("BuilderIC50Data.R")
	print("BUILDING IC50 DATA")
	
	library(stringr)
	
	#Read the input file.
	dataFile <- data.frame(read.delim(input.dataFile));
	
	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")
	
	#Split the data by the CONCEPT_CD.
	splitData <- split(dataFile,dataFile$CONCEPT_PATH);
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(dataFile$PATIENT_NUM));
	
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")	

	celllineValueMatrix <- dataBuilder(splitData = splitData,concept = concept.celllines,concept.type = 'CLINICAL');	
	dosageValueMatrix <- dataBuilder(splitData = splitData,concept = concept.dosage, concept.type = 'CLINICAL',conceptColumn = TRUE);	
	responseValueMatrix <- dataBuilder(splitData = splitData,concept = concept.response, concept.type = 'CLINICAL', conceptColumn = TRUE);	
	
	#These are the columns we pull from the temp matrix.
	celllineMatrixColumns <- c('PATIENT_NUM','VALUE')
	dosageMatrixColumns <- c('PATIENT_NUM','CONCEPT','VALUE')
	responseMatrixColumns <- c('PATIENT_NUM','CONCEPT','VALUE')
	
	#We reset the column names after merging.
	celllineFinalColumns <- c('CELL_LINE')
	dosageFinalColumns <- c('DOSAGE')
	responseFinalColumns <- c('RESPONSE')		
	
	#We need to merge the dosage and response by patient_num and the last part of their concept code. Alter the concept code column here for those two data frames.
	dosageValueMatrix$CONCEPT <- str_extract(dosageValueMatrix$CONCEPT,"(\\\\.+\\\\)+?$")
	responseValueMatrix$CONCEPT <- str_extract(responseValueMatrix$CONCEPT,"(\\\\.+\\\\)+?$")
	
	#Merge in the cell lines.
	finalData<-merge(finalData,celllineValueMatrix[celllineMatrixColumns],by="PATIENT_NUM")
	
	#Merge in all the dosages.
	finalData<-merge(finalData,dosageValueMatrix[dosageMatrixColumns],by=c("PATIENT_NUM"))
	
	#Merge in the responses, but with a concept field as another filter. This is where the concept names must match so we know the correct dosage/response combination.
	finalData<-merge(finalData,responseValueMatrix[responseMatrixColumns],by=c("PATIENT_NUM","CONCEPT"))
	
	#Create column names.
	colnames(finalData) <- c('PATIENT_NUM','CONCEPT',celllineFinalColumns,dosageFinalColumns,responseFinalColumns)	
	
	#Make sure we actually have data after the merging.
	#if(length(finalData)==0 || !finalData) stop("||FRIENDLY||R found no data after joining the selected concepts. Please verify that samples exist that meet your input criteria.")
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	write.matrix(finalData,output.dataFile,sep = "\t")
	print("-------------------")
	##########################################
}