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
#BuildSurvivalDataFile
#Parse the i2b2 output file and create input files for Cox/Survival Curve.
###########################################################################

SurvivalData.build <- 
function
(
input.dataFile,
output.dataFile="input",
concept.time,
concept.category = "",
concept.eventYes = "",
binning.enabled = FALSE,
binning.bins = "",
binning.type = "",
binning.manual = FALSE,
binning.binrangestring = "",
binning.variabletype = "",
input.gexFile = '',
input.snpFile = '',
concept.category.type = "CLINICAL",
genes.category = '',
genes.category.aggregate = FALSE,
sample.category = '',
time.category = '',
tissues.category = '',
gpl.category = '',
snptype.category = ''
)
{
	print("-------------------")
	print("BuildSurvivalData.R")
	print("BUILDING SURVIVAL DATA")
	
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
	finalData<-merge(finalData,splitData[[concept.time]][c('PATIENT_NUM','VALUE')],by="PATIENT_NUM")
	
	#If no event was selected, we consider everyone to have had the event.
	if(concept.eventYes=="")
	{
		finalData<-cbind(finalData,1)
	}
	else
	{
		#We merge the Yes events in, everything else gets set to NA. We will mark them as censored later.
		finalData<-merge(finalData,splitData[[concept.eventYes]][c('PATIENT_NUM','VALUE')],by="PATIENT_NUM",all.x=TRUE)	
	}
	
	#If no group was selected, everyone is put in the same category.
	if(concept.category=="")
	{
		finalData<-cbind(finalData,"STUDY")
	}
	else
	{
	
		fullConcept <- binning.manual == TRUE && binning.variabletype == "Categorical"
		
		categoryValueMatrix <- dataBuilder(	splitData = splitData,
											concept = concept.category,
											concept.type = concept.category.type,
											sampleType = sample.category,
											timepointType = time.category,
											tissueType = tissues.category,
											platform.type = gpl.category,
											GEXFile = input.gexFile,
											gene.list = genes.category,
											gene.aggregate = genes.category.aggregate,
											SNPFile = input.snpFile,
											SNPType = input.snpFile,fullConcept);
		
		categoryColumnList <- c('PATIENT_NUM','VALUE')
		
		#If the category matrix has a group column we need to add it to the final column list.
		if("GROUP" %in% colnames(categoryValueMatrix)) categoryColumnList <- c(categoryColumnList,'GROUP')
		
		#Merge the new category column into our final data matrix.
		finalData<-merge(finalData,categoryValueMatrix[categoryColumnList],by="PATIENT_NUM")		
	}
	
	#This is the list of columns for the final data object.
	finalColumnNames <- c("PATIENT_NUM","TIME","CENSOR","CATEGORY")
	
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
	
	#Binning Code.
	if(binning.enabled == TRUE)
	{
		#If we have a group column we need to do a per group bin.
		if("GROUP" %in% colnames(finalData))
		{
			library(plyr)
			finalData <- ddply(finalData, .(GROUP), .fun=BinningFunction,'CATEGORY',binning.bins = binning.bins,binning.type = binning.type,binning.manual = binning.manual,binning.binrangestring = binning.binrangestring,binning.variabletype = binning.variabletype,continuous.concept = concept.category)
		}
		else
		{
			#Call the function to do our binning.
			finalData <- BinningFunction(finalData,'CATEGORY',binning.bins = binning.bins,binning.type = binning.type,binning.manual = binning.manual,binning.binrangestring = binning.binrangestring,binning.variabletype = binning.variabletype,continuous.concept = concept.category)
		}
	}
	###################################	
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	#write.matrix(finalData,"outputfile",sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
    print("-------------------")
}
