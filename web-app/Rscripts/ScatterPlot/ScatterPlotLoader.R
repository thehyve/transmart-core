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
#ScatterPlot
#This will load our input files into variables so we can run the scatter plot.
###########################################################################

ScatterPlot.loader <- function(
  input.filename,
  output.file ="ScatterPlot",
  concept.dependent = "",
  concept.independent = "",
  concept.dependent.type = "",
  concept.independent.type = "",
  genes.dependent = "",
  genes.independent = "",
  snptype.dependent = "",
  snptype.independent = ""  
  )
 {
 	
	library(plyr)
	library(ggplot2)
	library(Cairo)
	
	#Clean the gene names if they have spaces.
	genes.dependent <- gsub("^\\s+|\\s+$", "",genes.dependent)
	genes.independent <- gsub("^\\s+|\\s+$", "",genes.independent)
	
	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	
	#Make sure the group columns are regarded as factors.
	if("GROUP" %in% colnames(line.data)) line.data$GROUP <- as.factor(line.data$GROUP)
	if("GROUP.1" %in% colnames(line.data)) line.data$GROUP.1 <- as.factor(line.data$GROUP.1)
	
	#If we have two group columns we need to make one text file per "GROUP" which has a linear regression for each of the items in "GROUP.1"
	if("GROUP.1" %in% colnames(line.data)) 
	{
		#This is a list of the distinct groups.
		groupList <- matrix(unique(line.data$GROUP));
		
		#For each of the "GROUPS" we need to call the linear regression on each of the "GROUP.1" values.
		subFunctionForLM <- function(currentGroup, groupedData)
		{
			#Build a list of indexes which represent the records we need to pull for each group.
			currentIndex <- which(line.data$GROUP==currentGroup)
			
			#Pull the records into another object.
			currentGroupingData <- line.data[currentIndex,]
			
			trimmedGroupName <- gsub("^\\s+|\\s+$", "",currentGroup)

			#This is the filename for this group.
			fileName <- paste("LinearRegression_",trimmedGroupName,".txt",sep="")
			
			#Run the lm function on each grouping.
			lapply(split(currentGroupingData,currentGroupingData$GROUP.1),lmPerGroup,"GROUP.1",fileName)
		}
		
		#This calls the first function on each "GROUP" which will call another function on "GROUP.1"
		lapply(groupList,subFunctionForLM,groupedData)
	}
	else if("GROUP" %in% colnames(line.data)) 
	{
		lapply(split(line.data, line.data$GROUP), lmPerGroup,"GROUP","LinearRegression.txt")
	}
	else
	{
		lmPerGroup(line.data,"GROUP","LinearRegression.txt")	
	}
	######################################################
	
	######################################################
	#Plotting the line.		
	
	xAxisLabel <- "";
	yAxisLabel <- "";
	
	xAxisDataTypeUnit <- "";
	yAxisDataTypeUnit <- "";
	
	#Get the labels for the x and y axis.
	if(concept.dependent.type == "CLINICAL")
	{
		yAxisLabel <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept.dependent,perl=TRUE)	
	}
	else if(concept.dependent.type == "MRNA")
	{
		yAxisDataTypeUnit <- "Gene Expression (normalized intensity)";
		yAxisLabel <- paste(genes.dependent,yAxisDataTypeUnit,sep=" ")
	}
	else if(concept.dependent.type == "SNP")
	{
		if(snptype.dependent == "CNV")
		{
			yAxisDataTypeUnit <- "Copy Number"
		}
		else
		{
			yAxisDataTypeUnit <- "Genotype"
		}
		
		yAxisLabel <- paste(genes.dependent,yAxisDataTypeUnit,sep=" ")		
	}
	
	if(concept.independent.type == "CLINICAL")
	{
		xAxisLabel <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept.independent,perl=TRUE)
	}
	else if(concept.independent.type == "MRNA")
	{
		xAxisDataTypeUnit <- "Gene Expression (normalized intensity)"
		xAxisLabel <- paste(genes.independent,xAxisDataTypeUnit,sep=" ")
	}
	else if(concept.independent.type == "SNP")
	{
		if(snptype.independent == "CNV")
		{
			xAxisDataTypeUnit <- "Copy Number"
		}
		else
		{
			xAxisDataTypeUnit <- "Genotype"
		}
		
		xAxisLabel <- paste(genes.independent,xAxisDataTypeUnit,sep=" ")
	}	
	
	#If there is a group column, make sure we set the graph up to use it to change the color of the points, as well as the shape.
	#If there is a GROUP.1 that means we have more than one group column so we need to break it out into different graphs.
	if("GROUP.1" %in% colnames(line.data))
	{	
		#Create the function that actually does the graphing.
		graphSubset <- function(currentGroup,dataToGraph)
		{
			trimmedGroupName <- gsub("^\\s+|\\s+$", "",currentGroup)
			
			CairoPNG(file=paste(output.file,"_",trimmedGroupName,".png",sep=""),width=800,height=800)
			
			#There is a case where we selected one gene and aggregated on it. In this case we only display the gene name once, we don't need the group name.
			if(genes.independent == trimmedGroupName)
			{
				modifiedXAxisLabel <- paste(trimmedGroupName,xAxisDataTypeUnit,sep=" ")
			}
			else
			{
				modifiedXAxisLabel <- paste(genes.independent,trimmedGroupName,xAxisDataTypeUnit,sep=" ")
			}
			
			tmp <- ggplot(dataToGraph[[currentGroup]], aes(X, Y)) 
			tmp <- tmp + theme_bw() 
			tmp <- tmp + geom_point(size = 4) 
			tmp <- tmp + stat_smooth(method="lm", se=FALSE,size=1.25) 
			tmp <- tmp + aes(colour = GROUP.1) 
			tmp <- tmp + aes(shape = GROUP.1) 
			tmp <- tmp + scale_shape_manual(values=1:20)
			tmp <- tmp + scale_colour_brewer("GROUP.1")
			tmp <- tmp + scale_x_continuous(modifiedXAxisLabel) 
			tmp <- tmp + scale_y_continuous(yAxisLabel)
			
			print(tmp)
			dev.off()
		}

		splitData <- split(line.data,line.data$GROUP)
		groupList <- matrix(unique(line.data$GROUP));
		lapply(groupList,graphSubset,splitData)
	}
	else if("GROUP" %in% colnames(line.data)) 
	{
		CairoPNG(file=paste(output.file,".png",sep=""),width=800,height=800)
		
		tmp <- ggplot(line.data, aes(X, Y)) 
		tmp <- tmp + theme_bw() 
		tmp <- tmp + geom_point(size = 4) 
		tmp <- tmp + stat_smooth(method="lm", se=FALSE,size=1.25) 
		tmp <- tmp + aes(colour = GROUP) 
		tmp <- tmp + aes(shape = GROUP) 
		tmp <- tmp + scale_shape_manual(values=1:20)
		tmp <- tmp + scale_x_continuous(xAxisLabel) 
		tmp <- tmp + scale_y_continuous(yAxisLabel)
		
		print (tmp)
		dev.off()
	}
	else
	{
		CairoPNG(file=paste(output.file,".png",sep=""),width=800,height=800)
		
		tmp <- ggplot(line.data, aes(X, Y)) 
		tmp <- tmp + geom_point() 
		tmp <- tmp + stat_smooth(method="lm", se=FALSE) 
		tmp <- tmp + scale_x_continuous(xAxisLabel) 
		tmp <- tmp + scale_y_continuous(yAxisLabel)
		
		print (tmp)		
		dev.off()
	}
	######################################################
}


lmPerGroup <- function(splitData,splitColumn,fileName)
{
	#Get the current groupname so we can use it in the name of the file we write.
	currentGroup <- unique(splitData[[splitColumn]])

	currentGroup <- gsub("^\\s+|\\s+$", "",currentGroup)
	
	#Place the linear model into a variable.
	linearResults <- lm(Y ~ X,data=splitData)
	linearSummary <- summary(linearResults)
	print(linearSummary)
	#If we have a group column we should write the group name to the file so we know which linear regression we are doing.
	if("GROUP" %in% colnames(splitData)) write(paste("name=",currentGroup,sep=""), file=fileName,append=T)
	
	#Write the results of the linear regression to the file.
	write(paste("n=",nrow(splitData),sep=""), file=fileName,append=T)
	write(paste("intercept=",format(linearResults$coefficients[[1]],digits=3),sep=""), file=fileName,append=T)
	write(paste("slope=",format(linearResults$coefficients[[2]],digits=3),sep=""), file=fileName,append=T)
	write(paste("nr2=",format(linearSummary[[8]],digits=3),sep=""), file=fileName,append=T)
	write(paste("ar2=",format(linearSummary[[9]],digits=3),sep=""), file=fileName,append=T)
	
	if(is.null(linearSummary$fstatistic[1]) || is.null(linearSummary$fstatistic[2]) || is.null(linearSummary$fstatistic[3]))
	{
		write("p=NA",file=fileName,append=T)
	}
	else
	{
		write(paste("p=",format(pf(linearSummary$fstatistic[1],linearSummary$fstatistic[2],linearSummary$fstatistic[3],lower.tail=FALSE),digits=3),sep=""), file=fileName,append=T)
	}
}
