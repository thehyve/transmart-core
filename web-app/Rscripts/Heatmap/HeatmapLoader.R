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
#Heatmap Loader
###########################################################################

Heatmap.loader <- function(
input.filename,
output.file ="Heatmap",
meltData = TRUE,
imageWidth = 1200,
imageHeight = 800,
pointsize = 15
)
{

	print("-------------------")
	print("HeatmapLoader.R")
	print("CREATING HEATMAP")

	library(Cairo)
	library(ggplot2)
	library(reshape2)
	library(gplots)
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))
		
	#If we have to melt and cast, do it here, otherwise we make the group column the rownames
	if(meltData == TRUE)
	{
		#Trim the patient.id field.
		mRNAData$PATIENT_NUM <- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT_NUM)
	
		#Melt the data, leaving 3 columns as the grouping fields.
		meltedData <- melt(mRNAData, id=c("GROUP","PATIENT_NUM"))

		#Cast the data into a format that puts the ASSAY.ID in a column.
		mRNAData <- data.frame(dcast(meltedData, GROUP ~ PATIENT_NUM))
	  
		#When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
		colnames(mRNAData) <- sub("^X","",colnames(mRNAData))
	}
	else
	{	
		colnames(mRNAData) <- gsub("^\\s+|\\s+$","",colnames(mRNAData))
		
		#Use only unique row names. This unique should get rid of the case where we have multiple genes per probe. The values for the probes are all the same.
		mRNAData <- unique(mRNAData)
		
	}
	
	#Set the name of the rows to be the names of the probes.
	rownames(mRNAData) = mRNAData$GROUP	  
  
	#Convert data to a integer matrix.
	mRNAData <- data.matrix(subset(mRNAData, select = -c(GROUP)))	
	
	#We can't draw a heatmap for a matrix with only 1 row.
	if(nrow(mRNAData)<2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Gene/Probe. Please check your variable selection and run again.")
	if(ncol(mRNAData)<2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Patient data. Please check your variable selection and run again.")
	
	#Prepare the package to capture the image file.
	CairoPNG(file=paste(output.file,".png",sep=""),width=as.numeric(imageWidth),height=as.numeric(imageHeight),pointsize=as.numeric(pointsize))
	
	colorPanelList <- colorpanel(100,low="green",mid="black",high="red")
	
	#Store the heatmap in a temp variable.
	tmp <- heatmap(
			mRNAData,
			Rowv=NA,
			Colv=NA,
			col=colorPanelList,
			margins=c(25,25),
			cexRow=1.5,
			cexCol=1.5
			)

	
	#Print the heatmap to an image
	print (tmp)		
	
	dev.off()
	print("-------------------")
}
