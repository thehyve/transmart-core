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
aggregate.probes = FALSE,
max.pcs.to.show = 10
)
{

    print("-------------------")
    print("LoadPCA.R")
    print("CREATING PCA PLOT")

    library(reshape2)
    library(Cairo)
	library(ggplot2)

    #Prepare the package to capture the image file.
    CairoPNG(file=paste("PCA.png",sep=""),width=400,height=400)

    #Pull the GEX data from the file.
    mRNAData <- data.frame(read.delim(input.filename))
    if (nrow(mRNAData) == 0) {
        Plot.error.message("Your selection yielded an empty dataset,\nplease check your subset and biomarker selection."); return()
    }

    if (nrow(mRNAData)<1) stop("Input data is empty. Common causes: either the specified subset has no matching data in the selected node, or the gene/pathway is not present.")

    if (aggregate.probes) {
        # probe aggregation function adapted from dataBuilder.R to heatmap's specific data-formats
        mRNAData <- PCA.probe.aggregation(mRNAData, collapseRow.method = "MaxMean", collapseRow.selectFewestMissing = TRUE)
    }

    mRNAData$PROBE.ID       <- gsub("^\\s+|\\s+$", "",mRNAData$PROBE.ID)
    mRNAData$GENE_SYMBOL    <- gsub("^\\s+|\\s+$", "",mRNAData$GENE_SYMBOL)
    mRNAData$PATIENT.ID     <- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT.ID)
	mRNAData$SUBSET   	    <- gsub("^\\s+|\\s+$", "",mRNAData$SUBSET)

    # The PROBE.ID column needs to have the values from GENE_SYMBOL concatenated as a suffix,
    # but only if the latter does not contain a private value (which means that the biomarker was not present in any of the dictionaries)
    mRNAData$PROBE.ID <- as.character(mRNAData$PROBE.ID)
    rowsToConcatenate <- grep("^PRIVATE", mRNAData$GENE_SYMBOL, invert = TRUE)
    mRNAData$PROBE.ID[rowsToConcatenate] <- paste(mRNAData$PROBE.ID[rowsToConcatenate], mRNAData$GENE_SYMBOL[rowsToConcatenate],sep="_")
    mRNAData$PROBE.ID <- as.factor(mRNAData$PROBE.ID)
	 groupValues <- levels(mRNAData$PROBE.ID)
	 mRNAData$PROBE.ID <- paste("X",as.numeric(mRNAData$PROBE.ID),sep="")

    #Grab only the columns we need for doing the melt/cast.
    mRNAData <- mRNAData[c('PATIENT.ID','VALUE','PROBE.ID')]

    #Melt the data, leaving 2 columns as the grouping fields.
    meltedData <- melt(mRNAData, id=c("PROBE.ID","PATIENT.ID"))

    #Cast the data into a format that puts the PATIENT.ID in a column.
    mRNAData <- data.frame(dcast(meltedData, PATIENT.ID ~ PROBE.ID))

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
    if (ncol(mRNAData) == 0) {
        Plot.error.message("The selected cohort has incomplete data for each of your biomarkers.\nNo data is left to plot a PCA with."); return()
    }

    ###print(mRNAData)

    print("Run the PCA Analysis")
    #Run the PCA Analysis
    pca.results <- prcomp(mRNAData)
    #print("Completed PCA Analysis")

    #Get the number of components.
    numberOfComponents <- length(pca.results$sdev)
    max.pcs.to.show <- min(max.pcs.to.show, numberOfComponents)

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

    f <- function(i,GENELISTLENGTH) {
        #Form the file name.
        currentFile <- paste('GENELIST',i,'.TXT',sep="")

        #Create the current data frame from the gene names and value columns.
        currentData <- data.frame(rownames(rotationFrame),round(rotationFrame[,i],3))

        colnames(currentData) <- c('GENE_SYMBOL','VALUE')

        #Reorder the genes based on decreasing absolute value of the value column.
        currentData <- currentData[order(abs(currentData$VALUE),decreasing = TRUE),]

        #Pull only the records we are interested in.
        currentData <- currentData[1:GENELISTLENGTH,]
        currentData$GENE_SYMBOL <- groupValues[as.numeric(sub("^X","",currentData$GENE_SYMBOL))]

        #Write the list to a file.
        write.table(currentData,currentFile,quote=F,sep="\t",row.names=F,col.names=F)
    }

    sapply(1:max.pcs.to.show, f, GENELISTLENGTH)

    #Finally create the Scree plot.
    plot(pca.results,type="lines", main="Scree Plot", npcs = max.pcs.to.show)
    title(xlab = "Component")

    dev.off()
	
	#Creates the plot of observations
	scores <- as.data.frame(pca.results$x)
	scores[,"subset"] <- sub("S2", "Subset 2", sub("S1", "Subset 1", substr(rownames(scores),0,2)))
	for (i in 1:2){
    	for(j in (i+1):3){     
			tmp <- ggplot(data=scores, aes_string(x=paste("PC", i, sep=""), y=paste("PC", j, sep="")))
			tmp <- tmp +geom_hline(yintercept=0, colour="gray65")
			tmp <- tmp +geom_vline(xintercept=0, colour="gray65")
			tmp <- tmp + geom_point(aes(colour=subset), size=3)
			tmp <- tmp + opts(title="Plot of observations")
			tmp <- tmp+ scale_color_manual("Subsets", breaks = c("Subset 1", "Subset 2"), values=c("orange", "yellow"))
			CairoPNG(file=paste("PCA_observations_", i, "_", j, ".png",sep=""),width=600,height=600)
			print (tmp)
			dev.off()
		}
	}
	
	#Creates the circle of correlations
	for (i in 1:2){
    	for(j in (i+1):3){
			corcir=circle(c(0,0), npoints=100)	
			correlations=as.data.frame(cor(mRNAData, pca.results$x))
			arrows=data.frame(genes=rownames(correlations), x1=rep(0, length(pca.results$center)), y1=rep(0,length(pca.results$center)), x2=correlations[,paste("PC", i, sep="")], y2=correlations[,paste("PC", j, sep="")])
			arrows$genes <- groupValues[as.numeric(sub("^X","",arrows$genes))]

			tmp <- ggplot()
			tmp <- tmp + geom_path(data=corcir, aes(x=x, y=y), colour="gray65")
			tmp <- tmp + geom_segment(data=arrows, aes(x=x1, y=y1, xend=x2, yend=y2), colour="gray65")
			tmp <- tmp + geom_text(data=arrows, aes(x=x2, y=y2, label=genes), size = 4, vjust=1)
			tmp <- tmp + geom_hline(yintercept=0, colour="gray65")
			tmp <- tmp + geom_vline(xintercept=0, colour="gray65")
			tmp <- tmp + xlim(-1.1,1.1) + ylim(-1.1,1.1)
			tmp <- tmp + labs(x=paste("PC", i, " axis", sep=""), y=paste("PC", j, " axis", sep=""))
			tmp <- tmp +opts(title="Circle of correlations")
			CairoPNG(file=paste("PCA_circle_correlations_", i, "_", j, ".png",sep=""),width=600,height=600)
			print (tmp)
			dev.off()
		}
	}
}

Plot.error.message <- function(errorMessage) {
    # TODO: This error handling hack is a temporary permissible quick fix:
    # It deals with getting error messages through an already used medium (the plot image) to the end-user in certain relevant cases.
    # It should be replaced by a system wide re-design of consistent error handling that is currently not in place. See ticket HYVE-12.
    print(paste("Error encountered. Caught by Plot.error.message(). Details:", errorMessage))
    tmp <- frame()
    tmp2 <- mtext(errorMessage,cex=2)
    print(tmp)
    print(tmp2)
    dev.off()
}

PCA.probe.aggregation <- function(mRNAData, collapseRow.method, collapseRow.selectFewestMissing, output.file = "aggregated_data.txt") {
    library(WGCNA)

    #Cast the data into a format that puts the PATIENT_NUM in a column
    castedData <- data.frame(dcast(mRNAData, PROBE.ID + GENE_SYMBOL ~ PATIENT.ID, value.var = "VALUE"))

    #Create a unique identifier column.
    castedData$UNIQUE_ID <- paste(castedData$GENE_SYMBOL,castedData$PROBE.ID,sep="")

    #Set the name of the rows to be the unique ID.
    rownames(castedData) = castedData$UNIQUE_ID

    if (nrow(castedData) <= 1) {
        warning("Only one probe.id present in the data. Probe aggregation not possible.")
        return (mRNAData)
    }

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
    finalData <- melt(finalData, id.vars = c("group", "PROBE.ID"))

    #Set the column names again.
    colnames(finalData) <- c("GENE_SYMBOL","PROBE.ID","PATIENT.ID","VALUE")

    #When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
    finalData$PATIENT.ID <- sub("^X","",finalData$PATIENT.ID)

    write.table(finalData, file = output.file, sep = "\t", row.names = FALSE)

    finalData
}
circle <- function(center=c(0,0), npoints=100){
	r=1
	tt=seq(0, 2*pi, length=npoints)
	xx=center[1]+r*cos(tt)
	yy=center[2]+r*sin(tt)
	return (data.frame(x=xx, y=yy))
}
