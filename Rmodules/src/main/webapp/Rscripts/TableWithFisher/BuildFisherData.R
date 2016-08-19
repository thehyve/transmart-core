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
#BuildFisherData
#Parse the i2b2 output file and create input files for the Fisher Table analysis.
###########################################################################

FisherData.build <- 
function
(
input.dataFile,
output.dataFile="outputfile",
concept.dependent,
concept.independent,
binning.Dep.enabled=FALSE,
binning.Indep.enabled=FALSE,
binning.Dep.bins='',
binning.Indep.bins='',
binning.Dep.type='',
binning.Indep.type='',
binning.Dep.manual=FALSE,
binning.Indep.manual=FALSE,
binning.Dep.binrangestring='',
binning.Indep.binrangestring='',
binning.Dep.variabletype='',
binning.Indep.variabletype='',
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
snptype.independent = ''
)
{
	print("-------------------")
	print("BuildFisherData.R")
	print("BUILDING FISHER DATA")
	
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
	#Get a table of X/Y value/Patient
	fullconcept.indep <- FALSE
	fullconcept.dep <- FALSE
	
	if(binning.Indep.manual == TRUE && binning.Indep.variabletype == "Categorical") fullconcept.indep <- TRUE
	if(binning.Dep.manual == TRUE && binning.Dep.variabletype == "Categorical") fullconcept.dep <- TRUE
	
	yValueMatrix <- dataBuilder( 	splitData = splitData,
								concept = concept.dependent,
								concept.type = concept.dependent.type,
								sampleType = sample.dependent,
								timepointType = time.dependent,
								tissueType = tissues.dependent,
								GEXFile = input.gexFile,
								gene.list = genes.dependent,
								gene.aggregate = genes.dependent.aggregate,
								SNPFile = input.snpFile,
								SNPType = snptype.dependent,
								fullConcept = fullconcept.dep);
									
	xValueMatrix <- dataBuilder(	splitData = splitData,
								concept = concept.independent,
								concept.type = concept.independent.type,
								sampleType = sample.independent,
								timepointType = time.independent,
								tissueType = tissues.independent,
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
	
	#Merge our X and patient nums.
	finalData<-merge(finalData,xValueMatrix[xMatrixColumns],by="PATIENT_NUM")
	#Merge our Y and final data.
	finalData<-merge(finalData,yValueMatrix[yMatrixColumns],by="PATIENT_NUM")
	
	#Create column names.
	colnames(finalData) <- c('PATIENT_NUM',xFinalColumns,yFinalColumns)	

	#We need to call the binning function if it was enabled.
	if(binning.Dep.enabled)
	{
		if("GROUP" %in% colnames(yValueMatrix))
		{
			library(plyr)
			finalData <- ddply(finalData, .(GROUP.1), .fun=BinningFunction,'Y',binning.bins = binning.Dep.bins,binning.type = binning.Dep.type,binning.manual = binning.Dep.manual,binning.binrangestring = binning.Dep.binrangestring,binning.variabletype = binning.Dep.variabletype,continuous.concept = concept.dependent)
		}
		else
		{	
			finalData = BinningFunction(finalData,'Y',binning.Dep.bins,binning.Dep.type,binning.Dep.manual,binning.Dep.binrangestring,binning.Dep.variabletype,concept.dependent)
		}
	}
	
	#We need to call the binning function if it was enabled.
	if(binning.Indep.enabled)
	{
		if("GROUP" %in% colnames(xValueMatrix))
		{
			library(plyr)
			finalData <- ddply(finalData, .(GROUP), .fun=BinningFunction,'X',binning.bins = binning.Indep.bins,binning.type = binning.Indep.type,binning.manual = binning.Indep.manual,binning.binrangestring = binning.Indep.binrangestring,binning.variabletype = binning.Indep.variabletype,continuous.concept = concept.independent)
		}
		else
		{
			finalData = BinningFunction(finalData,'X',binning.Indep.bins,binning.Indep.type,binning.Indep.manual,binning.Indep.binrangestring,binning.Indep.variabletype,concept.independent)
		}
	}	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Write the final data file.
	#write.matrix(finalData,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
    print("-------------------")
	##########################################
}


