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
#Clustered Heatmap Loader
###########################################################################

KMeansHeatmap.loader <- function(
input.filename,
output.file ="Heatmap",
clusters.number = 2,
probes.aggregate = false,
imageWidth = 1200,
imageHeight = 800,
pointsize = 15,
maxDrawNumber = 50
)
{
	print("-------------------")
	print("KMeansHeatmap.R")
	print("CREATING K-MEANS HEATMAP")

	library(Cairo)
	library(reshape2)
	library(gplots)
	
	#Validate the number of clusters after converting to a numeric.
	clusters.number <- as.numeric(clusters.number)	
	
	if(is.na(clusters.number)) stop("||FRIENDLY||The number of clusters supplied was invalid.")
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))

	#Trim the patient.id field.
	mRNAData$PATIENT_NUM <- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT_NUM)	
	
	#Trim the Gene/Probe field.
	mRNAData$GROUP <- sub("^\\s+|\\s+$", "",mRNAData$GROUP)
	
	#Melt the data, leaving 3 columns as the grouping fields.
	meltedData <- melt(mRNAData, id=c("GROUP","PATIENT_NUM"))

	#Cast the data into a format that puts the ASSAY.ID in a column.
	castedData <- data.frame(dcast(meltedData, GROUP ~ PATIENT_NUM))

	#Set the name of the rows to be the names of the probes.
	rownames(castedData) = castedData$GROUP
	
	#When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
	colnames(castedData) <- sub("^X","",colnames(castedData))	
	
	#Convert data to an integer matrix.
	print("Convert data to an integer matrix")
	matrixData <- data.matrix(subset(castedData, select = -c(GROUP)))

# by Serge and Wei to filter a sub set and reorder markers

        matrixData <- matrixData[!apply(is.na(matrixData),1,any), ]				# remove rows with NA

        num_markers<-dim(matrixData)[1]                                                         # number of markers in the dataset
        if (num_markers > maxDrawNumber) {                                                      # if more markers in the dataset, apply filter
                sd_rows_matrix<-apply (matrixData,1,sd,na.rm=T)
                matrixData<-matrixData[!is.na(sd_rows_matrix),]                                 # remove markers where sd is NA
                sd_rows_matrix<-sd_rows_matrix[!is.na(sd_rows_matrix)]
                cutoff_sd<- sd_rows_matrix[order(sd_rows_matrix,decreasing = T)][maxDrawNumber+1] # filter by SD, draw only the top maxDrawNumber
                matrixData<-matrixData[sd_rows_matrix>cutoff_sd,]
        }

	#Transpose the matrix to put the sample column into a row.
	print("Transpose the matrix to put the sample column into a row")
	transposedMatrixData <- t(matrixData)
	
	#Make sure the number of clusters is applicable.
	if(clusters.number >= nrow(transposedMatrixData)) stop(paste("||FRIENDLY||The number of clusters must fall between 1 and the number of subjects in your data. Your data only has ",as.character(nrow(transposedMatrixData))," subjects"))
	
	#Create the kmeans object. We cluster by columns.
	print("Create the kmeans object. We cluster by columns")
	kMeansObject <- kmeans(transposedMatrixData,centers=clusters.number)
	
	#We want to merge the cluster names back into our data set.
	print("Merge the cluster names back into our data set")
	dataWithCluster <- data.frame(transposedMatrixData,cluster=kMeansObject$cluster)
	
	#Order the data on the cluster column.
	print("Order the data on the cluster column")
	dataWithCluster <- dataWithCluster[order(dataWithCluster$cluster),]
	
	#Create a function that will help us decide which color to draw above the heatmap.
	print("Create a function that will help us decide which color to draw above the heatmap")
	color.map <- function(clusterNumber) { if (clusterNumber %% 2 == 0 ) "#8B8989" else "#5C3317" }
	
	#Use the function to create a list with a color for each patient.
	print("Use the function to create a list with a color for each patient")
	patientcolors <- unlist(lapply(dataWithCluster$cluster, color.map))
	
	#Remove the cluster column.
	print("Remove the cluster column")
	dataWithCluster <- subset(dataWithCluster, select = -c(cluster))	
	
	#Transpose the data back.
	print("Transpose the data back")
	dataWithCluster <- t(dataWithCluster)
	
	#If we didn't aggregate the probes the rownames are probe IDs, which are usually numeric. Strip the X from them here.
	rownames(dataWithCluster) <- sub("^X","",rownames(dataWithCluster))	
	
	#Push the data back to a matrix so we can plot it.
	print("Push the data back to a matrix so we can plot it")
	matrixData <- data.matrix(dataWithCluster)
	
	#We can't draw a heatmap for a matrix with only 1 row.
	if(nrow(matrixData)<2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Gene/Probe. Please check your variable selection and run again.")
	if(ncol(matrixData)<2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Patient data. Please check your variable selection and run again.")

	#Prepare the package to capture the image file.
	CairoPNG(file=paste(output.file,".png",sep=""),width=as.numeric(imageWidth),height=as.numeric(imageHeight),pointsize=as.numeric(pointsize))	
	
 	colorPanelList <- colorpanel(100,low="green",mid="black",high="red")

	#Store the heatmap in a temp variable.
	print("Create the heatmap")
	tmp <- heatmap(	matrixData,
					Rowv=NA,
					Colv=NA,col=colorPanelList,
					ColSideColors=patientcolors,
					margins=c(25,25),			
					cexRow=1.5,
					cexCol=1.5)		
	
	#Print the heatmap to an image
	print("Print the heatmap to an image")
	print (tmp)		
	
	dev.off()
}

ClusteredHeatmap.loader.single <- function(heatmapdata)
{
	#Convert data to an integer matrix.
	matrixData <- data.matrix(subset(heatmapdata, select = -c(GROUP,kMeansObject.cluster)))

	#Store the heatmap in a temp variable.
	tmp <- heatmap(matrixData,Rowv=NA,Colv=NA,col=redgreen(100))		
	
	#Print the heatmap to an image
	print (tmp)	
	
	#Set up a matrix of plots, 1 column, as long as the number of clusters.
	par(mfrow=c(1,clusters.number))
	
	#For each of the clusters we draw a heatmap.
	lapply(split(dataWithCluster, dataWithCluster$kMeansObject.cluster), ClusteredHeatmap.loader.single)
	
}


