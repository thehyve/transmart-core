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
meltData = TRUE,
imageWidth = 1200,
imageHeight = 800,
pointsize = 15,
maxDrawNumber = 50
)
{

	print("-------------------")
	print("HClusteredHeatmapLoader.R")
	print("CREATING HCLUSTERED HEATMAP")

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
	
# by Serge and Wei to filter a sub set and reorder markers

        mRNAData <- mRNAData[!apply(is.na(mRNAData),1,any), ]					# remove rows with NA

        num_markers<-dim(mRNAData)[1]                                                           # number of markers in the dataset
        if (num_markers > maxDrawNumber) {                                                      # if more markers in the dataset, apply filter
                sd_rows_mRNA<-apply (mRNAData,1,sd,na.rm=T)
                mRNAData<-mRNAData[!is.na(sd_rows_mRNA),]                                       # remove markers where sd is NA
                sd_rows_mRNA<-sd_rows_mRNA[!is.na(sd_rows_mRNA)]
                cutoff_sd<- sd_rows_mRNA[order(sd_rows_mRNA,decreasing = T)][maxDrawNumber+1]   # filter by SD, draw only the top maxDrawNumber
                mRNAData<-mRNAData[sd_rows_mRNA>cutoff_sd,]
        }

        colcolor<-colnames(mRNAData)                                                            # assign colors for different subset
        colcolor[grep("S1",colnames(mRNAData))]<-"orange"
        colcolor[grep("S2",colnames(mRNAData))]<-"yellow"

        mean_reorder<-rowMeans(mRNAData[,colcolor=="orange" ], na.rm = T)                       # reorder the data by rowmean of Subset 1
        mRNAData<-mRNAData[order(mean_reorder,decreasing = T),]
        rownames(mRNAData)<-gsub("_\\s+$","",rownames(mRNAData), ignore.case = FALSE, perl = T) # remove the _ at the end of the marker label

# end filter subset

# check whether there is enough data to draw heatmap
        n_remaining_marker<-nrow(mRNAData)
        n_remaining_sample<-ncol(mRNAData)
        if (n_remaining_marker>1 & n_remaining_sample >1) {

        #Store the heatmap in a temp variable.
	       tmp <- heatmap(
                              mRNAData,
# by Serge and Wei to make the map more informative
               	             ColSideColors=colcolor,
#                  	     col=cm.colors(800),
                       	     col=greenred(800),
                             margins=c(5,5),
                             labCol=NA,
                             )

   # add a legend to heatmap.
	       tmp_legend <- legend("topleft",
               		           legend = c("Subset 1","Subset 2"),
                        	   fill = c("orange","yellow"),
                        	   bg = "white", ncol = 1,
 				   cex=1.2,
				   )
# end map informative

	       print("Generating heatmap")

	#Print the heatmap to an image
	       print (tmp)		
	
        # by Serge and Wei to print out the legend
	       print (tmp_legend)

        } else {
	       tmp<-frame()
	       tmp2<-mtext ("not enough marker/samples to draw heatmap",cex=2)
	       print (tmp)
	       print (tmp2)
        }

	dev.off()
	print("-------------------")
}
