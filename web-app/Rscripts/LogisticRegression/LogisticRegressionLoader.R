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
	######################################################
	
	######################################################
	#Read the line graph data.
	line.data<-read.delim(input.filename,header=T)
	
	if(length(levels(line.data$X)) < 2)
	{
		stop("||FRIENDLY||After extracting data only one categorical concept was found. Please verify your variable selection and try again.")
	}
	
  #yaxislabels
 	yLabel <- unique(line.data$X)
 	yAxisLabel1 <- paste(yLabel[1],sep="")
 	yAxisLabel0 <- paste(yLabel[2],sep="")
   
	#We need to convert the value column from a factor to a numeric 0 or 1 values
	line.data$X <- factor(line.data$X,labels = c(0,1))
	
	
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
	
	xAxisLabel <- "";

   
	
			xAxisLabel <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept.dependent,perl=TRUE)	
			splitConcept <- strsplit(concept.independent,"\\|");
			splitConcept <- unlist(splitConcept);
			#yLabel <- sub(pattern="^\\\\(.*?\\\\){5}",replacement="",x=splitConcept,perl=TRUE)
 	    #yLabel <- unique(line.data$X)
	        #tempYAxisLabel <- paste(rev(yLabel), collapse= "-")
	        #yAxisLabel1 <- yLabel[1]
 	        #yAxisLabel0 <- yLabel[2]
 	
	######################################################
	#Plotting the line.


	
	#This is the name of the output image file.
	imageFileName <- paste(output.file,".png",sep="")
	
	#This initializes our image capture object.
	CairoPNG(file=imageFileName, width=700, height=700,units = "px")	
	
	#yAxisLabel <- paste("P (",yAxisLabel,")",sep=" ")
	
	
	#Printing actually puts the plot in the image.
	# visreg(logReg.fit,scale="response", xlab=xAxisLabel,ylab=yAxisLabel)
	
   
   
   
   
 	#Printing actually puts the plot in the image.
 	visreg(logReg.fit,scale="response", partial=FALSE, xlab="",ylab="",yaxt="n")
 	
 	#overlap plot with input data
 	par(new=T)   
 	X <- as.numeric(paste(line.data$X)) 
   
 	#plot observed events/non-events (dots across the top or bottom).
 	plot(X~line.data$Y,type="p",axes=F, pch=20,cex = .9,  yaxt="n", xlab=xAxisLabel,ylab="P(Outcome)")
 	axis(2, at=c(0, .2,.4,.6,.8,1),labels=c(yAxisLabel0,".2",".4",".6",".8",yAxisLabel1),las=0)

   
	#Turn of the graphics device to save the image.
	dev.off()
	######################################################
}
createAxisLabel <- function(concept.type,concept,genes)
{
	axisLabel <- ""

	if(concept.type == "CLINICAL") axisLabel <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=concept,perl=TRUE)
	#if(concept.type == "MRNA") axisLabel <- paste(genes,"Gene Expression (zscore)",sep=" ")
	#if(concept.type == "SNP") axisLabel <- "SNP"

	return(axisLabel)
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
