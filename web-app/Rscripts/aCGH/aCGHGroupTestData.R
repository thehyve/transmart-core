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
#aCGHgroupTestDataFile
#Parse the i2b2 output file and create input files for Group Test.
###########################################################################

aCGHGroupTestData.build <- 
function
(
  input.dataFile,
  input.acghFile,
  output.dataFile="input",
  concept.time,
  concept.category = "",
  concept.eventNo = ""
)
{
  print("-------------------")
	print("aCGHGroupTestData.R")
	print("BUILDING ACGH GROUP TEST DATA")
	
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
	finalData<-merge(finalData,splitData[[gsub("([\\])","\\\\\\\\",concept.time)]][c('PATIENT_NUM','VALUE')],by="PATIENT_NUM")
	
	#If eventNo was not specified, we consider everyone to have had the event.
	if(concept.eventNo=="")
	{
		finalData<-cbind(finalData,0)
	}	else {
		#We merge the eventNo in, everything else gets set to NA. We will mark them as censored later.
		eventNo<-strsplit(gsub("([\\])","\\\\\\\\",concept.eventNo)," [|] ")
    if(length(eventNo[[1]])>1) {
      #Multiple eventNo
		  boundData<-rbind(splitData[eventNo[[1]][1]][[1]],splitData[eventNo[[1]][2]][[1]])
    } else {
      #Single eventNo
      boundData<-splitData[eventNo[[1]][1]][[1]]
    }
		finalData<-merge(finalData,boundData[c('PATIENT_NUM','VALUE')],by="PATIENT_NUM",all.x=TRUE)	
	}
	
	#This is the list of columns for the final data object.
	finalColumnNames <- c("PATIENT_NUM","TIME","CENSOR") #,"CATEGORY")
	
	if("GROUP" %in% colnames(finalData)) finalColumnNames <- c(finalColumnNames,'GROUP')
	
	#Rename the columns.
	colnames(finalData) <- finalColumnNames
	
	#Make sure we have the value levels for the CENSOR column. This may throw a warning for duplicate values, but we can ignore it.
	finalData$CENSOR <- factor(finalData$CENSOR, levels = c(levels(finalData$CENSOR), "1"))
	finalData$CENSOR <- factor(finalData$CENSOR, levels = c(levels(finalData$CENSOR), "0"))

	#Replace the NA values in the CENSOR column with 0 (Censored).
	finalData$'CENSOR'[is.na(finalData$'CENSOR')] <- 0
	
	#Everything that isn't a 0 in the CENSOR column needs to be a 1 (Event happened).
	finalData$'CENSOR'[!finalData$'CENSOR'=='0'] <- 1		
	
	###################################	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	write.matrix(finalData,"outputfile",sep = "\t")
	print("-------------------")
}
