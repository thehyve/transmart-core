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
#BuildWaterfallDataFile
###########################################################################

WaterfallData.build <- 
function
(
	input.dataFile,
	concept = '',
	concept.type = 'CLINICAL',
	output.dataFile="outputfile",
	sample = '',
	time = '',
	tissues = '',
	input.gexFile = '',
	genes = '',
	genes.aggregate = FALSE,
	input.snpFile = '',
	snptype = '',
	lowRangeOperator = '',
	lowRangeValue = '',
	highRangeOperator = '',
	highRangeValue = '',
	gpl = ''
)
{
	print("-------------------")
	print("BuilderWaterfallData.R")
	print("BUILDING WATERFALL DATA")
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
	barValueMatrix <- dataBuilder(splitData = splitData,
								concept = concept,
								concept.type = concept.type,
								sampleType = sample,
								timepointType = time,
								tissueType = tissues,
								platform.type = gpl,
								GEXFile = input.gexFile,
								gene.list = genes,
								gene.aggregate = genes.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype);	
	##########################################
	
	#These are the columns we pull from the temp matrix.
	barMatrixColumns <- c('PATIENT_NUM','VALUE')
	
	barFinalColumns <- c('X')
	
	finalData<-merge(finalData,barValueMatrix[barMatrixColumns],by="PATIENT_NUM")
	
	#Create column names.
	colnames(finalData) <- c('PATIENT_NUM',barFinalColumns)

	#We need to add a column that indicate if the group should be colored or not.
	finalData$GROUP <- NA
	
	if(lowRangeValue != '') finalData <- addWaterfallRange(finalData,lowRangeOperator,lowRangeValue,'LOW')
	if(highRangeValue != '') finalData <- addWaterfallRange(finalData,highRangeOperator,highRangeValue,'HIGH')

	#All the remaining fill values should be 0.
	finalData$GROUP[is.na(finalData$GROUP)] <- 'BASE'
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	#write.matrix(format(finalData,sci=FALSE),output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(format(finalData,sci=FALSE),filename, sep = "\t", quote = FALSE, row.names = FALSE)
	print("-------------------")
	##########################################
}

#This will fill in the GROUP variable based on the range operator and ranged passed in.
addWaterfallRange <- 
function
(
dataToRange,
rangeOperator,
rangeValue,
fillValue
)
{
	#Find out which operator we are using.
	if(rangeOperator == '<')
	{
		dataToRange$GROUP[dataToRange$X < as.numeric(rangeValue)] <- fillValue
	}
	else if(rangeOperator == '<=')
	{
		dataToRange$GROUP[dataToRange$X <= as.numeric(rangeValue)] <- fillValue
	}
	else if(rangeOperator == '=')
	{
		dataToRange$GROUP[dataToRange$X == as.numeric(rangeValue)] <- fillValue
	}
	else if(rangeOperator == '>')
	{
		dataToRange$GROUP[dataToRange$X > as.numeric(rangeValue)] <- fillValue
	}
	else if(rangeOperator == '>=')
	{
		dataToRange$GROUP[dataToRange$X >= as.numeric(rangeValue)] <- fillValue
	}

	return(dataToRange)
	
}
