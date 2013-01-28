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
#BuildLinearDataFile
#Parse the i2b2 output file and create input files for graphing a line.
###########################################################################

LinearData.build <- 
function
(
input.dataFile,
output.dataFile="outputfile",
concept.dependent="",
concept.independent="",
concept.group=""
)
{

	dataFile <- data.frame(read.delim(input.dataFile));

	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")

	splitData <- split(dataFile,dataFile$CONCEPT_PATH);

	#I am leaving this code out, but if we ever decide to use an input box for the timepoint category, we'll need this.
	##########################################
	#Get the distinct values for the X axis. (In this case, weeks)
	#splitConcept <- strsplit(currentIndependent,"\\|");
	#splitConcept <- unlist(splitConcept);

	#This will be a temp matrix with VALUE.
	#tempConceptMatrix <- matrix(ncol=1,nrow=0);

	#For each of the passed in concepts, append the rows onto the end of our temp matrix.
	#for(entry in splitConcept)
	#{
	#	tempConceptMatrix <- rbind(tempConceptMatrix,splitData[[entry]][c('PATIENT_NUM','VALUE')])		
	#}

	#Add column names to our temp matrix.
	#colnames(tempConceptMatrix) <- c('PATIENT_NUM','VALUE')

	#independent.frame <- tempConceptMatrix
	##########################################

	##########################################
	#Get a table of Y value/Patient
	splitConcept <- strsplit(concept.dependent,"\\|");
	splitConcept <- unlist(splitConcept);

	#This will be a temp matrix with VALUE.
	tempConceptMatrix <- matrix(ncol=1,nrow=0);

	#For each of the passed in concepts, append the rows onto the end of our temp matrix.
	for(entry in splitConcept)
	{
	  tempConceptMatrix <- rbind(tempConceptMatrix,splitData[[entry]][c('PATIENT_NUM','CONCEPT_PATH','VALUE')])		
	}
	
	#Add column names to our temp matrix.
	colnames(tempConceptMatrix) <- c('PATIENT_NUM','CONCEPT_PATH','VALUE')

	yValueMatrix <- tempConceptMatrix
	##########################################

	##########################################
	#Get a table of groups/patient.
	splitConcept <- strsplit(concept.group,"\\|");
	splitConcept <- unlist(splitConcept);

	#This will be a temp matrix with VALUE.
	tempConceptMatrix <- matrix(ncol=1,nrow=0);

	#For each of the passed in concepts, append the rows onto the end of our temp matrix.
	for(entry in splitConcept)
	{
	  tempConceptMatrix <- rbind(tempConceptMatrix,splitData[[entry]][c('PATIENT_NUM','VALUE')])  	
	}

	#Add column names to our temp matrix.
	colnames(tempConceptMatrix) <- c('PATIENT_NUM','VALUE')

	groupValueMatrix <- tempConceptMatrix
	##########################################
	#A join between 2 data.tables X and Y is just X[Y].  This is much faster than merge().
	##########################################
	#Form a table of groups and y values.
	finalData<-merge(yValueMatrix[c('PATIENT_NUM','CONCEPT_PATH','VALUE')],groupValueMatrix[c('PATIENT_NUM','VALUE')],by="PATIENT_NUM")

	#Create column names.
	colnames(finalData) <- c('PATIENT_NUM','CONCEPT_PATH','VALUE','GROUP_VAR')

	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	write.table(finalData,"outputfile.txt",sep = "\t")
	##########################################

}



