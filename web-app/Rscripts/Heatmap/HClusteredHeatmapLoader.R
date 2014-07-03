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
maxDrawNumber = Inf,
color.range.clamps = c(-2.5,2.5),
aggregate.probes = FALSE,
cluster.by.rows = TRUE,
cluster.by.columns = TRUE
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
    if (nrow(mRNAData) == 0) {
        Plot.error.message("Your selection yielded an empty dataset,\nplease check your subset and biomarker selection."); return()
    }

    # The GROUP column needs to have the values from GENE_SYMBOL concatenated as a suffix,
    # but only if the latter does not contain a private value (which means that the biomarker was not present in any of the dictionaries)
    mRNAData$GROUP <- as.character(mRNAData$GROUP)
    rowsToConcatenate <- grep("^PRIVATE", mRNAData$GENE_SYMBOL, invert = TRUE)
    mRNAData$GROUP[rowsToConcatenate] <- paste(mRNAData$GROUP[rowsToConcatenate], mRNAData$GENE_SYMBOL[rowsToConcatenate],sep="_")
    mRNAData$GROUP <- as.factor(mRNAData$GROUP)

    if (aggregate.probes) {
        # probe aggregation function adapted from dataBuilder.R to heatmap's specific data-formats
        mRNAData <- Heatmap.probe.aggregation(mRNAData, collapseRow.method = "MaxMean", collapseRow.selectFewestMissing = TRUE)
    }

    #If we have to melt and cast, do it here, otherwise we make the group column the rownames
    if(meltData == TRUE)
    {
        #Trim the patient.id field.
        mRNAData$PATIENT_NUM <- gsub("^\\s+|\\s+$", "",mRNAData$PATIENT_NUM)

        #Melt the data, leaving 3 columns as the grouping fields.
        meltedData <- melt(mRNAData, id=c("GROUP","PATIENT_NUM","GENE_SYMBOL"))

        #Cast the data into a format that puts the ASSAY.ID in a column.
        mRNAData <- data.frame(dcast(meltedData, GROUP ~ PATIENT_NUM))

        #When we convert to a data frame the numeric columns get an x in front of them. Remove them here.
        colnames(mRNAData) <- sub("^X","",colnames(mRNAData))
    } else {
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

    # by Serge and Wei to filter a sub set and reorder markers

    mRNAData <- mRNAData[!apply(is.na(mRNAData),1,any), ]					# remove rows with NA
    if (nrow(mRNAData) == 0) {
        Plot.error.message("The selected cohort has incomplete data for each of your biomarkers.\nNo data is left to plot a heatmap with."); return()
    }

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
    if (is.null(color.range.clamps)) color.range.clamps = c(min(mRNAData), max(mRNAData))
    if (n_remaining_marker>1 & n_remaining_sample >1) {
        plotHeatmap(mRNAData, colcolor, cluster.by.rows, cluster.by.columns, color.range.clamps, output.file, extension = "png")
        plotHeatmap(mRNAData, colcolor, cluster.by.rows, cluster.by.columns, color.range.clamps, output.file, extension = "svg")
    } else {
        #Prepare the package to capture the image file.
        CairoPNG(file=paste(output.file,".png",sep=""),width=as.numeric(imageWidth),height=as.numeric(imageHeight),pointsize=as.numeric(pointsize))

        Plot.error.message("Not enough marker/samples to draw heatmap"); return()
    }

    print("-------------------")
}

plotHeatmap <- function(data, colcolors, cluster.by.rows, cluster.by.columns, color.range.clamps, output.file = "Heatmap", extension = "png") {
    require(Cairo)
    require(gplots)

    dendrogramOptions <- ifelse(cluster.by.rows,
            ifelse(cluster.by.columns, "both", "row"),
            ifelse(cluster.by.columns, "column", "none"))

    pxPerCell <- 15
    hmPars <- list(pointSize = pxPerCell / 1, labelPointSize = pxPerCell / 9)
    if (nrow(data) < 30 || ncol(data) < 30) {
        pxPerCell <- 40
        hmPars <- list(pointSize = pxPerCell / 5, labelPointSize = pxPerCell / 10)
    }

    maxResolution <- 30000
    if (nrow(data) > ncol(data) && nrow(data)*pxPerCell > maxResolution) {
        pxPerCell <- maxResolution/nrow(data)
        hmPars <- list(pointSize = pxPerCell / 1, labelPointSize = pxPerCell / 9)
    } else if (ncol(data)*pxPerCell > maxResolution) {
        pxPerCell <- maxResolution/ncol(data)
        hmPars <- list(pointSize = pxPerCell / 1, labelPointSize = pxPerCell / 9)
    }
    mainHeight <- nrow(data) * pxPerCell
    mainWidth <- ncol(data) * pxPerCell

    if (cluster.by.rows) {
        leftMarginSize <- pxPerCell * log(nrow(data), base = 2)
    } else {
        leftMarginSize <- pxPerCell * 1
    }
    rightMarginSize <- pxPerCell * max(10, max(nchar(rownames(data))))
    topMarginSize <- pxPerCell * 3
    bottomMarginSize <- pxPerCell * max(10, max(nchar(colnames(data))))
    if (cluster.by.columns) {
        topDendrogramHeight <- pxPerCell * log(ncol(data), base = 2)
    } else {
        topDendrogramHeight <- pxPerCell * 1
    }

    topSpectrumHeight <- rightMarginSize

    imageWidth <- leftMarginSize + mainWidth + rightMarginSize
    imageHeight <- topSpectrumHeight + topDendrogramHeight + topMarginSize + mainHeight + bottomMarginSize

    hmCanvasDiv <- list(xLeft = leftMarginSize / imageWidth, xMain = mainWidth / imageWidth, xRight = rightMarginSize / imageWidth,
                        yTopLarge = topSpectrumHeight / imageHeight, yDendrogram = topDendrogramHeight / imageHeight, yTopSmall = topMarginSize / imageHeight,
                        yMain = mainHeight / imageHeight, yBottom = bottomMarginSize / imageHeight)

    if (extension == "svg") {
        CairoSVG(file = paste(output.file,".svg",sep=""), width = imageWidth/200,
                 height = imageHeight/200, pointsize = hmPars$pointSize*0.35)
    } else {
        CairoPNG(file = paste(output.file,".png",sep=""), width = imageWidth,
                 height = imageHeight, pointsize = hmPars$pointSize)
    }
    par(mar = c(0, 0, 0, 0))

    heatmap.2(data,
              ColSideColors = colcolors,
              col = greenred(800),
              breaks = seq(color.range.clamps[1], color.range.clamps[2], length.out = 800+1),
              sepwidth=c(0,0),
              margins=c(0, 0),
              cexRow = hmPars$labelPointSize,
              cexCol = hmPars$labelPointSize,
              dendrogram = dendrogramOptions,
              Rowv = cluster.by.rows,
              Colv = cluster.by.columns,
              scale = "none",
              key = TRUE,
              keysize = 0.001,
              density.info = "histogram", # density.info=c("histogram","density","none")
              trace = "none",
              # 1 is subset color bar, 2 is heatmap, 3 is row dendrogram, 4 is column dendrogram, 5 is color histogram
              lmat = matrix(ncol = 3, byrow = TRUE, data = c(
                  0, 5, 0,
                  0, 4, 0,
                  0, 1, 0,
                  3, 2, 0,
                  0, 0, 0)),
              lwid = c(hmCanvasDiv$xLeft, hmCanvasDiv$xMain, hmCanvasDiv$xRight),
              lhei = c(hmCanvasDiv$yTopLarge, hmCanvasDiv$yDendrogram, hmCanvasDiv$yTopSmall, hmCanvasDiv$yMain, hmCanvasDiv$yBottom))

    legend(x = 1 - hmCanvasDiv$xRight*0.93, y = 1,
           legend = c("Subset 1","Subset 2"),
           fill = c("orange","yellow"),
           bg = "white", ncol = 1,
           cex = topSpectrumHeight * 0.006,
    )


    dev.off()
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
