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
output.file = "Heatmap",
meltData = TRUE,
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
    print("HeatmapLoader.R")
    print("CREATING HEATMAP")

    library(Cairo)
    library(ggplot2)
    library(reshape2)
    library(gplots)
    library(plyr)
	
    #Pull the GEX data from the file.
    mRNAData <- data.frame(read.delim(input.filename, stringsAsFactors = FALSE))
    if (nrow(mRNAData) == 0) {
        CairoPNG(file = paste(output.file,".png",sep=""), width=1200, height=600,units = "px")
        Plot.error.message("Your selection yielded an empty dataset,\nplease check your subset and biomarker selection."); return()
    }

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
    #If we have to melt and cast, do it here, otherwise we make the group column the rownames
    if(meltData == TRUE) {
        # The GROUP column needs to have the values from GENE_SYMBOL concatenated as a suffix,
        # but only if the latter does not contain a private value
        # (which means that the biomarker was not present in any of the dictionaries)
        mRNAData$GROUP <- as.character(mRNAData$GROUP)
        rowsToConcatenate <- grep("^PRIVATE", mRNAData$GENE_SYMBOL, invert = TRUE)
        mRNAData$GROUP[rowsToConcatenate] <- paste(mRNAData$GROUP[rowsToConcatenate],
                                                   mRNAData$GENE_SYMBOL[rowsToConcatenate], sep="_")
        mRNAData$GROUP <- as.factor(mRNAData$GROUP)
    
        if (aggregate.probes) {
            # probe aggregation function adapted from dataBuilder.R to heatmap's specific data-formats
            mRNAData <- Heatmap.probe.aggregation(mRNAData, collapseRow.method = "MaxMean",
                                                  collapseRow.selectFewestMissing = TRUE)
        }
    
        #Trim the patient.id field.
        mRNAData$PATIENT_NUM <- gsub("^\\s+|\\s+$", "", mRNAData$PATIENT_NUM)

        #Melt the data, leaving 3 columns as the grouping fields.
        meltedData <- melt(mRNAData, id = c("GROUP", "PATIENT_NUM", "GENE_SYMBOL"))

        #Cast the data into a format that puts the ASSAY.ID in a column.
        mRNAData <- data.frame(dcast(meltedData, GROUP ~ PATIENT_NUM))

        #When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
        colnames(mRNAData) <- sub("^X", "", colnames(mRNAData))
        
        #Set the name of the rows to be the names of the probes.
        rownames(mRNAData) = mRNAData$GROUP
        
        #Convert data to a integer matrix.
        mRNAData <- data.matrix(subset(mRNAData, select = -c(GROUP)))
    } else {
        colnames(mRNAData) <- gsub("^\\s+|\\s+$", "", colnames(mRNAData))

        #Use only unique row names. This unique should get rid of the case where we have multiple genes per probe. The values for the probes are all the same.
        mRNAData <- unique(mRNAData)
        
        #Convert data to a integer matrix.
        rownames(mRNAData) <- mRNAData$PROBE.ID
        mRNAData <- data.matrix(subset(mRNAData, select = -c(PROBE.ID)))
    }

    #We can't draw a heatmap for a matrix with no rows.
    if(nrow(mRNAData) < 1) stop("||FRIENDLY||R cannot plot a heatmap with no Gene/Probe selected. Please check your variable selection and run again.")
    if(ncol(mRNAData) < 2) stop("||FRIENDLY||R cannot plot a heatmap with only 1 Patient data. Please check your variable selection and run again.")

    #We can't draw a heatmap for a matrix with only 1 row (restriction of heatmap.2 function).
    #Adding an extra dummy row with NA values, does the trick as they seems to be ignored in the plot and the density histogram
    if(nrow(mRNAData) == 1) {
        mRNAData <- rbind(mRNAData, mRNAData[1, ])
        mRNAData[2, ] = NA
    }

    # by Serge and Wei to filter a sub set and reorder markers

    num_markers<-dim(mRNAData)[1]  # number of markers in the dataset
    if (num_markers > maxDrawNumber) {  # if more markers in the dataset, apply filter
        sd_rows_mRNA <- apply (mRNAData, 1, sd, na.rm = T)
        mRNAData <- mRNAData[!is.na(sd_rows_mRNA), ]  # remove markers where sd is NA
        sd_rows_mRNA <- sd_rows_mRNA[!is.na(sd_rows_mRNA)]
        indices_to_include <- order(sd_rows_mRNA, decreasing = T)[1:maxDrawNumber]  # filter by SD, keep only the top maxDrawNumber
        mRNAData <- mRNAData[indices_to_include, ]
    }

    colcolor <- colnames(mRNAData)  # assign colors for different subset
    colcolor[grep("^S1_|_S1_|_S1$", colnames(mRNAData))] <- "orange"
    colcolor[grep("^S2_|_S2_|_S2$", colnames(mRNAData))] <- "yellow"

    # reorder the data by rowmean of Subset 1
    mean_reorder <- rowMeans(mRNAData[ , colcolor=="orange"], na.rm = T)
    mRNAData <- mRNAData[order(mean_reorder, decreasing = T), ]
    # remove the _ at the end of the marker label
    rownames(mRNAData) <- gsub("_\\s+$", "", rownames(mRNAData), ignore.case = FALSE, perl = T)

# end filter subset

# check whether there is enough data to draw heatmap
    n_remaining_marker <- nrow(mRNAData)
    n_remaining_sample <- ncol(mRNAData)
    if (is.null(color.range.clamps)) color.range.clamps = c(min(mRNAData), max(mRNAData))
    if (n_remaining_marker > 1 & n_remaining_sample > 1) {
        plotHeatmap(mRNAData, colcolor, color.range.clamps, output.file)
    } else {
        #Prepare the package to capture the image file.
        CairoPNG(file = paste(output.file, ".png", sep = ""), width = as.numeric(imageWidth),
                height = as.numeric(imageHeight), pointsize = as.numeric(pointsize))
        tmp <- frame()
        tmp2 <- mtext("not enough marker/samples to draw heatmap", cex = 2)
        print(tmp)
        print(tmp2)
        dev.off()
    }

    print("-------------------")
    list(mRNAData)
}

plotHeatmap <- function(data, colcolors, color.range.clamps, output.file = "Heatmap") {
    require(Cairo)
    require(gplots)

    onlyOneSubset <- length(unique(colcolors))==1
    if (onlyOneSubset) { colcolors <- "white" }

    # The Cairo graphical backend has a width and height resolution restriction which depends upon environment settings
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
    rowSizes <- c(1, 3, ifelse(onlyOneSubset, 0.5, 3), nrow(data), legendSizesInCells, letterSizeInCells * rowLabelSizeMax) * pxPerCell
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
              ColSideColors = colcolors,
              col = plotColors,
              breaks = seq(color.range.clamps[1], color.range.clamps[2], length.out = 800 + 1),
              sepwidth = c(0, 0),
              margins = c(0, 0),
              cexRow = 1.7, #hmPars$labelPointSize,
              cexCol = 1.7, #hmPars$labelPointSize,
              scale = "none",
              dendrogram = "none",
              Rowv = NA,
              Colv = NA,
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

    # generate subset-legend if needed
    if (!onlyOneSubset) {
        legend(x = sum(hmCanvasColumnRatios[1:4]), y = 1 - sum(hmCanvasRowRatios[1:2]),
               legend = c("Subset 1", "Subset 2"),
               fill = c("orange", "yellow"),
               bg = "white", horiz = TRUE,
               cex = 1.2, xjust = 0, yjust = 1, bty = "n")
    }

    dev.off()
}

Heatmap.probe.aggregation <-
function(mRNAData, collapseRow.method, collapseRow.selectFewestMissing, output.file = "aggregated_data.txt") {
    library(WGCNA)

    meltedData <- melt(mRNAData, id = c("GROUP", "GENE_SYMBOL", "PATIENT_NUM"))

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
