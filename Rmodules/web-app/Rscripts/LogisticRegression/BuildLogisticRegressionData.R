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


# test
###########################################################################
#BuildLogisticRegressionDataFile
#Parse the i2b2 output file and create input files for an Logistic Regression plot.
###########################################################################

LogisticRegressionData.build <- 
function
(
input.dataFile,
output.dataFile="outputfile",
concept.dependent,
concept.independent,
binning.enabled,
binning.bins = "",
binning.type = "",
binning.manual = FALSE,
binning.binrangestring = "",
binning.variabletype,
binning.variable = "IND",
input.gexFile = '',
input.snpFile = '',
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
gpl.independent= ''
)
{
	print("-------------------")
	print("BuildLogisticRegressionData.R")
	print("BUILDING LOGISTIC REGRESSION DATA")
	
	#Read the input file.
	dataFile <- data.frame(read.delim(input.dataFile));
	
	#Set the column names.
	colnames(dataFile) <- defaultColumnList()

	#Split the data by the CONCEPT_CD.
	splitData <- split(dataFile,dataFile$CONCEPT_PATH);
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(dataFile$PATIENT_NUM));
	
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")
	
	##########################################
	#Get a table of x/y value/Patient
	fullconcept.indep <- FALSE
	fullconcept.dep <- FALSE

	if(binning.variable == "IND" && binning.manual == TRUE && binning.variabletype == "Categorical") fullconcept.indep <- TRUE
	if(binning.variable == "DEP" && binning.manual == TRUE && binning.variabletype == "Categorical") fullconcept.dep <- TRUE	
	
	yValueMatrix <- dataBuilder(splitData = splitData,
								concept = concept.dependent,
								concept.type = concept.dependent.type,
								sampleType = sample.dependent,
								timepointType = time.dependent,
								tissueType = tissues.dependent,
								platform.type = '',
								GEXFile = input.gexFile,
								gene.list = genes.dependent,
								gene.aggregate = genes.dependent.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype.dependent,
								fullConcept = fullconcept.dep);
                
	xValueMatrix <- dataBuilder(splitData = splitData,
								concept = concept.independent,
								concept.type = concept.independent.type,
								sampleType = sample.independent,
								timepointType = time.independent,
								tissueType = tissues.independent,
								platform.type = '',
								GEXFile = input.gexFile,
								gene.list = genes.independent,
								gene.aggregate = genes.independent.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype.independent,
								fullConcept = fullconcept.indep);
	##########################################	

	##########################################
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
		yFinalColumns <- c('Y','GROUP.1')
	}

	finalData<-merge(finalData,xValueMatrix[xMatrixColumns],by="PATIENT_NUM")
	finalData<-merge(finalData,yValueMatrix[yMatrixColumns],by="PATIENT_NUM")
	
	#Create column names. 
	colnames(finalData) <- c('PATIENT_NUM',xFinalColumns,yFinalColumns)	

	##########################################
	
	##########################################
	#Binning Code.
	if(binning.enabled == TRUE)
	{
	
	  #If we are binning and we have a group column, we need to bin on a per group basis.
	  if(binning.variable == "DEP" && "GROUP" %in% colnames(xValueMatrix))
	  {
	    library(plyr)
	    finalData <- ddply(finalData, .(GROUP.1), .fun=BinningFunction,'X',binning.bins = binning.bins,binning.type = binning.type,binning.manual = binning.manual,binning.binrangestring = binning.binrangestring,binning.variabletype = binning.variabletype,continuous.concept = continuous.concept)
	  }
	  else if(binning.variable == "IND" && "GROUP" %in% colnames(yValueMatrix))
	  {
	    library(plyr)
	    finalData <- ddply(finalData, .(GROUP.1), .fun=BinningFunction,'X',binning.bins = binning.bins,binning.type = binning.type,binning.manual = binning.manual,binning.binrangestring = binning.binrangestring,binning.variabletype = binning.variabletype,continuous.concept = continuous.concept)
	  }		
	  else
	  {
	    #Call the function to do our binning.
	    finalData <- BinningFunction(finalData,'X',binning.bins = binning.bins,binning.type = binning.type,binning.manual = binning.manual,binning.binrangestring = binning.binrangestring,binning.variabletype = binning.variabletype,continuous.concept = continuous.concept)		
	  }		
		
	}
	##########################################
	
	##########################################
	
	#Verify that we have data after merging the concepts.
	if(NROW(finalData)==0) stop("||FRIENDLY||R found no data after joining the selected concepts. Please verify that subjects exist that meet your input criteria.")
		
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	#write.matrix(finalData,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
	##########################################
	
	print("-------------------")

}
