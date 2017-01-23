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
maxDrawNumber = Inf,
color.range.clamps = c(-2.5,2.5),
aggregate.probes = FALSE,
calculateZscore = FALSE,
zscore.file = "zscores.txt"
)
{
    print("-------------------")
    print("KMeansHeatmap.R")
    print("CREATING K-MEANS HEATMAP")

    library(Cairo)
    library(reshape2)
    library(gplots)
    library(plyr)

    #Validate the number of clusters after converting to a numeric.
    clusters.number <- as.numeric(clusters.number)

    if(is.na(clusters.number)) stop("||FRIENDLY||The number of clusters supplied was invalid.")

    #Pull the GEX data from the file.
    mRNAData <- data.frame(read.delim(input.filename))
    if (nrow(mRNAData) == 0) {
        CairoPNG(file = paste(output.file,".png",sep=""), width=1200, height=600,units = "px")
        Plot.error.message("Your selection yielded an empty dataset,\nplease check your subset and biomarker selection."); return()
    }

    # The GROUP column needs to have the values from GENE_SYMBOL concatenated as a suffix,
    # but only if the latter does not contain a private value (which means that the biomarker was not present in any of the dictionaries)
    mRNAData$GROUP <- as.character(mRNAData$GROUP)
    rowsToConcatenate <- grep("^PRIVATE", mRNAData$GENE_SYMBOL, invert = TRUE)
    mRNAData$GROUP[rowsToConcatenate] <- paste(mRNAData$GROUP[rowsToConcatenate], mRNAData$GENE_SYMBOL[rowsToConcatenate],sep="_")
    mRNAData$GROUP <- as.factor(mRNAData$GROUP)

    groupValues <- levels(mRNAData$GROUP)
    mRNAData$GROUP <- paste("X",as.numeric(mRNAData$GROUP),sep="")

    if(calculateZscore){
      mRNAData = ddply(mRNAData, "GROUP", transform, probe.md = median(VALUE, na.rm = TRUE))
      mRNAData = ddply(mRNAData, "GROUP", transform, probe.sd = sd(VALUE, na.rm = TRUE))        
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
        # probe aggregation function adapted from dataBuilder.R to K-means clustering heatmap's specific data-formats
        mRNAData <- Heatmap.probe.aggregation(mRNAData, collapseRow.method = "MaxMean", collapseRow.selectFewestMissing = TRUE)
    }

    #Trim the patient.id field.
    mRNAData$PATIENT_NUM <- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT_NUM)

    #Trim the Gene/Probe field.
    mRNAData$GROUP <- sub("^\\s+|\\s+$", "",mRNAData$GROUP)

    #Melt the data, leaving 3 columns as the grouping fields.
    meltedData <- melt(mRNAData, id=c("GROUP","PATIENT_NUM","GENE_SYMBOL"))

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
    if (nrow(matrixData) == 0) {
        Plot.error.message("The selected cohort has incomplete data for each of your biomarkers.\nNo data is left to plot a heatmap with."); return()
    }

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


    if (is.null(color.range.clamps)) color.range.clamps = c(min(matrixData), max(matrixData))

    rowLabels <- groupValues[as.numeric(rownames(matrixData))]

    plotHeatmap(matrixData, rowLabels, patientcolors, color.range.clamps, output.file)
}

plotHeatmap <- function(data, rowLabels, colcolors, color.range.clamps, output.file = "Heatmap") {
    require(Cairo)
    require(gplots)

    # The Cairo graphical backend has a width and heigth resolution restriction which depends upon environment settings
    # The number of pixels (width and heigth) for each boxplot's cell is therefore dependent on the max number of row or column cells
    # These numbers were found experimentally to generate legible plots, but might need to be further reduced if problems occur
    maxDim <- max(dim(data)[1], dim(data)[2])
    if (maxDim < 250) {pxPerCell <- 30}
    else if (maxDim < 500) {pxPerCell <- 25}
    else if (maxDim < 1000) {pxPerCell <- 20}
    else if (maxDim < 2000) {pxPerCell <- 15}
    else {pxPerCell <- 10} # less than 10 pixels per cell makes the plot illegible

    # all paramaters determining the sizes of elements in the heatmap scale with the set pxPerCell (eg. fontsizes, legendsizes)
    hmPars <- list(pointSize = pxPerCell / 1, labelPointSize = pxPerCell / 9)

    # The heatmap is split into a grid for each of its elements to be drawn in (see lmat argument in heatmap.2 function)
    # the heatmap plot is split in 6 columns (left border, rows-dendrogram, rowcolors (not used), heatmap, labelStarts/legends, labelOverflow)
    # the heatmap plot is split in 6 rows (top border, columns-dendrogram, columncolors/subsetLegend, heatmap, labelStarts/colLegend, labelOverflow)
    # first, calculate labelOverflow sizes
    letterSizeInCells <- 0.7
    legendSizesInCells <- 20
    rowLabelSizeMax <- max(0, max(nchar(colnames(data))) - (legendSizesInCells / letterSizeInCells))
    colLabelSizeMax <- max(0, max(nchar(rownames(data))) - (legendSizesInCells / letterSizeInCells))

    # define the heatmap's grid sizes
    columnSizes <- c(1, 3, 0, ncol(data), legendSizesInCells, letterSizeInCells * colLabelSizeMax) * pxPerCell
    rowSizes <- c(1, 3, 3, nrow(data), legendSizesInCells, letterSizeInCells * rowLabelSizeMax) * pxPerCell
    totalWidth <- sum(columnSizes)
    totalHeight <- sum(rowSizes)
    hmCanvasColumnRatios <- columnSizes / totalWidth
    hmCanvasRowRatios <- rowSizes / totalHeight

    tryCatch(CairoPNG(file = paste(output.file, ".png", sep=""), width = totalWidth,
                      height = totalHeight, pointsize = hmPars$pointSize, units = "px"),
             error = function(e) {stop("Cairo graphical backend could not be created. Your plot size is likely too big.")})

    par(mar = c(0, 0, 0, 0))

    noDifferentColors <- 800
    plotColors <- greenred(noDifferentColors)

    colorLegendPlot <- function() {
        par(mar = c(6, 2, 6, 2))
        sequenceOfBars <- rep(1, length(plotColors))
        barplot(sequenceOfBars, main="Color mapping of Z score", cex.main = 2.2,
                col = plotColors, space = 0, border = NA, axes = FALSE)
        axis(1, at = c(1, noDifferentColors/2, noDifferentColors), labels = c(color.range.clamps[1], 0, color.range.clamps[2]), cex.axis = 3, lwd = 4, padj = 1)
    }

    heatmap.2(data,
              Rowv=NA,
              Colv=NA,
              ColSideColors = colcolors,
              col = plotColors,
              breaks = seq(color.range.clamps[1], color.range.clamps[2], length.out = 800 + 1),
              sepwidth = c(0, 0),
              margins = c(0, 0),
              cexRow = 1.7, #hmPars$labelPointSize,
              cexCol = 1.7, #hmPars$labelPointSize,
              labRow = rowLabels,
              scale = "none",
              dendrogram = "none",
              density.info = "none", # histogram", # density.info=c("histogram","density","none")
              trace = "none",
              lmat = matrix(ncol = 6, byrow = TRUE, data = c(
                  # 1 is subset column color bar, 2 is heatmap, 3 is row-clustering, 4 is column-clustering, 5 is color histogram (turned off), 6 is custom color legend
                  -1, -1, -1, -1, -1, -1,
                  -1,  5, -1,  4, -1, -1,
                  -1, -1, -1,  1, -1, -1,
                  -1,  3, -1,  2, -1, -1,
                  -1, -1, -1, -1,  6, -1,
                  -1, -1, -1, -1, -1, -1)),
              lhei = hmCanvasRowRatios,
              lwid = hmCanvasColumnRatios,
              key = FALSE,
              extrafun = colorLegendPlot)

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

Heatmap.probe.aggregation <- function(mRNAData, collapseRow.method, collapseRow.selectFewestMissing, output.file = "aggregated_data.txt") {
    library(WGCNA)

    meltedData <- melt(mRNAData, id=c("GROUP","GENE_SYMBOL","PATIENT_NUM"))

    #Cast the data into a format that puts the PATIENT_NUM in a column.
    castedData <- data.frame(dcast(meltedData, GROUP + GENE_SYMBOL ~ PATIENT_NUM))

    #Create a unique identifier column.
    castedData$UNIQUE_ID <- paste(castedData$GENE_SYMBOL,castedData$GROUP,sep="")

    #Set the name of the rows to be the unique ID.
    rownames(castedData) = castedData$UNIQUE_ID

    if (nrow(castedData) <= 1) {
        warning("Only one probe.id present in the data. Probe aggregation not possible.")
        return (mRNAData)
    }

    #Run the collapse on a subset of the data by removing some columns.
    finalData <- collapseRows(subset(castedData, select = -c(GENE_SYMBOL,GROUP,UNIQUE_ID) ),
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
    finalData <- merge(finalData,castedData[c('UNIQUE_ID','GROUP')],by=c('UNIQUE_ID'))

    #Remove the unique_id and selected row ID column.
    finalData <- subset(finalData, select = -c(UNIQUE_ID))

    #Melt the data back.
    finalData <- melt(finalData)

    #Set the column names again.
    colnames(finalData) <- c("GENE_SYMBOL","GROUP","PATIENT_NUM","VALUE")

    #When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
    finalData$PATIENT_NUM <- sub("^X","",finalData$PATIENT_NUM)

    #Return relevant columns
    finalData <- finalData[,c("PATIENT_NUM","VALUE","GROUP","GENE_SYMBOL")]

    write.table(finalData, file = output.file, sep = "\t", row.names = FALSE)

    finalData
}

