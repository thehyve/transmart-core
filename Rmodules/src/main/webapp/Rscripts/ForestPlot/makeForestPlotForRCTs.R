makeForestPlotForRCTs <- function(mylist, statistic="OR", referencerow=2,fileNameQualifier="",output.file,concept.dependent,concept.independent)
{
  require("rmeta")
  library(Cairo)
  
  
  require("rmeta")
  #If we have a qualifier we need to throw a "_" after the name of the file.
  #If we have a qualifier we need to throw a "_" after the name of the file.
  if(fileNameQualifier != '') fileNameQualifier <- paste('_',fileNameQualifier,sep="");
  
  
  
  #This is the name of the output file for the statistical tests.
  forestPlotResultsFile <- paste("forestPlotTextTable",fileNameQualifier,".txt",sep="")
  
  
  #This is the name of the output file for the statistical tests.
  statisticalSummaryFile <- paste("statisticalSummary",fileNameQualifier,".txt",sep="")
  
  
  numstrata <- length(mylist)
  # make an array "event_total.vec" of the number of people in the event group, in each stratum
  # make an array "unevent_total.vec" of the number of people in the unevent group, in each stratum
  # make an array "event_case.vec" of the number of people in the event group that have the disease,
  # in each stratum
  # make an array "unevent_case.vec" of the number of people in the unevent group that have the disease,
  # in each stratum
  #_______________ default matrix
  #event_case   | event_control
  #unevent_case | unevent_control
  #
  
  event_total.vec <- vector()  #event_total.vec
  unevent_total.vec <- vector() #unevent_total.vec
  event_case.vec <- vector()  #event_case.vec
  event_control.vec <- vector()  #event_control.vec
  unevent_case.vec <- vector() #unevent_case.vec
  unevent_control.vec <- vector() #unevent_control.vec
  if (referencerow == numstrata) { nonreferencerow <- 2 }
  else                   { nonreferencerow <- 1 }
  for (i in 1:numstrata)
  {
    mymatrix <- mylist[[i]]
    print(mymatrix)
    
    unevent_case <- mymatrix[referencerow,1]
    unevent_control <- mymatrix[referencerow,2]
    unevent_total <- unevent_case + unevent_control
    unevent_total.vec[i] <- unevent_total
    unevent_case.vec[i] <- unevent_case
    unevent_control.vec[i]<-unevent_control
    
    event_case <- mymatrix[nonreferencerow,1]
    event_control <- mymatrix[nonreferencerow,2]
    event_total <- event_case + event_control
    event_total.vec[i] <- event_total
    event_case.vec[i] <- event_case
    event_control.vec[i]<-event_control
  }

  
  authors=names(mylist)
  myMH <- meta.MH(event_total.vec, unevent_total.vec, event_case.vec, unevent_case.vec, conf.level=0.95, names=authors,statistic=statistic)
  print(myMH)
  print(summary(myMH))
  
  
  mean <-vector()
  lower<-vector()
  upper<-vector()
  m<-vector()
  l<-vector()
  u<-vector()
  stat_label<-"Odds Ratio"
  if(statistic =="OR"){
  mean<- c(myMH$logOR)
  print("mean")
    print(format(exp(mean)),digits=2)
  print("myMH$logOR")
  print(format(exp(myMH$logOR)),digits=2)
  lower<- mean-c(myMH$selogOR)*2
    #print(format(exp(lower)),digits=2)
  upper<- mean+c(myMH$selogOR)*2
    #print(format(exp(upper)),digits=2)
  m<- c(NA,NA,myMH$logOR,NA,myMH$logMH)
    #print(m)
  l<- m-c(NA,NA,myMH$selogOR,NA,myMH$selogMH)*2
    #print(l)
  u<- m+c(NA,NA,myMH$selogOR,NA,myMH$selogMH)*2
    #print(u)
  } else {
    stat_label<-"Relative Risk"
    mean<- c(myMH$logRR)
    #print(format(exp(or)),digits=2)
    lower<- mean-c(myMH$selogRR)*2
    #print(format(exp(lower)),digits=2)
    upper<- mean+c(myMH$selogRR)*2
    #print(format(exp(upper)),digits=2)
    m<- c(NA,NA,myMH$logRR,NA,myMH$logMH)
    #print(m)
    l<- m-c(NA,NA,myMH$selogRR,NA,myMH$selogMH)*2
    #print(l)
    u<- m+c(NA,NA,myMH$selogRR,NA,myMH$selogMH)*2
    #print(u)
  }

  ######################################################
  #Label the table
  #TO DO
  caseLabel <- "";
  controlLabel <- "";
  
  splitConcept <- strsplit(concept.independent,"\\|");
  splitConcept <- unlist(splitConcept);
  studylabels <- sub(pattern="^\\\\(.*?\\\\){4}",replacement="",x=splitConcept,perl=TRUE)
  studylabels<-sub("\\\\", "", studylabels)
  print("studylabels")
  print(studylabels)
  splitConcept <- strsplit(concept.dependent,"\\|");
  splitConcept <- unlist(splitConcept);
  eventlabels <- sub(pattern="^\\\\(.*?\\\\){4}",replacement="",x=splitConcept,perl=TRUE)
  eventlabels<-sub("\\\\", "", eventlabels)
  print("eventlabels")
  print(eventlabels)
  
  ######################################################
  
  
  tabletext<-cbind(c("","Stratification",myMH$names,NA,"Summary"),
                   c("Independent","(Event Outcome)",paste(event_case.vec,"/",event_total.vec,sep=" "),NA,NA),
                   c("Independent","(Non-Event Outcome)",paste(event_control.vec,"/",event_total.vec,sep=" "),NA,NA),
                   c("Reference","(Event Outcome)",paste(unevent_case.vec,"/",unevent_total.vec,sep=" "), NA,NA),
                   c("Reference","(Non-Event Outcome)",paste(unevent_control.vec,"/",unevent_total.vec,sep=" "), NA,NA),
                   c("",stat_label,format(exp(mean),digits=2),NA,format(exp(myMH$logMH),digits=2)),
                   c("","Est. ( 95% CI )",paste("  (",format(exp(lower),digits=2),"-",format(exp(upper),digits=2),")",sep=" "),NA,NA))

  print(tabletext)
  
  #This initializes our image capture object.
  CairoPNG(file=imageFileName, width=1000, height=500,pointsize=11,units = "px",dpi="auto")
  par(mar=c(2,2,1,1)+0.1)
  forestplot(tabletext,m,l,u,zero=0,align="c",is.summary=c(TRUE,TRUE,rep(FALSE,numstrata),TRUE),summary=TRUE,
             graphwidth=unit(3,"inches"),
             clip=c(log(0.1),log(2.5)), xlog=TRUE,
             col=meta.colors(box="blue",line="darkBlue", summary="blue"))
  
  rng <- par("usr")
  #title(main="My Title") 
  studylabels<-getStudyLabels(concept.independent)
  eventlabels<-getEventLabels(concept.dependent)
  legendtext<-rbind(c(paste("Independent: ",studylabels[1],sep="")),
                    c(paste("Reference: ",studylabels[2],sep="")),
                    c(paste("Event Outcome :",eventlabels[1],sep="")),
                    c(paste("Non-Event Outcome:",eventlabels[2],sep="")))
  
  legend( "bottomleft", legend=legendtext,title="Legend",adj=0,horiz = FALSE,cex=.85 )
  #box("plot", col="red") 
  #mtext("Figure", SOUTH<-1, line=3, adj=1.0,cex=.75, col="blue") 
  #box("figure", col="blue")
  print(par()$mar)
  
 
  dev.off()
  
}
getStudyLabels <-function(concept.independent){
  ######################################################
  #Label the table
  
  splitConcept <- strsplit(concept.independent,"\\|");
  splitConcept <- unlist(splitConcept);
  studylabels <- sub(pattern="^\\\\(.*?\\\\){4}",replacement="",x=splitConcept,perl=TRUE)
  studylabels<-sub("\\\\", "", studylabels)
  print("studylabels")
  print(studylabels)
  return (studylabels)
  
}

getEventLabels<-function(concept.dependent){
  splitConcept <- strsplit(concept.dependent,"\\|");
  splitConcept <- unlist(splitConcept);
  eventlabels <- sub(pattern="^\\\\(.*?\\\\){4}",replacement="",x=splitConcept,perl=TRUE)
  eventlabels<-sub("\\\\", "", eventlabels)
  print("eventlabels")
  print(eventlabels)
  return (eventlabels)
}