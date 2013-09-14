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
output.file ="PCA"
)
{

	print("-------------------")
	print("LoadPCA.R")
	print("CREATING PCA PLOT")

	library(reshape2)
	library(Cairo)
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))

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