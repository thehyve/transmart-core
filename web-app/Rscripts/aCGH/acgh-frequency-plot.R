acgh.frequency.plot <- function
(
  column = 'group'
)
{
  library(Cairo)

  # read the data
  dat       <- read.table('outputfile.txt'  , header=TRUE, sep='\t', quote='"', as.is=TRUE      , check.names=FALSE, stringsAsFactors = FALSE)
  phenodata <- read.table('phenodata.tsv', header=TRUE, sep='\t', quote='"', strip.white=TRUE, check.names=FALSE, stringsAsFactors = FALSE)

  # Determine the groups (NA and '' are discarded)
  groupnames <- unique(phenodata[,column])
  groupnames <- groupnames[!is.na(groupnames)]
  groupnames <- groupnames[groupnames!='']

  # get the data-information columns
  first.data.col <- min(grep('chip', names(dat)), grep('flag', names(dat)))
  data.info      <- dat[,1:(first.data.col-1)]

  # We only need the flag-columns (posible values: [-1,0,1,2] -> [loss,norm,gain,amp])
  calls <- as.matrix(dat[,grep('flag', colnames(dat)), drop=FALSE])

  # Determine 'gain' and 'loss' for each group
  for (group in groupnames) 
  {
      group.samples <- which(phenodata[,column] == group & !is.na(phenodata[,column]))
      group.ids     <- phenodata[group.samples, "PATIENT_NUM"]
      highdimColumnsMatchingGroupIds <- match(paste("flag.",group.ids,sep=""), colnames(calls))
      highdimColumnsMatchingGroupIds <- highdimColumnsMatchingGroupIds[which(!is.na(highdimColumnsMatchingGroupIds))]
      group.calls   <- calls[ , highdimColumnsMatchingGroupIds, drop=FALSE]

      data.info[, paste('gain.freq.', group, sep='')] <- rowSums(group.calls > 0) / ncol(group.calls)
      data.info[, paste('loss.freq.', group, sep='')] <- rowSums(group.calls < 0) / ncol(group.calls)
  }

  # Replace chromosome X with number 23 to get only integer column values
  data.info$chromosome[data.info$chromosome=='X'] <- 23
  data.info$chromosome[data.info$chromosome=='XY'] <- 24
  data.info$chromosome[data.info$chromosome=='Y'] <- 25
  data.info$chromosome[data.info$chromosome=='M'] <- 26
  data.info$chromosome[data.info$chromosome=='[:alpha:]'] <- 0
  data.info$chromosome[data.info$chromosome==''] <- 0
  data.info$chromosome <- as.integer(data.info$chromosome)
  # Order by chromosome and start bp to ensure correct chromosome labels in frequency plots
  data.info <- data.info[with(data.info,order(chromosome,start)),]

  # Helper function to create frequency-plot for 1 group
  FreqPlot <- function(data, group, main = 'Frequency Plot',...) 
  {
    par(mar=c(5,4,4,5) + 0.1)
    cols <- c('blue', 'red')
    names(cols) <- c('gain', 'loss')

    chromosomes <- data$chromosome
    a.freq <- data[,paste('gain', '.freq.', group, sep='')]
    b.freq <- data[,paste('loss', '.freq.', group, sep='')]

    if ('num.probes' %in% colnames(data) & !any(is.na(data$num.probes))) {
      chromosomes <- rep(chromosomes, data$num.probes)
      a.freq <- rep(a.freq, data$num.probes)
      b.freq <- rep(b.freq, data$num.probes)
    }

    plot(a.freq, ylim=c(-1,1), type='h', col=cols['gain'], xlab='chromosomes', ylab='frequency', xaxt='n', yaxt='n', main=main, ...)
    points(-b.freq, type='h', col=cols['loss'])
    abline(h=0)
    abline(v=0, lty='dashed')
    cs.chr = cumsum(table(chromosomes))
    for(i in cs.chr)
      abline(v=i, lty='dashed')

    ax <- (cs.chr + c(0,cs.chr[-length(cs.chr)])) / 2
    lbl.chr <- unique(chromosomes)
    lbl.chr[lbl.chr==0] <- 'U'
    lbl.chr[lbl.chr==23] <- 'X'
    lbl.chr[lbl.chr==24] <- 'XY'
    lbl.chr[lbl.chr==25] <- 'Y'
    lbl.chr[lbl.chr==26] <- 'M'

    # Check if chromosomes (labels) are loaded properly
    if (length(ax) != length(lbl.chr)) {
        stop("||FRIENDLY||There is an error in the chromosome/region data. It may not be loaded properly."); return()
    }

    axis(side=1, at=ax, labels=lbl.chr, las=2)
    axis(side=2, at=c(-1, -0.5, 0, 0.5, 1), labels=c('100 %', ' 50 %', '0 %', '50 %', '100 %'), las=1)
    labels <- c(0.01, 0.05, 0.025, 0.1, 0.25, 0.5, 1)
    axis(side=4, at=-log10(labels) - 1, labels=labels, las=1)
    mtext('gain', side=2, line=3, at=0.5, col=cols['gain'])
    mtext('loss', side=2, line=3, at=-0.5, col=cols['loss'])
  }

  # Create the Plots.
  filename <- paste('frequency-plot','.png',sep='')
  CairoPNG(file=filename, width=1000, height=length(groupnames) * 400)
  par(mfrow = c(length(groupnames),1))
  for (group in groupnames) 
  {
    FreqPlot(data.info, group, paste('Frequency Plot for "', group, '"', sep=''))
  }

  dev.off()
}

