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
#BinningFunction
#Bin columns in the output file based on concepts.
###########################################################################

BinningFunction <- 
function(
dataFrame,
binningColumn,
binning.bins = "",
binning.type = "",
binning.manual = FALSE,
binning.binrangestring = "",
binning.variabletype = "",
continuous.concept = ""
)
{
	##########################################
	#Read the input file.
	#dataFrame <- data.frame(read.delim(dataLocation));

	#Add an empty bins column.
	dataFrame$bins <- NA
	
	numberOfBins <- binning.bins
	
	if(!is.numeric(numberOfBins))
	{
		numberOfBins <- as.numeric(numberOfBins)
	}
	
	binningType <- binning.type	
	
	#ESB
	if(binningType == "ESB" && binning.manual == FALSE)
	{
		if(!is.numeric(dataFrame[[binningColumn]])) dataFrame[[binningColumn]] = as.numeric(levels(dataFrame[[binningColumn]]))[as.integer(dataFrame[[binningColumn]])]

		#This is our high value.
		highValue <- max(dataFrame[[binningColumn]])

		#This is our min value.
		lowValue <- min(dataFrame[[binningColumn]])

		#This is the step value for our bins.
		stepvalue <- (highValue - lowValue) / numberOfBins

		#Add all items to the top bin.
		dataFrame$bins = numberOfBins

		#This is the first lower bound.
		binEnd <- highValue - stepvalue

		#Loop over all bins but the last one.
		for(i in seq((numberOfBins-1),1,-1))
		{ 
			#We need all the values which are less than the end to be in the next lowest bin.
			dataFrame$bins[dataFrame[[binningColumn]] <= binEnd] = i

			#This is the ending row number.
			binEnd <- binEnd - stepvalue  
		}
		
	}

	#EDP
	if(binningType == "EDP" && binning.manual == FALSE)
	{
		if(!is.numeric(dataFrame[[binningColumn]])) dataFrame[[binningColumn]] = as.numeric(levels(dataFrame[[binningColumn]]))[as.numeric(dataFrame[[binningColumn]])]
		
		#Add an empty bins column.
		dataFrame$bins <- NA

		#If we are EDP'ing we need to find out how to evenly split the population.
		totalPopulation <- length(dataFrame$PATIENT_NUM)

		#Find the bin size (Rounded up, we will be heavy in the lesser bins.)
		binSize <- ceiling(totalPopulation / numberOfBins)
	
		#Sort on the [[binningColumn]] so our bins align properly.
		dataFrame <- dataFrame[order(dataFrame[[binningColumn]]), ]	
		
		#This is the starting row number.
		binStart <- 1
		binEnd <- 1

		#Loop over all bins but the last one.
		for(i in 1:(numberOfBins-1))
		{
			if(i>1) binStart <- binStart + binSize

			#This is the ending row number.
			binEnd <- binStart + binSize - 1

			#We need to loop and assign binning groups.
			dataFrame$bins[binStart:binEnd] = i

			#If any row in any bin has a value that matches the first row in this bin, add those rows to the previous bin.
			if(i>1) dataFrame$bins[(dataFrame[[binningColumn]]==dataFrame[[binningColumn]][binStart]) & (dataFrame$bins==i)] = i-1
	  
		}

		#The last bin has everybody that is left.
		dataFrame$bins[is.na(dataFrame$bins)] = numberOfBins
		
	}
					
	if(binning.manual == TRUE)
	{
		if(binning.variabletype == "Categorical")
		{
			
			#We assume we get a list of categories with "<>" in between categories, "|" in between bins.
			splitTotalRangeString <- strsplit(binning.binrangestring,"\\|");
			splitTotalRangeString <- unlist(splitTotalRangeString);
			
			#Each entry is a bin.
			for(currentBin in splitTotalRangeString)
			{
				#For each range we have name,low#,high#
				splitRange <- strsplit(currentBin,"<>");
				splitRange <- unlist(splitRange);
				
				binName <- splitRange[1]
				
				splitRange <- splitRange[-1]
				
				for(currentCat in splitRange)
				{
					#Assign everybody in this range to the current bin.
					dataFrame$bins[(dataFrame[[binningColumn]] == currentCat)] = binName					
				}
			}

			#Remove anyone who is NA.
			dataFrame <- dataFrame[!is.na(dataFrame$bins),]							
		}
		
		if(binning.variabletype == "Continuous")
		{
			#Convert the [[binningColumn]] column to a number.
			if(!is.numeric(dataFrame[[binningColumn]])) dataFrame[[binningColumn]] = as.numeric(levels(dataFrame[[binningColumn]]))[as.numeric(dataFrame[[binningColumn]])]
			
			#This is used for a str_extract later.
			library(stringr)
		
			#We assume we get a list of ranges with "|" in between.
			splitTotalRangeString <- strsplit(binning.binrangestring,"\\|");
			splitTotalRangeString <- unlist(splitTotalRangeString);

			#Each entry is a bin.
			for(currentBin in splitTotalRangeString)
			{

			  #For each range we have name,low#,high#
			  splitRange <- strsplit(currentBin,",");
			  splitRange <- unlist(splitRange);

			  #In order to set the bin field, we extract the ranges.
			  binName <- splitRange[1]
			  lowRange <- as.numeric(str_extract(splitRange[2],"-?\\d*\\.?\\d*"))
			  highRange <- as.numeric(str_extract(splitRange[3],"-?\\d*\\.?\\d*"))
				
			  #Assign everybody in this range to the current bin.
			  dataFrame$bins[(dataFrame[[binningColumn]] > lowRange) & (dataFrame[[binningColumn]] <= highRange)] = binName
			  
			}

			#Remove anyone who is NA.
			dataFrame <- dataFrame[!is.na(dataFrame$bins),]
		}		
	}
	
	#For our continuous offerings we get a high/low on the bins to put in the bin name.
	if((binningType == "EDP" && binning.manual == FALSE) || (binningType == "ESB" && binning.manual == FALSE) || (binning.manual == TRUE && binning.variabletype == "Continuous"))
	{
		if(binning.manual)
		{
			#We need to rename the bins based on high/low of each group.
			for(j in 1:numberOfBins)
			{
				#Find the lower value in this bin.
				lowValue <- min(dataFrame[[binningColumn]][dataFrame$bins==paste('bin',j,sep='')])
				
				#Find out which bin we get the max from. Either the next one (Or this one if we are on last bin).
				if(j==numberOfBins)
				{
					highValue <- max(dataFrame[[binningColumn]])
					binUpperSymbol <- " <= "
				}
				else
				{
					highValue <- min(dataFrame[[binningColumn]][dataFrame$bins==paste('bin',j+1,sep='')])
					binUpperSymbol <- " < "
				}
							
				#Create the bin name.
				binName <- paste(lowValue,' <= ',binningColumn,binUpperSymbol,highValue,sep="")		
				
				#Assign the bin name to this bin.
				dataFrame$bins[dataFrame$bins==paste('bin',j,sep='')] = binName
			}		
		}
		else
		{
			#We need to rename the bins based on high/low of each group.
			for(j in 1:numberOfBins)
			{
				#Find the lower value in this bin.
				lowValue <- min(dataFrame[[binningColumn]][dataFrame$bins==j])

				#Find out which bin we get the max from. Either the next one (Or this one if we are on last bin).
				if(j==numberOfBins)
				{
					highValue <- max(dataFrame[[binningColumn]])
					binUpperSymbol <- " <= "
				}
				else
				{
					highValue <- min(dataFrame[[binningColumn]][dataFrame$bins==j+1])
					binUpperSymbol <- " < "
				}
							
				#Create the bin name.
				binName <- paste(lowValue,' <= ',binningColumn,binUpperSymbol,highValue,sep="")		
				
				#Assign the bin name to this bin.
				dataFrame$bins[dataFrame$bins==j] = binName
			}
		}
	}	
	
	#For our categorical binning put together a list of the categories within each group.
	if(binning.manual == TRUE && binning.variabletype == "Categorical")
	{
		#This is the dataframe that we will store the legend information in.
		tempDataFrame <- NULL;
	
		#We assume we get a list of categories with "<>" in between categories, "|" in between bins.
		splitTotalRangeString <- strsplit(binning.binrangestring,"\\|");
		splitTotalRangeString <- unlist(splitTotalRangeString);
		
		#Each entry is a bin.
		for(currentBin in splitTotalRangeString)
		{
			#For each range we have name,low#,high#
			splitRange <- strsplit(currentBin,"<>");
			splitRange <- unlist(splitRange);
			
			inputBinName <- splitRange[1]
			
			#Get the list of unique groupings in the bin.
			uniqueVector <- unique(dataFrame[[binningColumn]][dataFrame$bins==inputBinName])
			
			#Trim the unique vectors so we can print the pretty forms of the concepts.
			f1 <- function(x) sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=x,perl=TRUE)
			f2 <- function(x) sub(pattern="\\\\$", replacement="",x=x,perl=TRUE)
			uniqueVector <- lapply(uniqueVector,f1)
			uniqueVector <- lapply(uniqueVector,f2)
			
			#Create the bin name.
			#binName <- paste(inputBinName,' : ',paste(uniqueVector,collapse=","),sep="")					
			
			#Assign the bin name to this bin.
			#dataFrame$bins[dataFrame$bins==inputBinName] = binName
			tempDataFrame <- rbind(tempDataFrame,data.frame(binname=inputBinName,bincontents=paste(uniqueVector,collapse=",")));
		}
		
		write.table(tempDataFrame,"legend.txt",quote=F,sep="\t",row.names=F,col.names=F)
	}
	##########################################
	
	##########################################
	#After we set the bin column, copy it to [[binningColumn]] and remove bin.
	dataFrame[[binningColumn]] = dataFrame$bins
	dataFrame$bins <- NULL
	
	return(dataFrame)
	##########################################
	
}
