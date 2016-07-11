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
#BuildScatterDataFile
#Parse the i2b2 output file and create input files for a scatter plot.
###########################################################################

ScatterData.build <- 
function
(
input.dataFile,
input.gexFile = '',
input.snpFile = '',
output.dataFile="outputfile",
concept.dependent,
concept.independent,
concept.dependent.type = "CLINICAL",
concept.independent.type = "CLINICAL",
genes.dependent = '',
genes.dependent.aggregate = FALSE,
genes.independent = '',
genes.independent.aggregate = FALSE,
sample.dependent = '',
sample.independent = '',
time.dependent = '',
time.independent = '',
tissues.dependent = '',
tissues.independent = '',
snptype.dependent = '',
snptype.independent = '',
gpl.dependent = '',
gpl.independent = '',
logX=''
)
{
	print("-------------------")
	print("BuilderScatterData.R")
	print("BUILDING SCATTER DATA")
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
		
	##########################################
	#Build the X and Y values. They could be concepts from i2b2, or GEX/SNP values from another file.
	yValueMatrix <- dataBuilder(	splitData = splitData,
									concept = concept.dependent,
									concept.type = concept.dependent.type,
									sampleType = sample.dependent,
									timepointType = time.dependent,
									tissueType = tissues.dependent,
									platform.type = gpl.dependent,
									GEXFile = input.gexFile,
									gene.list = genes.dependent,
									gene.aggregate = genes.dependent.aggregate,
									SNPFile = input.snpFile,
									SNPType = snptype.dependent);
	
	xValueMatrix <- dataBuilder(	splitData = splitData,
									concept = concept.independent,
									concept.type = concept.independent.type,
									sampleType = sample.independent,
									timepointType = time.independent,
									tissueType = tissues.independent,
									platform.type = gpl.independent,
									GEXFile = input.gexFile,
									gene.list = genes.independent,
									gene.aggregate = genes.independent.aggregate,
									SNPFile = input.snpFile,
									SNPType = snptype.independent);
	##########################################

	##########################################
	#Merge the x and y matrices. Make sure we merge the group column if it exists.
	
	#These are the columns we pull from the temp matrix.
	xMatrixColumns <- c('PATIENT_NUM','VALUE')
	yMatrixColumns <- c('PATIENT_NUM','VALUE')
	
	#We reset the column names after merging.
	xFinalColumns <- c('X')
	yFinalColumns <- c('Y')
	
	#If the X matrix has a group column, reset the lists we use to make the columns here.
	if("GROUP" %in% colnames(xValueMatrix)) 
	{
		xMatrixColumns <- c('PATIENT_NUM','VALUE','GROUP')
		xFinalColumns <- c('X','GROUP')
	}
	
	#If the Y matrix has a group column, reset the lists we use to make the columns here.
	if("GROUP" %in% colnames(yValueMatrix)) 
	{
		yMatrixColumns <- c('PATIENT_NUM','VALUE','GROUP')
		yFinalColumns <- c('Y','GROUP')
	}
	
	finalData<-merge(finalData,xValueMatrix[xMatrixColumns],by="PATIENT_NUM")
	finalData<-merge(finalData,yValueMatrix[yMatrixColumns],by="PATIENT_NUM")
	
	#Create column names.
	colnames(finalData) <- c('PATIENT_NUM',xFinalColumns,yFinalColumns)

	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	#write.matrix(finalData,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
    print("-------------------")
	##########################################
}
