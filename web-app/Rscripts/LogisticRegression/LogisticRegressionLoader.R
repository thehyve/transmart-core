###########################################################################
#LogisticalRegression to determine probability
#This will load our input files into variables so we can run the logistical regression.
###########################################################################

LogisticRegressionData.loader <- function(
	input.filename,
	output.file="LogisticRegression",
	concept.dependent = "",
	concept.independent = "",
	binning.enabled = FALSE,
	binning.variable = "IND",
	binning.manual = FALSE,
	binning.type = '',
	binning.variabletype = ''
  )
{
 	######################################################
	#We need this package for a str_extract when we take text out of the concept.
	library(stringr)
	library(plyr)
	library(ggplot2)
	library(Cairo)
	library(visreg)
	pROC.available <- "pROC" %in% row.names(installed.packages())
	if (pROC.available) library(pROC)
	######################################################
	
	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	
	nDataConcepts <- length(levels(line.data$X))
	if(nDataConcepts != 2)
	{
		stop(paste("||FRIENDLY||After extracting data ",nDataConcepts," categorical concepts were found while 2 concepts are expected. Please verify your variable selection and try again."))
	}
	
	# If (two) categorical concepts are used to define both outcome groups (no binning of numerical or categorical variables),
	# the order of those categorical concepts as specified by the user, determines the mapping to EventOccurred (numeric value 1).
	# If some form of binning is used to define both outcome groups (e.g. "bin1" and "bin2"), the alphabetical order will be used to define the mapping.
 	conceptsData <- sort(unique(line.data$X))

	# Split the concept.dependent argument into seperate concept paths
	conceptpathsDependent <- unlist(strsplit(concept.dependent,"\\|"));
	if (length(conceptpathsDependent) == 2)
	{   # Check if the conceptPaths should determine order for mapping
		# Extract labels from conceptpaths
		conceptLabelsDependent <- unlist(lapply(conceptpathsDependent,extractLabelFromConceptPath))

		# Check if conceptLabels equal data found in line.data$X
		if ( all(sort(conceptLabelsDependent)==conceptsData)) {
		# The order of the dependent concept paths determines which concept needs to be mapped to EventOccured (numeric value 1)
 			conceptsData <- conceptLabelsDependent
		}
	}

	yLabel <- conceptsData
	# The first concept is mapped to EventOccurred (numeric value 1), the second is mapped to 0
 	yAxisLabel1 <- paste(yLabel[1],sep="")
 	yAxisLabel0 <- paste(yLabel[2],sep="")
   
	# We need to convert the value column from a factor to a numeric 0 or 1 values
	# The first concept is mapped to EventOccurred (numeric value 1)
	line.data <- transform(line.data, X = ifelse(X==yAxisLabel1,1,0))
	
	# fit a binomial generalized linear models (glm)
	logReg.fit <- glm(X~Y,data=line.data,family="binomial")
	
	#The filename for the summary stats file.
	coefFileName <- paste("LOGREG_RESULTS",".txt",sep="")
	summaryFileName <- paste("LOGREGSummary",".txt",sep="")
	
	#Get a summary of the LOG REG glm fit
	summaryLogReg <- summary(logReg.fit)
	
	# To test the overall fit of the model by once again treating it as a chi square value. 
	# A chi square of residual  deviance  on number of  degrees of freedom yields the overall model fit p-value . 
	overall.model.pvalue<-1-pchisq(summaryLogReg$deviance,summaryLogReg$df[2])
	
	write("||PVALUES||", file=coefFileName,append=T)
		
	#Write the coefficient values to a file.
 	write(paste("I.p=",format(summaryLogReg$coef['(Intercept)', "Pr(>|z|)"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("I.est=",format(summaryLogReg$coef['(Intercept)', "Estimate"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("I.zvalue=",format(summaryLogReg$coef['(Intercept)', "z value"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("I.std=",format(summaryLogReg$coef['(Intercept)', "Std. Error"],digits=3),sep=""), file=coefFileName,append=TRUE)  
		
 	
 	write(paste("Y.p=",format(summaryLogReg$coef['Y', "Pr(>|z|)"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("Y.est=",format(summaryLogReg$coef['Y', "Estimate"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("Y.zvalue=",format(summaryLogReg$coef['Y', "z value"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("Y.std=",format(summaryLogReg$coef['Y', "Std. Error"],digits=3),sep=""), file=coefFileName,append=TRUE)	
	

 	#write(paste("X.p=",format(summaryLogReg$coef['X', "Pr(>|z|)"],digits=3),sep=""), file=coefFileName,append=TRUE)
 	#write(paste("X.est=",format(summaryLogReg$coef['X', "Estimate"],digits=3),sep=""), file=coefFileName,append=TRUE)	
  	#write(paste("X.zvalue=",format(summaryLogReg$coef['X', "z value"],digits=3),sep=""), file=coefFileName,append=TRUE)		
 	#write(paste("X.std=",format(summaryLogReg$coef['X', "Std. Error"],digits=3),sep=""), file=coefFileName,append=TRUE)	
 	
 	#Write the Deviance Residuals values to a file.
 	write(paste("deviance.resid.min=",format(min(summaryLogReg$deviance.resid),digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("deviance.resid.1Q=",format(quantile(summaryLogReg$deviance.resid,0.25),digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("deviance.resid.med=", format(median(summaryLogReg$deviance.resid),digits=3),sep=""), file=coefFileName,append=TRUE)
 	write(paste("deviance.resid.3Q=",format(quantile(summaryLogReg$deviance.resid,0.75),digits=3),sep=""), file=coefFileName,append=TRUE)  
 	write(paste("deviance.resid.max=", format(max(summaryLogReg$deviance.resid),digits=3),sep=""), file=coefFileName,append=TRUE)  
	write(paste("null.resid=", format(summaryLogReg$null,digits=3),format(summaryLogReg$df[2]+1,digits=3),sep=":"), file=coefFileName,append=TRUE)  
	write(paste("deviance.resid=", format(summaryLogReg$deviance,digits=3), format(summaryLogReg$df[2]),sep=":"), file=coefFileName,append=TRUE)  
	write(paste("overall.model.pvalue=", format(overall.model.pvalue,digits=3),sep=""), file=coefFileName,append=TRUE)   	
 	
	#write the Dev 

	print(summaryLogReg)
	
	write("||END||", file=coefFileName,append=T)
	
	#copy the glm fit summary output in a file
	capture.output(summaryLogReg,file=summaryFileName)
	
	#We need to convert the value column from a factor to a numeric.
	#finalData$VALUE <- as.numeric(levels(finalData$VALUE))[as.integer(finalData$VALUE)]

	######################################################
	#Label the Axis.		
	xAxisLabel <- extractLabelFromConceptPath(concept.independent)

	######################################################
	#Prepare image.

	#This is the name of the output image file.
	imageFileName <- paste(output.file,".png",sep="")
	
	#This initializes our image capture object.
	if (pROC.available) {
		CairoPNG(file=imageFileName, width=1000, height=1000, units="px")
		#par(mfrow = c(2,1), cex=1.3)
		layout(matrix(c(0,1,0,2),2,2,byrow=TRUE), heights=c(1,1), widths=c(1,5))
		par(cex=1.3, lwd=2, cex.axis=1.5, cex.lab=1.5, cex.main=1.5, cex.sub=1.5, mar=c(5,4,4,2)+.1)
	} else {
		CairoPNG(file=imageFileName, width=700, height=700, units="px")
	}

	######################################################
	#Plotting the line.
	
	#yAxisLabel0 <- paste("P(",yAxisLabel0,")",sep=" ")
	#yAxisLabel1 <- paste("P(",yAxisLabel1,")",sep=" ")
	
 	#Printing actually puts the plot in the image.
 	visreg(logReg.fit,scale="response", partial=FALSE, xlab="",ylab="",yaxt="n")
 	
 	#overlap plot with input data
 	par(new=T)   
 	X <- as.numeric(paste(line.data$X)) 
   
	#plot observed events/non-events (dots across the top or bottom).
	plot(X~line.data$Y,type="p",axes=F, pch=20,cex = .9,  yaxt="n", xlab=xAxisLabel,ylab="P(Outcome)")
	axis(2, at=c(0, .2,.4,.6,.8,1),labels=c(yAxisLabel0,".2",".4",".6",".8",yAxisLabel1),las=2)

	if (pROC.available) {
		# Create ROC curve
		roc.curve <- roc(X~Y,data=line.data)
		par(pty="s", asp=1)
		plot.roc(roc.curve, percent=FALSE, auc.polygon=TRUE, max.auc.polygon=TRUE, grid=TRUE, print.auc=TRUE, show.thres=TRUE, print.auc.cex=1.5)

		# Alternative ROC curve creation which for the univariate case is identical to the direct method above
		# Create ROC curve from probabilities extracted from regression model
		#predicted.prob <- predict(logReg.fit, type=c("response"))
		#roc.curve.prob <- roc(line.data$X,predicted.prob)
		#plot(roc.curve.prob, percent=FALSE, auc.polygon=TRUE, max.auc.polygon=TRUE, grid=TRUE, print.auc=TRUE, show.thres=TRUE)
	}

	#Turn of the graphics device to save the image.
	dev.off()
	######################################################
}

createAxisLabel <- function(concept.type,concept,genes)
{
	axisLabel <- ""

	if(concept.type == "CLINICAL") axisLabel <- extractLabelFromConceptPath(concept)
	#if(concept.type == "MRNA") axisLabel <- paste(genes,"Gene Expression (zscore)",sep=" ")
	#if(concept.type == "SNP") axisLabel <- "SNP"

	return(axisLabel)
}

extractLabelFromConceptPath <- function(concept.path)
{   # Retrieve last part of concept path
	# Assume format of path string is "\\" { <subpath> "\\" }+
	# Remove double backslash at the end
	strippedPath <- sub(pattern="\\\\$",replacement="",x=concept.path)
	label <- sub(pattern="^\\\\(.*\\\\)+",replacement="",x=strippedPath)
	return(label)
}

createSummaryCoef<-function(a){
	coef(summary(a))->lo
	a<-colnames(lo)
	b<-rownames(lo)
	c<-length(a)
	e<-character(0)
	r<-NULL
	for (x in (1:c)){
		d<-rep(paste(a[1:c],b[x],sep=" "))
		e<-paste(c(e,d))
		t<-lo[x,]
		r<-c(r,t)
		names(r)<-e
	}
	return(r)
 }
