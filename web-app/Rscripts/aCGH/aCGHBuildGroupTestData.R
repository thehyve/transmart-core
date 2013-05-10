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
  concept.category,
  concept.group,
  output.column.group='group',
  output.dataFile='phenodata.tsv',
  output.acghFile='regions.tsv'                      
)
{
	print("-------------------")
	print("aCGHBuildGroupTestData.R")
	print("BUILDING ACGH GROUP TEST DATA")

	# Copy the aCGH file
	file.copy(input.acghFile,output.acghFile,overwrite = TRUE)
	  
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
	
	#Add the value for the time to the final data.
	#group<-strsplit(gsub("([\\])","\\\\\\\\",concept.group)," [|] ")
	group<-strsplit(concept.group," [|] ")
	if(length(group[[1]])>1) {
	#Multiple groups
	  boundData<-rbind(splitData[group[[1]][1]][[1]],splitData[group[[1]][2]][[1]])
	} else {
	#Single eventNo
	  boundData<-splitData[group[[1]][1]][[1]]
	}
	finalData<-merge(finalData,boundData[c('PATIENT_NUM','VALUE')],by="PATIENT_NUM",all.x=TRUE)	
	
	finalColumnNames <- c("PATIENT_NUM",output.column.group)
	colnames(finalData) <- finalColumnNames
	  
	###################################	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	write.matrix(finalData,output.dataFile,sep = "\t")
	print("-------------------")
}
