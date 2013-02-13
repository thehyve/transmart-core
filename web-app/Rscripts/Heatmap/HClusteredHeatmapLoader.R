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

HClusteredHeatmap.loader <- function(
input.filename,
output.file ="Heatmap",
imageWidth = 1200,
imageHeight = 800,
pointsize = 15
)
{

	library(Cairo)
	library(ggplot2)
	library(gplots)
	library(reshape2)
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))

	#Trim the patient.id field.
	mRNAData$PATIENT_NUM <- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT_NUM)
	
	#Melt the data, leaving 3 columns as the grouping fields.
	meltedData <- melt(mRNAData, id=c("GROUP","PATIENT_NUM"))

	#Cast the data into a format that puts the ASSAY.ID in a column.
	castedData <- data.frame(dcast(meltedData, GROUP ~ PATIENT_NUM))

	#Set the name of the rows to be the names of the probes.
	rownames(castedData) = castedData$GROUP

	#When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
	colnames(castedData) <- sub("^X","",colnames(castedData))	
	
	#Convert data to a integer matrix.
	matrixData <- data.matrix(subset(castedData, select = -c(GROUP)))
	
	#We can't draw a heatmap for a matrix with only 1 row.
	if(nrow(matrixData)<2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Gene/Probe. Please check your variable selection and run again.")
	if(ncol(matrixData)<2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Patient data. Please check your variable selection and run again.")
	
	#Prepare the package to capture the image file.
	CairoPNG(file=paste(output.file,".png",sep=""),width=as.numeric(imageWidth),height=as.numeric(imageHeight),pointsize=as.numeric(pointsize))
	
	colorPanelList <- colorpanel(100,low="green",mid="black",high="red")
	
	#Store the heatmap in a temp variable.
	tmp <- heatmap(	matrixData,
					col=colorPanelList,
					margins=c(25,25),
					cexRow=1.5,
					cexCol=1.5)		
		
	#Print the heatmap to an image
	print (tmp)		
	
	dev.off()
}
