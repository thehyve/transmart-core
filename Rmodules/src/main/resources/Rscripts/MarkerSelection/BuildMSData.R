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
#BuildScatterDataFile
#Parse the i2b2 output file and create input files for a running a CMS.
###########################################################################

MSData.build <- 
function
(
input.dataFile,
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
	print("BuildCMSData.R")
	print("BUILDING CMS DATA")
	
	#Call a function to gather the gene expression data and filter the two subsets.
	geneExpressionMatrix <- gexSubsetBuilder(
								GEXFile = input.dataFile,
								sample.subset1 = sample.subset1,
								time.subset1 = time.subset1,
								tissues.subset1 = tissues.subset1,
								platform.subset1 = platform.subset1,
								sample.subset2 = sample.subset2,
								time.subset2 = time.subset2,
								tissues.subset2 = tissues.subset2,
								platform.subset2 = platform.subset2,
								gene.list = genes,
								gene.aggregate = genes.aggregate							
								)
	
	#Pull out a subset of columns we are interested in.
	geneExpressionMatrix <- geneExpressionMatrix[c('PATIENT.ID','ZSCORE','PROBE.ID','GENE_SYMBOL','SUBSET','ASSAY.ID')]
	
	#Trim the probe.id field.
	geneExpressionMatrix$GENE_SYMBOL <- gsub("^\\s+|\\s+$", "",geneExpressionMatrix$GENE_SYMBOL)	
	
	#Pull the columns we are interested in out of the data.
	finalFrame <- geneExpressionMatrix[c('PATIENT.ID','ZSCORE','PROBE.ID','GENE_SYMBOL','SUBSET')]	
		
	colnames(finalFrame) <- c('PATIENT.ID','VALUE','PROBE.ID','GENE_SYMBOL','SUBSET')

	#We need MASS to dump the data frame to a file.
	require(MASS)	
	
	#Write the final data file.
	#write.matrix(finalFrame,output.dataFile,sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
    print("-------------------")
	##########################################
}
