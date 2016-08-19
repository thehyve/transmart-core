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
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH","PARENT_PATH","PARENT_CODE","CONTEXT_NAME")
	
	#Split the data by the CONCEPT_CD.
	splitData <- split(dataFile,dataFile$CONCEPT_PATH);
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(dataFile$PATIENT_NUM));
	
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")	

	celllineValueMatrix <- dataBuilder(splitData = splitData,concept = concept.celllines,concept.type = 'CLINICAL');	
	
	#Grab the Dose/Response Data.
	dosageValueMatrix 		<- dataFile[which(dataFile$CONTEXT_NAME == "DOSE"),]
	responseValueMatrix 	<- dataFile[which(dataFile$CONTEXT_NAME == "RESPONSE"),]
	cvValueMatric			<- dataFile[which(dataFile$CONTEXT_NAME == "COEFFICIENT_OF_VARIATION"),]
	
	#These are the columns we pull from the temp matrix.
	celllineMatrixColumns <- c('PATIENT_NUM','VALUE')
	dosageMatrixColumns <- c('PATIENT_NUM','PARENT_CODE','VALUE')
	responseMatrixColumns <- c('PATIENT_NUM','PARENT_CODE','VALUE')
	cvMatrixColumns <- c('PATIENT_NUM','PARENT_CODE','VALUE')
	
	#We reset the column names after merging.
	celllineFinalColumns <- c('CELL_LINE')
	dosageFinalColumns <- c('DOSAGE')
	responseFinalColumns <- c('RESPONSE')		
	cvFinalColumns <- c('CV')		
	
	#Merge in the cell lines.
	finalData<-merge(finalData,celllineValueMatrix[celllineMatrixColumns],by="PATIENT_NUM")
	
	#Merge in all the dosages.
	finalData<-merge(finalData,dosageValueMatrix[dosageMatrixColumns],by=c("PATIENT_NUM"))
	
	#Merge in the responses, but with a concept field as another filter. 
	finalData<-merge(finalData,responseValueMatrix[responseMatrixColumns],by=c("PATIENT_NUM","PARENT_CODE"))
	
	#Merge in the CV values.
	finalData<-merge(finalData,cvValueMatric[cvMatrixColumns],by=c("PATIENT_NUM","PARENT_CODE"))
	
	#Create column names.
	colnames(finalData) <- c('PATIENT_NUM','CONCEPT',celllineFinalColumns,dosageFinalColumns,responseFinalColumns,cvFinalColumns)	
	
	#Make sure we actually have data after the merging.
	#if(length(finalData)==0 || !finalData) stop("||FRIENDLY||R found no data after joining the selected concepts. Please verify that samples exist that meet your input criteria.")
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	#write.matrix(finalData,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
	print("-------------------")
	##########################################
}
