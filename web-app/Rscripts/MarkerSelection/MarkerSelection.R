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
#Comparative Marker Selection
###########################################################################

MS.loader <- function(
input.filename,
output.file ="CMS.TXT",
numberOfPermutations = 5000,
numberOfMarkers = 100
)
{
	##########################################
	print("-------------------")
	print("CMS.R")
	print("CREATING CMS")
	
	#We need this to do the ddply below.
	library(plyr)
	library(multtest)
	library(reshape2)
	
	#---------------------
	#Prepare raw data.
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))

	#Trim the probe.id field.
	mRNAData$PROBE.ID 		<- gsub("^\\s+|\\s+$", "",mRNAData$PROBE.ID)
	mRNAData$GENE_SYMBOL 	<- gsub("^\\s+|\\s+$", "",mRNAData$GENE_SYMBOL)
	mRNAData$PATIENT.ID   	<- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT.ID)

	#Create a data.frame with unique probe/gene ids.
	geneStatsData <- data.frame(mRNAData$PROBE.ID,mRNAData$GENE_SYMBOL);
	
	#Add a column name to our data.frame.
	colnames(geneStatsData) <- c('PROBE.ID','GENE_SYMBOL')	
	
	geneStatsData <- unique(geneStatsData[,c("PROBE.ID","GENE_SYMBOL")]);

	#---------------------
	
	#---------------------
	#Prepare the casted raw data.
	print("FORGING DATA");
	#Get a copy of the raw data.
	coercedData <- mRNAData

	#Grab only the columns we need for doing the melt/cast.
	coercedData <- coercedData[c('PATIENT.ID','VALUE','PROBE.ID','GENE_SYMBOL')]

	#Melt the data, leaving 2 columns as the grouping fields.
	meltedData <- melt(coercedData, id=c("PROBE.ID","GENE_SYMBOL","PATIENT.ID"))
	
	#Cast the data into a format that puts the PATIENT.ID in a column.
	coercedData <- data.frame(dcast(meltedData, PROBE.ID + GENE_SYMBOL ~ PATIENT.ID))

	#When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
	colnames(coercedData) <- sub("^X","",colnames(coercedData))
	
	#Get a gene list that we can use later to preserve the list of the genes.
	geneList <- as.vector(coercedData$GENE_SYMBOL)
	probeList <- as.vector(coercedData$PROBE.ID)
	#---------------------
	
	#---------------------
	#Perform multiple hypothesis testing.
	print("MULTIPLE HYPOTHESIS TESTING");
	#Remove the gene symbol column.
	coercedDataWithoutGroup <- data.matrix(subset(coercedData, select=-c(GENE_SYMBOL,PROBE.ID)))

	#Get a vector representing our subsets.
	classVector <- colnames(coercedDataWithoutGroup)
	classVector <- gsub("^S1.*","0",classVector)
	classVector <- gsub("^S2.*","1",classVector)
	classVector <- as.numeric(classVector)
	
	#Check the class vector to verify we have two subsets.
	if(length(unique(classVector)) < 2) stop("||FRIENDLY||There is only one subset selected, please select two in order to run the comparative analysis.")
	
	#Generate the t statistic.
	tTestStatistic <- mt.teststat(coercedDataWithoutGroup, classVector)
	
	#Find the raw p-value using the test statistic.
	rawp0 <- 2 * (1 - pnorm(abs(tTestStatistic)))
	
	#This is the list of all the procedures we use to generate adjusted p-values.
	procs <- c("Bonferroni", "Holm", "Hochberg", "SidakSS", "SidakSD", "BH", "BY")
	
	#Find the adjusted p-values
	adjustedPValues <- mt.rawp2adjp(rawp0, procs)
	
	#Change the list to be in order of the original list.
	adjustedPValues <- data.frame(round(adjustedPValues$adjp[order(adjustedPValues$index), ],5))
	
	#Add the GENE_SYMBOL column back to our adjusted p-values matrix.
	adjustedPValues$GENE_SYMBOL <- geneList
	adjustedPValues$PROBE.ID <- probeList
	
	#Add the t score to the frame.
	adjustedPValues$t <- tTestStatistic
	
	#Merge the stats into the gene stats frame.
	geneStatsData <- merge(geneStatsData,adjustedPValues,by=c('GENE_SYMBOL','PROBE.ID'))
	#---------------------
	
	#---------------------
	#PERMUTATION TESTING
	print("PERMUTATION TESTING");
	#Do permutation testing to get p-values, and adjusted values.
	permTesting <- mt.maxT(coercedDataWithoutGroup, classVector, B = numberOfPermutations)
	
	#Reorder the values based on the original index.
	permTesting <- permTesting[order(permTesting$index),]
	
	#Add the gene column back.
	permTesting$GENE_SYMBOL <- geneList
	permTesting$PROBE.ID <- probeList
	
	#We don't need the index column anymore.
	permTesting <- subset(permTesting, select = -c(index))
	
	#Rename the columns to show they are from the permutation testing.
	colnames(permTesting) <- c('t.permutation','rawp.permutation','adjp.permutation','GENE_SYMBOL','PROBE.ID')
	
	#Merge the stats into the gene stats frame.
	geneStatsData <- merge(geneStatsData,permTesting,by=c('GENE_SYMBOL','PROBE.ID'))
	#---------------------
	
	#---------------------
	#RANKING
	print("RANKING");
	#Order the data by absolute value of the t statistic.
	geneStatsData <- geneStatsData[order(abs(geneStatsData$t),decreasing = TRUE), ]	
	
	#Only take the top X t-scores, or the whole list if we don't have X items.
	GENELISTLENGTH <- length(geneStatsData$PROBE.ID)
	
	if(GENELISTLENGTH > numberOfMarkers) GENELISTLENGTH <- numberOfMarkers
	
	geneStatsData <- geneStatsData[1:GENELISTLENGTH,]
	
	#Add a rank column.
	geneStatsData$RANK <- 1:nrow(geneStatsData)
	#---------------------	
	
	#---------------------
	#MEAN/SD
	print("MEAN/SD");
	
	#We don't need to rank the MEAN/SD on all the data, just the top X probes. Cut the list based on the probes in the ranked data frame.
	rankCutmRNAData <- subset(mRNAData, PROBE.ID %in% geneStatsData$PROBE.ID)

	#Get the list of distinct subsets.
	distinctSubsetNames <- unique(mRNAData$SUBSET)	
	
	#Get our S1/S2 data frames.
	S1Frame <- rankCutmRNAData[which(rankCutmRNAData$SUBSET == distinctSubsetNames[1]),c('GENE_SYMBOL','PROBE.ID','VALUE')]
	S2Frame <- rankCutmRNAData[which(rankCutmRNAData$SUBSET == distinctSubsetNames[2]),c('GENE_SYMBOL','PROBE.ID','VALUE')]
	
	#Calculate Means.
	S1Mean <- ddply(S1Frame,.(PROBE.ID,GENE_SYMBOL),function(df)mean(df$VALUE))
	S2Mean <- ddply(S2Frame,.(PROBE.ID,GENE_SYMBOL),function(df)mean(df$VALUE))
	
	#Calculate SD.
	S1SD <- ddply(S1Frame,.(PROBE.ID,GENE_SYMBOL),function(df)sd(df$VALUE))
	S2SD <- ddply(S2Frame,.(PROBE.ID,GENE_SYMBOL),function(df)sd(df$VALUE))
	
	#Add column names for means.
	colnames(S1Mean) <- c('PROBE.ID','GENE_SYMBOL','S1.Mean')
	colnames(S2Mean) <- c('PROBE.ID','GENE_SYMBOL','S2.Mean')
	
	#Add column names for SD.
	colnames(S1SD) <- c('PROBE.ID','GENE_SYMBOL','S1.SD')
	colnames(S2SD) <- c('PROBE.ID','GENE_SYMBOL','S2.SD')
	
	#Merge the means back in.
	geneStatsData <- merge(geneStatsData,S1Mean,by=c('GENE_SYMBOL','PROBE.ID'))
	geneStatsData <- merge(geneStatsData,S2Mean,by=c('GENE_SYMBOL','PROBE.ID'))
	
	#Merge the SD back in.
	geneStatsData <- merge(geneStatsData,S1SD,by=c('GENE_SYMBOL','PROBE.ID'))
	geneStatsData <- merge(geneStatsData,S2SD,by=c('GENE_SYMBOL','PROBE.ID'))
	#---------------------		
	
	#---------------------
	#FOLD CHANGE
	
	#The fold change is the S1 Mean divided by the S2 Mean.
	geneStatsData$FoldChange <- geneStatsData$S1.Mean/geneStatsData$S2.Mean
	#---------------------	
	
	#---------------------
	#HEATMAP

	#Get a copy of the data.
	shortGeneStatsData <- geneStatsData
	
	#Add a column which tells us if the gene had a positive or negative t-score.
	shortGeneStatsData$positive <- shortGeneStatsData$t > 0
	
	#We need to generate a heatmap with the top 100 markers. First we merge the t-scores back in with the original data.
	heatmapData <- merge(coercedData,shortGeneStatsData,by=c('GENE_SYMBOL','PROBE.ID'))
	
	#We want to show the positive t scores in descending order, then the negative t scores in ascending order.
	positiveHeatmapData <- heatmapData[which(heatmapData$positive == TRUE),]
	negativeHeatmapData <- heatmapData[which(heatmapData$positive == FALSE),]
	
	#Order the data by absolute value of the t statistic.
	positiveHeatmapData <- positiveHeatmapData[order(positiveHeatmapData$t), ]	
	negativeHeatmapData <- negativeHeatmapData[order(negativeHeatmapData$t,decreasing = TRUE), ]	

	#Put all the data together.
	finalHeatmapData <- rbind(negativeHeatmapData,positiveHeatmapData)
	
	#Remove the t score and positive columns.
	finalHeatmapData$GENE_SYMBOL <- paste(finalHeatmapData$GENE_SYMBOL, finalHeatmapData$PROBE.ID, sep='/')
	finalHeatmapData <- subset(finalHeatmapData, select = -c(PROBE.ID,t,positive,S1.Mean,S2.Mean,S1.SD,S2.SD,FoldChange,RANK,rawp,Bonferroni,Holm,Hochberg,SidakSS,SidakSD,BH,BY,t.permutation,rawp.permutation,adjp.permutation))
	
	#Rename the first column to be "GROUP".
	colnames(finalHeatmapData)[1] <- 'GROUP'
	#---------------------
	
	#---------------------
	#WRITE TO FILE
	
	#We need MASS to dump the matrix to a file.
	require(MASS)

	#Before we write the CMS file we need to replace any empty genes with NA.
	geneStatsData$GENE_SYMBOL[which(geneStatsData$GENE_SYMBOL=="")] <- NA
	
	#Write the file with the stats by gene. This will get read into the UI.
	write.matrix(geneStatsData,output.file,sep = "\t")
	
	#Write the data file we will use for the heatmap.
	write.matrix(finalHeatmapData,'heatmapdata',sep = "\t")
	#---------------------
	
	print("-------------------")
	##########################################
}
