# TOOL acgh-survival-test.R: "Survival test for called copy number data" (Statistical test for survival and called copy number data. The testing is recommended to be performed after running the Identify common regions from called copy number data tool.)
# INPUT acgh.txt: acgh.txt TYPE GENE_EXPRS 
# INPUT META clinical.txt: clinical.txt TYPE GENERIC 
# OUTPUT survival-test.txt: survival-test.txt
# PARAMETER survival: survival TYPE METACOLUMN_SEL DEFAULT Overall survival time (Phenodata column with survival data)
# PARAMETER status: status TYPE METACOLUMN_SEL DEFAULT Survival status (Phenodata column with patient status: alive=0, dead=1)
# PARAMETER number.of.permutations: number.of.permutations TYPE INTEGER DEFAULT 10000 (The number of permutations. At least 10000 recommended for final calculations.)
# PARAMETER test.aberrations: test.aberrations TYPE [1: gains, -1: losses, 0: both] DEFAULT 0 (Whether to test only for gains or losses, or both.) 

# Ilari Scheinin <firstname.lastname@gmail.com>, adapted by Tim Hulsen for use in tranSMART
# 2013-05-08

acgh.survival.test <- function
(
  survival='Overall survival time',
  status='Survival status',
  number.of.permutations=10000,
  test.aberrations=0
)
{

  dat <- read.table('aCGH.txt', header=TRUE, sep='\t', quote='', as.is=TRUE, check.names=FALSE)
  phenodata <- read.table('clinical.txt', header=TRUE, sep='\t', check.names=FALSE)
  
  phenodata_<-reshape(phenodata,v.names=c("CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH"),timevar="CONCEPT_PATH_SHORT",idvar="PATIENT_NUM",direction='wide')
  colnames(phenodata_)<-c("PATIENT_NUM","SUBSET","CONCEPT_CODE_OST","CONCEPT_PATH_SHORT_OST","Overall survival time","CONCEPT_PATH_OST","CONCEPT_CODE_SS","CONCEPT_PATH_SHORT_SS","Survival status","CONCEPT_PATH_SS")
  phenodata_[,"Overall survival time"]<-as.numeric(as.character(phenodata_[,"Overall survival time"]))
  phenodata_[,"Survival status"]<-as.character(phenodata_[,"Survival status"])
  phenodata_[,"Survival status"][is.na(phenodata_[,"Survival status"])] <- 0 
  phenodata_[,"Survival status"][phenodata_[,"Survival status"]!=0] <- 1 
  phenodata_[,"Survival status"]=as.numeric(phenodata_[,"Survival status"])

  first.data.col <- min(grep('^chip\\.', names(dat)), grep('^flag\\.', names(dat)))
  data.info <- dat[,1:(first.data.col-1)]
  calls <- as.matrix(dat[,grep('^flag\\.', colnames(dat))])

  library(CGHtest)
  pvs <-  pvalstest_logrank(calls, data.info, dataclinvar=phenodata_, whtime=which(colnames(phenodata_) == survival), whstatus=which(colnames(phenodata_) == status), lgonly=as.integer(test.aberrations), niter=number.of.permutations)
  fdrs <- fdrperm(pvs)

  fdrs <- cbind(fdrs, dat[,first.data.col:ncol(dat)])

  options(scipen=10)
  write.table(fdrs, file='survival-test.txt', quote=FALSE, sep='\t')

}

# EOF
