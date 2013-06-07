# TOOL acgh-survival-test.R: "Survival test for called copy number data" (Statistical test for survival and called copy number data. The testing is recommended to be performed after running the Identify common regions from called copy number data tool.)
# INPUT regions.tsv: regions.tsv TYPE GENE_EXPRS 
# INPUT META phenodata.tsv: phenodata.tsv TYPE GENERIC 
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

  dat <- read.table('regions.tsv', header=TRUE, sep='\t', as.is=TRUE, check.names=FALSE)
  phenodata <- read.table('phenodata.tsv', header=TRUE, sep='\t', check.names=FALSE)

  first.data.col <- min(grep('^chip\\.', names(dat)), grep('^flag\\.', names(dat)))
  data.info <- dat[,1:(first.data.col-1)]
  calls <- as.matrix(dat[,grep('^flag\\.', colnames(dat))])

  nrcpus=0
  try({
    nrcpus=as.numeric(system("nproc", intern=TRUE))
  }, silent=TRUE);
  if(nrcpus<1) {
    nrcpus=2
  }

  # first try parallel computing
  prob <- TRUE
  try({
    library(CGHtestpar)
    pvs <-  pvalstest_logrank(calls, data.info, dataclinvar=phenodata, whtime=which(colnames(phenodata) == survival), whstatus=which(colnames(phenodata) == status), lgonly=as.integer(test.aberrations), niter=number.of.permutations, ncpus=nrcpus)
    fdrs <- fdrperm(pvs)
    prob <- FALSE
  }, silent=TRUE)
  # if problems, fall back to sequential computing
  if (prob) {
    library(CGHtest)
    pvs <-  pvalstest_logrank(calls, data.info, dataclinvar=phenodata, whtime=which(colnames(phenodata) == survival), whstatus=which(colnames(phenodata) == status), lgonly=as.integer(test.aberrations), niter=number.of.permutations)
    fdrs <- fdrperm(pvs)
  }

  fdrs <- cbind(fdrs, dat[,first.data.col:ncol(dat)])

  options(scipen=10)
  write.table(fdrs, file='survival-test.txt', quote=FALSE, sep='\t', row.names=FALSE, col.names=TRUE)

}

# EOF
