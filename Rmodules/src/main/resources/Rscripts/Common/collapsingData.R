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

#This code will set the GEX up to run the collapse rows algorithm on it.

collapsingGEXData <- 
function
(
	GEXData,
	valueColumn = "ZSCORE"
)
{
	library(WGCNA)
	library(reshape2)
	
	#We only need specific columns from our Gene Expression Data.
	GEXDataToCollapse <- GEXData[c("GENE_SYMBOL",valueColumn,"PROBE.ID","ASSAY.ID")]
	
	#Melt the data, leaving 3 columns as the grouping fields.
	meltedData <- melt(GEXDataToCollapse, id=c("PROBE.ID","GENE_SYMBOL","ASSAY.ID"))
	
	#Cast the data into a format that puts the ASSAY.ID in a column.
	castedData <- data.frame(dcast(meltedData, PROBE.ID + GENE_SYMBOL ~ ASSAY.ID))
	
	#Create a unique identifier column.
	castedData$UNIQUE_ID <- paste(castedData$GENE_SYMBOL,castedData$PROBE.ID,sep="")
	
	#Set the name of the rows to be the unique ID.
	rownames(castedData) = castedData$UNIQUE_ID

	#Run the collapse on a subset of the data by removing some columns.
	finalData <- collapseRows(subset(castedData, select = -c(GENE_SYMBOL,PROBE.ID,UNIQUE_ID) ),
				  rowGroup=castedData$GENE_SYMBOL,
				  rowID=castedData$UNIQUE_ID,
				  method="absMaxMean", 
				  connectivityBasedCollapsing=TRUE,
				  methodFunction=NULL, 
				  connectivityPower=1,
				  selectFewestMissing=TRUE, 
				  thresholdCombine=NA)
	
	#Coerce the data into a data frame.
	finalData=data.frame(finalData$group2row, finalData$datETcollapsed)
	
	#Rename the columns, the selected row_id is the unique_id.
	colnames(finalData)[2] <- 'UNIQUE_ID'
		
	#Merge the probe.id back in.
	finalData <- merge(finalData,castedData[c('UNIQUE_ID','PROBE.ID')],by=c('UNIQUE_ID'))
	
	#Remove the unique_id and selected row ID column.
	finalData <- subset(finalData, select = -c(UNIQUE_ID))
	
	#Melt the data back.
	finalData <- melt(finalData)
	
	#Set the column names again.
	colnames(finalData) <- c("GENE_SYMBOL","PROBE.ID","ASSAY.ID",valueColumn)
	
	#When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
	finalData$ASSAY.ID <- sub("^X","",finalData$ASSAY.ID)
	
	finalData
	
}

collapsingSNPCNVData <- 
function
(
	SNPData
)
{
	library(WGCNA)

	#We only need specific columns from our Gene Expression Data.
	SNPDataToCollapse <- SNPData[c("GENE","COPYNUMBER","PATIENT.ID","PROBE.ID")]
	
	#Melt the data, leaving 3 columns as the grouping fields.
	meltedData <- melt(SNPDataToCollapse, id=c("PROBE.ID","GENE","PATIENT.ID"))
	
	#Cast the data into a format that puts the ASSAY.ID in a column.
	castedData <- data.frame(cast(meltedData, PROBE.ID + GENE ~ PATIENT.ID))
	
	#Set the name of the rows to be the names of the probes.
	rownames(castedData) = castedData$PROBE.ID

	#Run the collapse on a subset of the data by removing some columns.
	finalData <- collapseRows(subset(castedData, select = -c(GENE,PROBE.ID) ),
				  rowGroup=castedData$GENE,
				  rowID=castedData$PROBE.ID,
				  method="absMaxMean", 
				  connectivityBasedCollapsing=TRUE,
				  methodFunction=NULL, 
				  connectivityPower=1,
				  selectFewestMissing=TRUE, 
				  thresholdCombine=NA)

	#Coerce the data into a data frame.
	finalData=data.frame(finalData$group2row, finalData$datETcollapsed)
	
	#Melt the data back.
	finalData <- melt(finalData)
	
	#Set the column names again.
	colnames(finalData) <- c("GENE","PROBE.ID","PATIENT.ID","COPYNUMBER")
	
	#When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
	finalData$PATIENT.ID <- sub("^X","",finalData$PATIENT.ID)
	
	finalData
	
}