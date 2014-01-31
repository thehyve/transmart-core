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
#PCA Loader
###########################################################################

PCA.loader <- function(
input.filename,
output.file ="PCA",
aggregate.probes = FALSE
)
{

	print("-------------------")
	print("LoadPCA.R")
	print("CREATING PCA PLOT")

	library(reshape2)
	library(Cairo)
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))

    if (nrow(mRNAData)<1) stop("Input data is empty. Common causes: either the specified subset has no matching data in the selected node, or the gene/pathway is not present.")

	if (aggregate.probes) {
        # probe aggregation function adapted from dataBuilder.R to heatmap's specific data-formats
        mRNAData <- PCA.probe.aggregation(mRNAData, collapseRow.method = "MaxMean", collapseRow.selectFewestMissing = TRUE)
    }

	mRNAData$PROBE.ID 		<- gsub("^\\s+|\\s+$", "",mRNAData$PROBE.ID)
	mRNAData$GENE_SYMBOL 	<- gsub("^\\s+|\\s+$", "",mRNAData$GENE_SYMBOL)
	mRNAData$PATIENT.ID   	<- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT.ID)

	#Grab only the columns we need for doing the melt/cast.
	mRNAData <- mRNAData[c('PATIENT.ID','VALUE','PROBE.ID','GENE_SYMBOL')]

	#Melt the data, leaving 2 columns as the grouping fields.
	meltedData <- melt(mRNAData, id=c("PROBE.ID","GENE_SYMBOL","PATIENT.ID"))
	
	#Cast the data into a format that puts the PATIENT.ID in a column.
	mRNAData <- data.frame(dcast(meltedData, PATIENT.ID ~ PROBE.ID + GENE_SYMBOL))
  
	#Make the rownames be the patient nums so we can drop the patient_num column.
	rownames(mRNAData) <- mRNAData$PATIENT.ID
  
	print(sprintf("rows %d cols %d", nrow(mRNAData), ncol(mRNAData)))

	#Drop patient_num column.
	mRNAData <- subset(mRNAData, select = -c(PATIENT.ID))

	print(sprintf("rows %d cols %d PATIENT.ID dropped", nrow(mRNAData), ncol(mRNAData)))

	### for poorly curated data, drop columns where there are one or more missing values
	### print(colSums(is.na(mRNAData)))   # print numbers os NA values for each column
	mRNAData <- subset(mRNAData, select = colSums(is.na(mRNAData))<1)

	print(sprintf("rows %d cols %d NA columns dropped", nrow(mRNAData), ncol(mRNAData)))

	###print(mRNAData)

	print("Run the PCA Analysis")
	#Run the PCA Analysis
	pca.results <- prcomp(mRNAData)
	#print("Completed PCA Analysis")
	
	#Get the number of components.
	numberOfComponents <- length(pca.results$sdev)
	
	print(sprintf("Number of components %d", numberOfComponents))
	
	GENELISTLENGTH <- length(pca.results$center)
	if(GENELISTLENGTH > 20) GENELISTLENGTH <- 20

	#Create a data frame with 1 row per component.
	component.summary <- data.frame(paste("PC",1:numberOfComponents,sep=""))
		
	#Create a table with Eigen Value and %Variation.
	component.summary$EIGENVALUE <- round(pca.results$sdev[1:numberOfComponents]**2,5)
	component.summary$PERCENTVARIANCE <- round(pca.results$sdev[1:numberOfComponents]**2 / sum(pca.results$sdev**2) * 100,5)

	colnames(component.summary) <- c('PC','EIGENVALUE','PERCENTVARIANCE')
	
	write.table(component.summary,"COMPONENTS_SUMMARY.TXT",quote=F,sep="\t",row.names=F,col.names=T)
	
	rotationFrame <- data.frame(pca.results$rotation)

	f <- function(i,GENELISTLENGTH) 
	  {
	  
		#Form the file name.
		currentFile <- paste('GENELIST',i,'.TXT',sep="")
		
		#Create the current data frame from the gene names and value columns.
		currentData <- data.frame(rownames(rotationFrame),round(rotationFrame[,i],3))
		
		colnames(currentData) <- c('GENE_SYMBOL','VALUE')
		
		#Reorder the genes based on decreasing absolute value of the value column.
		currentData <- currentData[order(abs(currentData$VALUE),decreasing = TRUE),]
		
		#Pull only the records we are interested in.
		currentData <- currentData[1:GENELISTLENGTH,]
		
		#Write the list to a file.
		write.table(currentData,currentFile,quote=F,sep="\t",row.names=F,col.names=F)

	  }

	sapply(1:ncol(rotationFrame), f, GENELISTLENGTH)
	
	#Finally create the Scree plot.
	CairoPNG(file=paste("PCA.png",sep=""),width=800,height=800)
	
	plot(pca.results,type="lines", main="Scree Plot")
	title(xlab = "Component")
	
	dev.off()
}


PCA.probe.aggregation <- function(mRNAData, collapseRow.method, collapseRow.selectFewestMissing) {
  library(WGCNA)

  # Keeps relevant columns. Throws out SUBSET column, since this is not being used by PCA anyway.
  mRNAData <- mRNAData[,c("PATIENT.ID","VALUE","PROBE.ID","GENE_SYMBOL")]

  #Cast the data into a format that puts the PATIENT_NUM in a column
  castedData <- data.frame(dcast(mRNAData, PROBE.ID + GENE_SYMBOL ~ PATIENT.ID, value.var = "VALUE"))

  #Create a unique identifier column.
  castedData$UNIQUE_ID <- paste(castedData$GENE_SYMBOL,castedData$PROBE.ID,sep="")

  #Set the name of the rows to be the unique ID.
  rownames(castedData) = castedData$UNIQUE_ID

  #Run the collapse on a subset of the data by removing some columns.
  finalData <- collapseRows(subset(castedData, select = -c(GENE_SYMBOL,PROBE.ID,UNIQUE_ID) ),
                            rowGroup = castedData$GENE_SYMBOL,
                            rowID = castedData$UNIQUE_ID,
                            method = collapseRow.method,
                            connectivityBasedCollapsing = TRUE,
                            methodFunction = NULL,
                            connectivityPower = 1,
                            selectFewestMissing = collapseRow.selectFewestMissing,
                            thresholdCombine = NA)

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
  colnames(finalData) <- c("GENE_SYMBOL","PROBE.ID","PATIENT.ID","VALUE")

  #When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
  finalData$PATIENT.ID <- sub("^X","",finalData$PATIENT.ID)
  finalData
}
