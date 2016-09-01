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
#BuildHeatmapDataFile
###########################################################################

HeatmapData.build <- 
function
(
input.gexFile,
sample.subset1,
time.subset1,
tissues.subset1,
platform.subset1,
sample.subset2,
time.subset2,
tissues.subset2,
platform.subset2,
genes,
genes.aggregate,
output.dataFile="outputfile"
)
{
	##########################################
	print("-------------------")
	print("BuildHeatmapData.R")
	print("BUILDING HEATMAP DATA")

	allGEXData <- data.frame(read.delim(input.gexFile));
	
	#We have two subsets that we have to filter on using the passed in criteria.
	subset1 <- allGEXData[grep("^subset1",allGEXData$SUBSET),]
	subset2 <- allGEXData[grep("^subset2",allGEXData$SUBSET),]
	
	subset1 <- gexBuilder(
				GEXData = subset1,
				sampleType = sample.subset1,
				timepointType = time.subset1,
				tissueType = tissues.subset1,
				gene.list = genes,
				gene.aggregate = genes.aggregate,
				probe.average = TRUE,
				subsetname.replace = TRUE);
				
	if(nrow(subset2) > 0)
	{
		subset2 <- gexBuilder(
					GEXData = subset2,
					sampleType = sample.subset2,
					timepointType = time.subset2,
					tissueType = tissues.subset2,
					gene.list = genes,
					gene.aggregate = genes.aggregate,
					probe.average = TRUE,
					subsetname.replace = TRUE);				
		
		geneExpressionMatrix <- rbind(subset1,subset2)
	}
	else
	{
		geneExpressionMatrix <- subset1
	}
	
	#We need MASS to dump the matrix to a file.
	require(MASS)	

	finalData <- geneExpressionMatrix
	#Write the final data file.
	# write.matrix(geneExpressionMatrix,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
	print("-------------------")
	##########################################
}





