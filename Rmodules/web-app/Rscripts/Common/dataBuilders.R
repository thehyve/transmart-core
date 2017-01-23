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
#dataBuilders
#These functions direct how data from the different file types (Clinical,GEX,SNP) should extracted.
###########################################################################


#########################
#PARENT FUNCTIONS
#These functions call the specific data extraction functions.
#########################
dataBuilder <- 
function
(
splitData,
concept,
concept.type,
sampleType = '',
timepointType = '',
tissueType = '',
platform.type = '',
GEXFile = '',
gene.list = '',
gene.aggregate,
SNPFile = '',
SNPType = '',
fullConcept = FALSE,
conceptColumn = FALSE,
encounterColumn = FALSE
)
{
	print("     -------------------")
	print(Sys.time())
	print("     dataBuilders.R")
	sprintf("     BUILDING DATA: %s", concept.type)
	
	#Depending on the type of data we are using we use a different extraction method.
	if(concept.type == "CLINICAL")
	{
		#Extract the concepts from the clinical data file.
		axisValueMatrix <- extractConcepts(splitData,concept,fullConcept,conceptColumn=conceptColumn)
	}
	else if(concept.type == "MRNA")
	{
		axisValueMatrix <- gexBuilder(	GEXFile = GEXFile,
						sampleType = sampleType,
						timepointType = timepointType,
						tissueType = tissueType,
						platform.type = platform.type,
						gene.list = gene.list,
						gene.aggregate = gene.aggregate)
	}
	else if(concept.type == "SNP")
	{
		#Pull the data from the SNP file.
		snpData <- data.frame(read.delim(SNPFile));
		
		#PATIENT.ID,GENE,PROBE.ID,GENOTYPE,COPYNUMBER,SAMPLE,TIMEPOINT,TISSUE,SEARCH_ID
		snpData <- filterData(snpData,gene.list,sampleType,timepointType,tissueType,platform.type)
		
		if(SNPType == "CNV")
		{
			
			#If we need to aggregate the probes, do so here. Check to make sure we actually need to aggregate. CHANGE THIS.
			if((gene.aggregate == TRUE || gene.aggregate == 'true') && (length(levels(snpData$PROBE.ID)) > 1))
			{
				#Pass the complete set of data to the function that collapses the data.			
				collapsedData <- collapsingSNPCNVData(snpData)
				
				#We need to merge the collapsed data with the original data set to get the patient_ids back.
				axisValueMatrix <- merge(snpData,collapsedData,by=c("PROBE.ID","PATIENT.ID"))
				
				#Pull the columns we are interested in out of the merged data.
				axisValueMatrix <- axisValueMatrix[c("PATIENT.ID","COPYNUMBER.x","GENE.x")]
			}
			else
			{
				#Extract the patient_num and value.
				axisValueMatrix <- snpData[c("PATIENT.ID","COPYNUMBER","PROBE.ID")]	
			}		
		}
		else
		{
			#Add the No Call level to the factor.
			snpData$GENOTYPE <- factor(snpData$GENOTYPE, levels = c(levels(snpData$GENOTYPE), "No Call"))
			
			#Replace the 0 0 text with No Call.
			snpData$GENOTYPE[gsub("(^ +)|( +$)", "",snpData$GENOTYPE) == "0 0"] <- 'No Call'
			
			#Extract the patiensnpData$GENOTYPE[gsub("(^ +)|( +$)", "",snpData$GENOTYPE) == "A A"]t_num and value.
			axisValueMatrix <- snpData[c("PATIENT.ID","GENOTYPE","PROBE.ID")]
		}
		
		colnames(axisValueMatrix) <- c('PATIENT_NUM','VALUE','GROUP')
	}
	print("     -------------------")
	
	return(axisValueMatrix)
}



#Function to put common gene expression file conversions in the same place.
gexBuilder <- 
function
(
GEXFile = '',
sampleType = '',
timepointType = '',
tissueType = '',
platform.type = '',
gene.list = '',
gene.aggregate,
probe.average = FALSE,
subsetname.replace = FALSE,
data.reduce = TRUE,
GEXData = ''
)
{
	if(GEXFile != '')
	{
		#Pull the data from the MRNA file.
		mrnaData <- data.frame(read.delim(GEXFile));
	}
	else
	{
		#Use the data that was passed in.
		mrnaData <- GEXData
	}
	
	#This tells us which column to use for the value when collapsing.
	columnToCollapse <- "ZSCORE"
	
	#Filter the data based on the gene,sample,timepoint and tissue type.
	mrnaData <- filterData(dataToFilter = mrnaData,
							gene.list = gene.list,
							sampleType = sampleType,
							timepointType = timepointType,
							tissueType = tissueType,
							platform.type = platform.type)
	
	#If we need to put the subset column into the patient.id, do it here.
	if(subsetname.replace)
	{
		mrnaData <- formSubsetNames(mrnaData)
	}

	#If we need to average the probes, do so here.
	if(probe.average)
	{
		mrnaData <- probeAverage(mrnaData)
		columnToCollapse <- "ZSCORE"
	}
	
	#If we reduce the number of columns in the data, do it here.
	if(data.reduce == TRUE)
	{
	
		#If we need to aggregate the probes, do so here. Check to make sure we actually need to aggregate. CHANGE THIS.
		if((gene.aggregate == TRUE || gene.aggregate == 'true') && (length(levels(mrnaData$PROBE.ID)) > 1))
		{
			#Pass the complete set of data to the function that collapses the data.			
			collapsedData <- collapsingGEXData(mrnaData,columnToCollapse)

			#We need to merge the collapsed data with the original data set to get the patient_ids back.
			mrnaData <- merge(mrnaData,collapsedData,by=c("PROBE.ID","ASSAY.ID","GENE_SYMBOL"))

			#Pull the columns we are interested in out of the merged data.
			mrnaData <- mrnaData[c("PATIENT.ID",paste(columnToCollapse,".x",sep=""),"GENE_SYMBOL")]
			
		}
		else
		{
			#If we are reducing data and not aggregating probes, we need to add the gene name to the probe.id.
			mrnaData$PROBE_GENE <- paste(mrnaData$PROBE.ID,mrnaData$GENE_SYMBOL,sep="_")
		
			#Extract the patient_num and value.
			mrnaData <- mrnaData[c("PATIENT.ID",columnToCollapse,"PROBE_GENE")]	
			
		}

		colnames(mrnaData) <- c('PATIENT_NUM','VALUE','GROUP')
	
		return(mrnaData)
	}
	
	return(mrnaData)
}

gexSubsetBuilder <- 
function
(
	GEXFile = '',
	sample.subset1,
	time.subset1,
	tissues.subset1,
	platform.subset1,
	sample.subset2,
	time.subset2,
	tissues.subset2,
	platform.subset2,
	gene.list = '',
	gene.aggregate
)
{
	allGEXData <- data.frame(read.delim(GEXFile));
	
	#We have two subsets that we have to filter on using the passed in criteria.
	subset1 <- allGEXData[grep("^subset1",allGEXData$SUBSET),]
	subset2 <- allGEXData[grep("^subset2",allGEXData$SUBSET),]	
	
	subset1 <- gexBuilder(
				GEXData = subset1,
				sampleType = sample.subset1,
				timepointType = time.subset1,
				tissueType = tissues.subset1,
				platform.type = platform.subset1,
				gene.list = gene.list,
				gene.aggregate = gene.aggregate,
				probe.average = TRUE,
				subsetname.replace = TRUE,
				data.reduce = FALSE);
				
	if(nrow(subset2) > 0)
	{				
		subset2 <- gexBuilder(
					GEXData = subset2,
					sampleType = sample.subset2,
					timepointType = time.subset2,
					tissueType = tissues.subset2,
					platform.type = platform.subset2,
					gene.list = gene.list,
					gene.aggregate = gene.aggregate,
					probe.average = TRUE,
					subsetname.replace = TRUE,
					data.reduce = FALSE);				
		
		geneExpressionMatrix <- rbind(subset1,subset2)
	}
	else
	{
		geneExpressionMatrix <- subset1
	}
	
	return(geneExpressionMatrix)
}
#########################


#########################
#CHILD FUNCTIONS
#These functions do the actual work of filtering/reforming the data.
#########################

#Add the subset to the patient_id column.
formSubsetNames <- 
function
(
	mrnaData
)
{
	mrnaData[,"PATIENT.ID"] <- as.character(mrnaData[,"PATIENT.ID"])
	
	mrnaData[grep("^subset1",mrnaData$SUBSET),c('PATIENT.ID')] <- paste('S1',as.character(mrnaData[grep("^subset1",mrnaData$SUBSET),c('PATIENT.ID')]),sep="_")
	mrnaData[grep("^subset2",mrnaData$SUBSET),c('PATIENT.ID')] <- paste('S2',as.character(mrnaData[grep("^subset2",mrnaData$SUBSET),c('PATIENT.ID')]),sep="_")

	return(mrnaData)
}

#This averages the probes. We shouldn't ever have to average the probes but for testing purposes we don't have the data to differentiate the samples.
probeAverage <-
function
(
	mrnaData
)
{

	library(data.table)

	#Convert to a data.table to do the aggregation.
	mrnaData <- data.table(mrnaData)
	
	#Aggregate and return the mean expression value and max probe id.
	mrnaData <- mrnaData[,list(ZSCORE = median(ZSCORE),ASSAY.ID = max(ASSAY.ID)), by = "PATIENT.ID,PROBE.ID,GENE_SYMBOL,SUBSET"]
	
	#Convert back to a data.frame.
	mrnaData <- data.frame(mrnaData)
	
	return(mrnaData)

}

#Filter gene expression data based on gene, sample, timepoint and tissue fields.
filterData <- 
function
(
dataToFilter,
gene.list,
sampleType,
timepointType,
tissueType,
platform.type
)
{
	
	#For each of the possible filters need to turn them into a list (Assuming they are seperated by ","s).
	if(gene.list != '') 		gene.list 		<- unlist(strsplit(gene.list,','))
	if(sampleType != '') 		sampleType 		<- unlist(strsplit(sampleType,','))
	if(timepointType != '') 	timepointType 	<- unlist(strsplit(timepointType,','))
	if(tissueType != '') 		tissueType 		<- unlist(strsplit(tissueType,','))
	if(platform.type != '') 	platform.type 	<- unlist(strsplit(platform.type,','))

	#If we have a filter criteria, filter on it.
	if(gene.list != '' && gene.list !='ALL'){			dataToFilter <- dataToFilter[which(dataToFilter[["SEARCH_ID"]] 		%in% gene.list),]}
	if(sampleType != '' && sampleType !='ALL'){			dataToFilter <- dataToFilter[which(dataToFilter[["SAMPLE.TYPE"]] 	%in% sampleType),]}
	if(timepointType != '' && timepointType !='ALL'){	dataToFilter <- dataToFilter[which(dataToFilter[["TIMEPOINT"]] 		%in% timepointType),]}		
	if(tissueType != '' && tissueType !='ALL'){			dataToFilter <- dataToFilter[which(dataToFilter[["TISSUE.TYPE"]] 	%in% tissueType),]}		
	if(platform.type != '' && platform.type !='ALL'){	dataToFilter <- dataToFilter[which(dataToFilter[["GPL.ID"]] 		%in% platform.type),]}		
	
	return(dataToFilter)
}
#########################
#This is a conveniance method that will return the list of columns in the clinical data export file. Keeping it in one place will prevent us from having to changing all the workflows when we add a column.
defaultColumnList <-  function()
{
	#return(c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH","ENCOUNTER_NUM","LINK_TYPE"))
	return(c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH"))
}
