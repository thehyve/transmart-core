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
numberOfMarkers = 100,
aggregate.probes = FALSE,
calculateZscore = FALSE,
zscore.file = "zscores.txt"
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
	library(limma)
	
	#The Multiple hypothesis correction method used for DE analysis
	mhcMethod = "BH"
  
	#---------------------
	#Prepare raw data.
	
	#Pull the GEX data from the file.
	mRNAData <- data.frame(read.delim(input.filename))

	# Check for a single probe
	if(length(unique(mRNAData$PROBE.ID)) == 1) {
	  stop("Marker selection is not available for a single probe. If you would like to see the heatmap, please use the Heatmap tool instead.")
	}

	# Check for a single gene and probe aggregation
	if(length(unique(mRNAData$GENE_SYMBOL)) == 1 && aggregate.probes) {
	  stop("You selected probe aggregation on a single gene. Marker Selection is not available for a single probe. If you would like to see the heatmap, please use the Heatmap tool instead.")
	}

	#Trim the probe.id field.
	mRNAData$PROBE.ID 		<- gsub("^\\s+|\\s+$", "",mRNAData$PROBE.ID)
	mRNAData$GENE_SYMBOL 	<- gsub("^\\s+|\\s+$", "",mRNAData$GENE_SYMBOL)
	mRNAData$PATIENT.ID   	<- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT.ID)
	
	if(calculateZscore){
	  mRNAData = ddply(mRNAData, "PROBE.ID", transform, probe.md = median(VALUE, na.rm = TRUE))
	  mRNAData = ddply(mRNAData, "PROBE.ID", transform, probe.sd = sd(VALUE, na.rm = TRUE))        
	  mRNAData$VALUE = with(mRNAData, ifelse(probe.sd == 0, 0,
	                                         (mRNAData$VALUE - mRNAData$probe.md) / mRNAData$probe.sd))
	  mRNAData$VALUE = with(mRNAData, ifelse(VALUE > 2.5, 2.5,
	                                         ifelse(VALUE < -2.5, -2.5, VALUE)))
	  mRNAData$VALUE = round(mRNAData$VALUE, 9)
	  mRNAData$probe.md = NULL
	  mRNAData$probe.sd = NULL
	  write.table(mRNAData,zscore.file,quote=F,sep="\t",row.names=F,col.names=T)
	}
  
    if (aggregate.probes) {
        # probe aggregation function adapted from dataBuilder.R to heatmap's specific data-formats
        mRNAData <- MS.probe.aggregation(mRNAData, collapseRow.method = "MaxMean", collapseRow.selectFewestMissing = TRUE)
    }

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
	#Fitting linear model with limma
	print("LINEAR MODEL")
	#Remove the gene symbol column.
	coercedDataWithoutGroup <- data.matrix(subset(coercedData, select=-c(GENE_SYMBOL,PROBE.ID)))
	rownames(coercedDataWithoutGroup)=coercedData$PROBE.ID  

	#Creating a named vector for mapping PROBE.ID to GENE.SYMBOL 
	gene.symbols=coercedData$GENE_SYMBOL
	names(gene.symbols)=coercedData$PROBE.ID
  
	#Get the vectors representing our subsets.
	# ... for S1
	classVector_S2 <- colnames(coercedDataWithoutGroup)
	classVector_S2 <- gsub("^S1.*","0",classVector_S2)
	classVector_S2 <- gsub("^S2.*","1",classVector_S2)
	classVector_S2 <- as.numeric(classVector_S2)
	# ... for S2
	classVector_S1 <- colnames(coercedDataWithoutGroup)
	classVector_S1 <- gsub("^S1.*","1",classVector_S1)
	classVector_S1 <- gsub("^S2.*","0",classVector_S1)
	classVector_S1 <- as.numeric(classVector_S1) 
	
	#Check the class vectors to verify we have two subsets.
	if(length(unique(classVector_S1)) < 2) stop("||FRIENDLY||There is only one subset selected, please select two in order to run the comparative analysis.")
	if(length(unique(classVector_S2)) < 2) stop("||FRIENDLY||There is only one subset selected, please select two in order to run the comparative analysis.")
	
	# Design matrix 
	design <- cbind(S1=classVector_S1,S2=classVector_S2)
  
	##... and contrast matrix
	contrast.matrix = makeContrasts(S1-S2, levels=design)
	
  	print(contrast.matrix)
	
	# Linear model fitting 
	print("-lmFit")
	fit <- lmFit(coercedDataWithoutGroup, design)
	fit <- contrasts.fit(fit, contrast.matrix)
	print("-eBayes")
	fit <- eBayes(fit)
	print("topTable ...")
	
	contr=1
	top.fit = data.frame(
	  ID=rownames(fit$coefficients), #Remove Dependence on limma version
	  logFC=fit$coefficients[,contr],
	  t=fit$t[,contr],
	  P.Value=fit$p.value[,contr],
	  adj.P.val=p.adjust(p=fit$p.value[,contr], method=mhcMethod),
	  B=fit$lods[,contr]
	)
	
	top.fit.ranked.decr = top.fit[ order(top.fit$B, decreasing=T), ]
	rownames(top.fit.ranked.decr) = NULL
	
	top.fit.ranked.decr.filt = top.fit.ranked.decr[1:numberOfMarkers,]
	topgenes = cbind(gene.symbols[as.vector(top.fit.ranked.decr.filt$ID)], top.fit.ranked.decr.filt)
	colnames(topgenes) = c("GENE_SYMBOL", "PROBE.ID", "logFC", "t", "P.value", "adj.P.val", "B")
	rownames(topgenes) = NULL
	
	#End Linear model fitting
	
	#HEATMAP
	#We need to generate a heatmap with the top 100 markers. First we merge the t-scores back in with the original data.
	#heatmapData <- merge(coercedData,shortGeneStatsData,by=c('GENE_SYMBOL','PROBE.ID'))
	heatmapData <- merge(coercedData,topgenes,by=c('GENE_SYMBOL','PROBE.ID'))
	
	#Add a column which tells us if t score is positive
	heatmapData$positive <- heatmapData$t > 0
	
	#We want to show the positive t scores in descending order, then the negative t scores in ascending order.
	positiveHeatmapData <- heatmapData[which(heatmapData$positive == TRUE),]
	negativeHeatmapData <- heatmapData[which(heatmapData$positive == FALSE),]
	
	#Order the data by absolute value of the t statistic.
	positiveHeatmapData <- positiveHeatmapData[order(positiveHeatmapData$t), ]	
	negativeHeatmapData <- negativeHeatmapData[order(negativeHeatmapData$t,decreasing = TRUE), ]	

	#Put all the data together.
	finalHeatmapData <- rbind(negativeHeatmapData,positiveHeatmapData)
	
	#In case there is no GENE_SYMBOL annotation existing for given probe
	if(length(which(finalHeatmapData$GENE_SYMBOL==""))>0)
	  finalHeatmapData$GENE_SYMBOL[which(finalHeatmapData$GENE_SYMBOL=="")]="/"
  
	finalHeatmapData$GENE_SYMBOL <- paste(finalHeatmapData$PROBE.ID, finalHeatmapData$GENE_SYMBOL, sep='_')
	#finalHeatmapData$GENE_SYMBOL <- paste(finalHeatmapData$GENE_SYMBOL, " (" , finalHeatmapData$PROBE.ID, ")",sep='')
	finalHeatmapData$GENE_SYMBOL<-gsub("_\\s+$","",finalHeatmapData$GENE_SYMBOL, ignore.case = FALSE, perl = T)
	# end
	#finalHeatmapData <- subset(finalHeatmapData, select = -c(PROBE.ID,t,positive,S1.Mean,S2.Mean,S1.SD,S2.SD,FoldChange,RANK,rawp,BH,BY))
	finalHeatmapData <- subset(finalHeatmapData, select = -c(PROBE.ID,t,positive,logFC,P.value,adj.P.val,B))
	
	
	#Rename the first column to be "PROBE.ID".
	colnames(finalHeatmapData)[1] <- 'PROBE.ID'
	
	#Performing row-wise z-score scaling
	finalHeatmapData.zscore = t(scale(t(finalHeatmapData[,-1])))
	finalHeatmapData.zscore = cbind(finalHeatmapData[,1], finalHeatmapData.zscore)
	
# 	#---------------------
 	#WRITE TO FILE
  #Before we write the CMS file we need to replace any empty genes with NA.
  topgenes$GENE_SYMBOL[which(topgenes$GENE_SYMBOL=="")] <- NA
  #Write the file with the stats by gene. This will get read into the UI.
  write.table(topgenes,output.file,sep = "\t", , quote=F, row.names=F)
  #Write the data file we will use for the heatmap.
  write.table(finalHeatmapData,'heatmapdata',sep = "\t", quote=F, row.names=F)
	#---------------------
	
	print("-------------------")
	##########################################
}
MS.probe.aggregation <- function(mRNAData, collapseRow.method,
                                 collapseRow.selectFewestMissing, 
                                 output.file = "aggregated_data.txt") {
    library(WGCNA)
    
    #remove SUBSET column
    mRNAData <- subset(mRNAData, select = -c(SUBSET))
    
    meltedData <- melt(mRNAData, id=c("PROBE.ID","GENE_SYMBOL","PATIENT.ID"))
    #Cast the data into a format that puts the PATIENT.ID in a column.
    castedData <- data.frame(dcast(meltedData, PROBE.ID + GENE_SYMBOL ~ PATIENT.ID))
    #Create a unique identifier column.
    castedData$UNIQUE_ID <- paste(castedData$PROBE.ID, castedData$GENE_SYMBOL,sep="___")
    #Set the name of the rows to be the unique ID.
    rownames(castedData) = castedData$UNIQUE_ID
    if (nrow(castedData) <= 1) {
        warning("Only one probe.id present in the data. Probe aggregation can not be performed.")
        return (mRNAData)
    }
    
    #Input expression matrix for collapseRows function
    datET = subset(castedData, select = -c(GENE_SYMBOL,PROBE.ID,UNIQUE_ID))
    GENE_SYMBOL = as.vector(castedData$GENE_SYMBOL)
    UNIQUE_ID = as.vector(castedData$UNIQUE_ID)
    rownames(datET) = UNIQUE_ID
     #Run the collapsing on a subset of the data by removing some columns.
     finalData <- collapseRows( datET = datET,
                                rowGroup = GENE_SYMBOL,
                                rowID = UNIQUE_ID,
                                method = collapseRow.method,
                                connectivityBasedCollapsing = TRUE,
                                methodFunction = NULL,
                                connectivityPower = 1,
                                selectFewestMissing = collapseRow.selectFewestMissing,
                                thresholdCombine = NA
                                )
      #Coerce the data into a data frame.
      finalData=data.frame(finalData$group2row, finalData$datETcollapsed)
      #Rename the columns, the selected row_id is the unique_id.
      colnames(finalData)[2] <- 'UNIQUE_ID'
 
      #Merge the probe.id and gene symbol back in and remove the group and unique_id columns.
      matrix_annot = matrix(unlist(strsplit(as.vector(finalData$UNIQUE_ID), split="___")), ncol=2, byrow=T)
      colnames(matrix_annot) = c("PROBE.ID", "GENE_SYMBOL")
      finalData = cbind(matrix_annot, finalData)
      finalData = subset(finalData, select=-c(group, UNIQUE_ID))
      #Melt the data back into the initial format.
      finalData <- melt(finalData)
  
      #Set the column names again.
      colnames(finalData) <- c("PROBE.ID","GENE_SYMBOL","PATIENT.ID","VALUE")
      #When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
      finalData$PATIENT.ID <- sub("^X","",finalData$PATIENT.ID)
 
      #Return relevant columns
      finalData <- finalData[,c("PATIENT.ID","VALUE","PROBE.ID","GENE_SYMBOL")]
     
      finalData$SUBSET<-finalData$PATIENT.ID
      finalData$SUBSET[grep("^S1_|_S1_|_S1$",finalData$SUBSET)]<-"S1"
      finalData$SUBSET[grep("^S2_|_S2_|_S2$",finalData$SUBSET)]<-"S2"
 
      write.table(finalData, file = output.file, sep = "\t", row.names = FALSE)
      return(finalData)
}
