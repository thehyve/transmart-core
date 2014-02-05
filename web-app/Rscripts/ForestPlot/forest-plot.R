forest.plot.or <- function(m0 = NULL, n0 = NULL, m1 = NULL, n1 = NULL, authors,
                           group.labels = c('Group 0', 'Group 1'),
                           log.scale = standard.or.plot,
                           meta.ci = rep(NA,3),
                           study.categories = numeric(0),
                           category.labels = character(0),
                           categories.ci = matrix(NA,length(category.labels),3),
                           studies.ci = matrix(NA,length(authors),3),
                           plot.categories.ci = rep(T,length(category.labels)),
                           categories.indent = T,
                           plot.lim = matrix(c(0,10,.2,5),ncol=2,byrow=T)[1+log.scale,],
                           xlim = c(0,100),ylim=c(0,diff(xlim)*par()$fin[2]/par()$fin[1]),
                           authors.spaces = 20,
                           nN.spaces = 20,
                           OR.plot.spaces = 20,
                           OR.num.spaces = 20,
                           ci.spaces = c(5,5),
                           J=0.5, test = F, na.rm = F,
                           na.string = "n/a",
                           tick.lim = T,
                           ref.vline.at = 1,
                           add.ticks.at = ref.vline.at,
                           tick.pch = 17,
                           dots.prop2n = F, f.radius=0.5,
                           cex = 1-0.3*length(grep("^win\\.metafile\\:", names(dev.cur()))), circles.cex = cex,
                           or.side.labels = character(0), or.side.labels.cex=cex,
                           blank.after.category.label = F,
                           blank.between.categories = T,
                           blank.before.category.subtotal = F,
                           blank.after.total = plot.xaxis,
                           meta.ci.pch = 15,
                           category.subtotal.pch = meta.ci.pch,
                           ci.txt = "Odds Ratio (95% CrI)",
                           study.txt = "Trial",
                           ratios.gr1.over.gr0 = T,
                           or.ncharacters = 4,
                           or.min.ndecimals = 0,
                           standard.or.plot = T,
                           show.nN.indiv = standard.or.plot, show.nN.tot=F,
                           print.or.ci = T,
                           report.meta.ci = standard.or.plot,
                           sort.by.desc.samplesize = F,
                           placebo.index = seq(along=m1),
                           plot.xaxis = F,
                           reverse.printed.columns.order = F,
                           nN.labels = "n/N"
                           )                          
{
  # Version 1.4 (March 2013)
  #
  # Function developed by 
  # Lawrence Joseph and Patrick Belisle
  # Division of Clinical Epidemiology
  # Montreal General Hospital
  # Montreal, Qc, Can
  #
  # patrick.belisle@clinepi.mcgill.ca
  # http://www.medicine.mcgill.ca/epidemiology/Joseph/PBelisle/forest-plot.html
  #
  # Please refer to our webpage for details on each argument.

  
  # Useful functions within this fct ----------------------------------------------------------

  Round <- function(x, len, min.ndecimals)
  {
    if (is.numeric(x))
    {
      x.int <- floor(x)
      x.fraction <- x - x.int

      lx.int <- nchar(x.int)

      complete.with.fraction <- (min.ndecimals > 0) | (lx.int < (len-1))
      ndigits2complete.with <- pmax(min.ndecimals, (len-1)-lx.int)
    
      x <- x.int

      for (i in seq(along=ndigits2complete.with))
      {
        x.fraction[i] <- round(x.fraction[i], ndigits2complete.with[i])
        if (x.fraction[i] == 1)
        {
           x.int[i] <- x.int[i] + ifelse(x.int[i]>=0, 1, -1)
        }
  
        x[i] <- paste(c(x.int[i], ".", substring(x.fraction[i], 3)), collapse="")
      }
  
      # Complete with 0's the numbers that are still too short
  
      lx <- nchar(x)
      for (i in seq(along=lx)[lx<len])
      {
        x[i] <- paste(c(x[i],rep(0, len-lx[i])), collapse="")
      }
  
      for (i in seq(along=x))
      {
        lx <- nchar(x[i])
        if (substring(x[i], lx, lx) == ".") x[i] <- substring(x[i], 1, lx-1)
      }
    }

    x
  }

  indexc <- function(char1, text.vector)
  {
          w <- NULL
          for(t in text.vector)
          {
                  t.letters <- substring(t, seq(nchar(t)), seq(nchar(t)))
                  w <- c(w, match(char1, t.letters)[1])
          }
          w[is.na(w)] <- 0
          w
  }

  or <- function(m1,n1,m0,n0, gr1.over.gr0=T, J=0)
  {
    or <- (m1+J)/(n1-m1+J) / ( (m0+J)/(n0-m0+J) )
    if (!gr1.over.gr0) or <- 1/or
    or
  }

  or.bayesianci <- function(m1,n1,m0,n0, gr1.over.gr0=T, level=.95, M=100000, J=0)
  {
    #
    ci.lower <- rep(NA, length(m1))
    ci.upper <- rep(NA, length(m1))
    l <- (1+level)/2
    l <- c(1-l,l)

    for (i in which(!is.na(m1)))
    {
      if (gr1.over.gr0)
      {
        a1 <- rbeta(M, m1[i]+J, n1[i]-m1[i]+J)
        a0 <- rbeta(M, m0[i]+J, n0[i]-m0[i]+J)
      }
      else
      {
        a0 <- rbeta(M, m1[i]+J, n1[i]-m1[i]+J)
        a1 <- rbeta(M, m0[i]+J, n0[i]-m0[i]+J)
      }

      or <- a1/a0/((1-a1)/(1-a0))
      ci <- quantile(or, probs=l)
      ci.lower[i] <- ci[1]
      ci.upper[i] <- ci[2]
    }
  
    ci <- matrix(c(ci.lower, ci.upper), ncol=2)
    ci
  }

  hline <- function(x1,x2,xlim,y)
  {
    if (!any(is.na(c(x1,x2))))
    {
      x <- sort(c(x1,x2))
      x2 <- x[2]
      x1 <- x[1]
    
      l.arrow <- F
      r.arrow <- F
      if (x2 > xlim[2])
      {
        r.arrow <- T
        x2 <- xlim[2]
      }
      if (x1 < xlim[1])
      {
        l.arrow <- T
        x1 <- xlim[1]
      }
  
      points(c(x1,x2),rep(y,2),type='l')

      if (l.arrow | r.arrow)
      {
        arrows(x1, y, x2, y, code=as.numeric(l.arrow + 2*r.arrow), length=.1)
      }
    }
  }
  
  Rearranged.ci.matrix <- function(m, matrix.name)
  {
    if (ncol(m) != 3)
    {
      msg <- paste(c("Matrix", matrix.name, "MUST have three columns, which is not the case."), collapse=" ") 
      stop(msg)
    }
    
    complete.rows <- which(apply(!is.na(m), 1, sum) == 3)
    tmp.matrix <- m[complete.rows,]
    
    columns.order.is.fine <- F
    o <- c(1, 2, 3)
    cond <- tmp.matrix[,o[1]] >= tmp.matrix[,o[2]] & tmp.matrix[,o[1]] <= tmp.matrix[,o[3]]
    if (all(cond))
    {
      columns.order.is.fine <- T  
    }
    else
    {
      o <- c(2, 1, 3)
      cond <- tmp.matrix[,o[1]] >= tmp.matrix[,o[2]] & tmp.matrix[,o[1]] <= tmp.matrix[,o[3]]
      if (all(cond)) columns.order.is.fine <- T
    }
    
    if (!columns.order.is.fine)
    {
      msg <- paste(c("Columns in matrix", matrix.name, "seem to be in a odd/unexpected order. Please enter median in first column, followed by LCL and UCL."), collapse=" ") 
      stop(msg)
    }
    
    rearranged <- any(o!=c(1,2,3))
    
    if (rearranged) m <- m[,o]
    
    list(m=m, rearranged=rearranged, o=o)
  }

  # --- End of functions' definitions -----------------------------------------------
  
  w <- which(m0 > n0)
  if (length(w))
  {
    msg <- paste("Values in numerators (m0) must be lower - or equal - than values in denominators (n0): please check components", paste(w, collapse=', '))
    stop(msg)
  }
  
  w <- which(m1 > n1)
  if (length(w))
  {
    msg <- paste("Values in numerators (m1) must be lower - or equal - than values in denominators (n1): please check components", paste(w, collapse=', '))
    stop(msg)
  }  
  
  nplots <- 1
  
  # Rearrange studies.ci in the expected order (Median, lowerCL, upperCL)
  if (any(!is.na(studies.ci)))
  {
    tmp <- Rearranged.ci.matrix(studies.ci, "studies.ci")
    if (tmp$rearranged) studies.ci <- tmp$m
  }
  
  categories.ci.orig <- categories.ci
  if (any(!is.na(categories.ci)))
  {
    if (is.character(categories.ci)) categories.ci <- matrix(as.numeric(categories.ci), nrow=nrow(categories.ci))
    # Rearrange categories.ci so that columns are in expected order, that is, (median, LCL, UCL)
    tmp <- Rearranged.ci.matrix(categories.ci, "categories.ci")
    if (tmp$rearranged)
    {
      categories.ci <- tmp$m
      categories.ci.orig <- categories.ci.orig[,tmp$o]
    }
  }
  
  printed.columns.order <- c(1, 2)
  if (reverse.printed.columns.order) printed.columns.order <- rev(printed.columns.order)
  

  show.nN.indiv <- standard.or.plot & show.nN.indiv
  show.nN.tot   <- show.nN.indiv & show.nN.tot

  if (standard.or.plot && (length(m1) != length(n1) || length(m0) != length(n0) || length(placebo.index) != length(n1) || max(placebo.index) != length(n0)))
  {
    stop("Lengths of vectors placebo.index, m1, n1 must be the same and max(index)=length(m0).\n")
  }


  if (standard.or.plot)
  {
    m0.tot <- sum(m0, na.rm=T)
    n0.tot <- sum(n0, na.rm=T)

    m0 <- m0[placebo.index]
    n0 <- n0[placebo.index]

    if (sort.by.desc.samplesize)
    {
      ssize <- n0 + n1
      o <- order(ssize, decreasing=T)
      m0 <- m0[o]
      m1 <- m1[o]
      n0 <- n0[o]
      n1 <- n1[o]
      authors <- authors[o]
      studies.ci <- studies.ci[o,]
      placebo.index <- placebo.index[o]
    }
  }

  # ----------------------------------------------------------------------------
  
  n.textlines.top <- 2
  n.blanks.top <- 1
  n.textlines.bottom <- 2 + report.meta.ci
  n.blanks.bottom <- 1
  
  blank.after.total.wt <- 0.5

  if (!test && standard.or.plot && any(is.na(meta.ci)) && report.meta.ci)
  {
    cat("meta.ci not given: continue? (y/n) ")
    continue <- readline()
    if (continue != "y") stop("Abort.\n")
  }
  
  n.categories <- 0
  if (length(study.categories) || length(category.labels) || length(categories.ci) || length(plot.categories.ci))
  {
    n.categories <- length(category.labels)

    if (length(study.categories) != length(m1) && standard.or.plot) stop("study.categories and m1 must be of equal length.\n")
    if (nrow(categories.ci) != n.categories) stop("nrow(categories.ci) must be equal to length(category.labels)\n")
    if (length(plot.categories.ci) != n.categories) stop("plot.categories.ci and category.labels must be of same length\n")
  }
    
  if (nrow(studies.ci) != length(authors)) stop("nrow(studies.ci) must be equal to length(authors).\n")

  
  if (na.rm && standard.or.plot)
  {
    nas <- is.na(m0) & is.na(m1)
    if (any(nas))
    {
       m0 <- m0[!nas]
       m1 <- m1[!nas]
       n0 <- n0[!nas]
       n1 <- n1[!nas]
       authors <- authors[!nas]
       if (n.categories) study.categories <- study.categories[!nas]
    }
  }

  if (n.categories)
  {
    tmp <- sort(unique(study.categories))
    if (length(tmp) != n.categories || !all(tmp==seq(n.categories)))
    {
      stop("Not all categories are present in data set (values of study.categories must cover the range 1..#categories [after removal of na's in m0,m1,n0,n1 if na.rm=T])")
    }
    
    # sort data by category (study.categories)
    
    j <- seq(along=study.categories) + (length(study.categories)+1)*(study.categories-1)
    k <- order(j)
    
    if (!all(k==seq(along=k)) && standard.or.plot)
    {
      m0 <- m0[k]
      n0 <- n0[k]
      m1 <- m1[k]
      n1 <- n1[k]
      authors <- authors[k]
      study.categories <- study.categories[k]
      studies.ci <- studies.ci[k,]
    }
  } 

  # Transform plot.lim in the chosen scale
  
  if (log.scale) plot.lim <- log(plot.lim)

  if (show.nN.indiv && standard.or.plot)
  {
    # Total number of events

    m1.tot <- sum(m1, na.rm=T)
    n1.tot <- sum(n1, na.rm=T)
  
    sample.size <- as.numeric(n0+n1)

    # ... put in character string ...
    fraction0 <- paste(m0.tot, '/', n0.tot, sep='')
    fraction1 <- paste(m1.tot, '/', n1.tot, sep='')
  
    fractions0 <- paste(m0, '/', n0, sep='')
    fractions1 <- paste(m1, '/', n1, sep='')
    fractions0[is.na(m0)] <- na.string
    fractions1[is.na(m1)] <- na.string
  }
  
  if (n.categories && show.nN.indiv && standard.or.plot)
  {
    m0.categorytot <- rep(NA, n.categories)
    m1.categorytot <- rep(NA, n.categories)
    n0.categorytot <- rep(NA, n.categories)
    n1.categorytot <- rep(NA, n.categories)
   
    for (i in seq(n.categories))
    {
      m0.categorytot[i] <- sum(m0[study.categories==i], na.rm=T)
      m1.categorytot[i] <- sum(m1[study.categories==i], na.rm=T)
      n0.categorytot[i] <- sum(n0[study.categories==i], na.rm=T)
      n1.categorytot[i] <- sum(n1[study.categories==i], na.rm=T)    
    }

    fraction04category <- paste(m0.categorytot, '/', n0.categorytot, sep='')
    fraction14category <- paste(m1.categorytot, '/', n1.categorytot, sep='')
  }


  nstudies <- length(authors)
  n.textlines <- nstudies + n.textlines.top + n.blanks.top + n.textlines.bottom + n.blanks.bottom + n.categories*(1+blank.after.category.label) + sum(plot.categories.ci)*(1+blank.before.category.subtotal)*(n.categories>0)+(n.categories>0)*(n.categories-1)*blank.between.categories + blank.after.total*blank.after.total.wt

  if (n.categories)
  {
    n.categories.ci.plottedbefore <- c(0, cumsum(plot.categories.ci[-length(plot.categories.ci)]))

    ylinenumber4categorylabel <- rep(NA, n.categories)
    ylinenumber4categorysubtotal <- rep(NA, n.categories)
    for (i in seq(n.categories))
    {
      ylinenumber4categorylabel[i] <- n.textlines.top + n.blanks.top + (i-1)*(1+blank.after.category.label+blank.between.categories) + n.categories.ci.plottedbefore[i]*(1+blank.before.category.subtotal) + sum(study.categories<i) + 1

      ylinenumber4categorysubtotal[i] <- ylinenumber4categorylabel[i] + blank.after.category.label + blank.before.category.subtotal + sum(study.categories==i) + 1
    }

    ylinenumber <- n.textlines.top + n.blanks.top + seq(along=authors) + study.categories*(1+blank.after.category.label) + (study.categories-1)*blank.between.categories+(1+blank.before.category.subtotal)*(n.categories.ci.plottedbefore[study.categories])
  }
  else
  {
    ylinenumber <- n.textlines.top + n.blanks.top + seq(along=authors)
  }

  #


  nN.allowed.spaces <- nplots*(2 + 2*nN.spaces)*show.nN.indiv
  total.spaces <- authors.spaces + nN.allowed.spaces + nplots*(ci.spaces[1] + OR.plot.spaces + print.or.ci*(ci.spaces[2]+OR.num.spaces))
  xunit <- diff(xlim)/total.spaces

  yunit <- diff(ylim)/n.textlines

  # Prepare plot -----------------------------------------

  plotlim0 <- (authors.spaces+ci.spaces[1]+nN.allowed.spaces)*xunit
  plotlim1 <- plotlim0 + OR.plot.spaces*xunit
  plotlim <- c(plotlim0,plotlim1)

  x1 <- plot.lim[1]
  x2 <- plot.lim[2]
  z1 <- plotlim0
  z2 <- plotlim1
  
  b <- (z2-z1)/(x2-x1)
  a <- (z1*x2-z2*x1)/(x2-x1)
  
  #
  
  if (test)
  {
    or.ci <- matrix(plot.lim*c(.9,1.1), ncol=2, nrow=nstudies, byrow=T)
    # multiplying factor is used to have arrows displayed (as CI will be out of bounds), 
    # thus making the whole plot-region easy to figure
    if (log.scale) or.ci <- exp(or.ci)

    plot.limits <- c(authors.spaces, 1, nN.spaces, 1, nN.spaces, ci.spaces[1], OR.plot.spaces)
    if (print.or.ci) plot.limits <- c(plot.limits, ci.spaces[2], OR.num.spaces)

    plot.limits <- xunit*cumsum(plot.limits) 
    or.ci[is.na(n0)|is.na(n1)|is.na(m0)|is.na(m1),] <- NA
    Studies.ci <- studies.ci

    or.pointestimate <- or(m1,n1,m0,n0, gr1.over.gr0=ratios.gr1.over.gr0, J=J)
  }
  else
  {
    or.pointestimate <- studies.ci[,1]
    or.ci <- studies.ci[,-1,drop=F]

    # Computation of point estimates for values not given by user
    na.ci <- is.na(or.pointestimate)
    if (any(na.ci))
    {
      which.na <- which(na.ci)
      or.pointestimate[which.na] <- or(m1[which.na], n1[which.na], m0[which.na], n0[which.na], gr1.over.gr0=ratios.gr1.over.gr0, J=J)
    }

    # Computation of ci for values not given by user
    na.ci <- apply(is.na(or.ci), 1, any)
    if (any(na.ci))
    {
      which.na <- which(na.ci)
      or.ci[which.na,] <- or.bayesianci(m1[which.na], n1[which.na], m0[which.na], n0[which.na], gr1.over.gr0=ratios.gr1.over.gr0, J=J)
    }

    Studies.ci <- cbind(or.pointestimate, or.ci)
  }
  
  #

  if (log.scale)
  {
    x0 <- a + b*log(or.ci[,1])
    x1 <- a + b*log(or.ci[,2])
    xp <- a + b*log(or.pointestimate)
  }
  else
  {
    x0 <- a + b*or.ci[,1]
    x1 <- a + b*or.ci[,2]
    xp <- a + b*or.pointestimate
  }
  
  # Confidence intervals in numbers

  ci <- Round(or.pointestimate, or.ncharacters, or.min.ndecimals)
  ci.l <- Round(or.ci[,1], or.ncharacters, or.min.ndecimals)
  ci.u <- Round(or.ci[,2], or.ncharacters, or.min.ndecimals)

  ci <- paste(ci, ci.l, sep=' (')
  ci <- paste(ci, ci.u, sep=' to ')
  ci <- paste(ci, ')', sep='')
  ci[indexc("N",ci)>0] <- "-"

  # Do the plots

  plot(-100,-100,xlim=xlim,ylim=ylim, xlab="",ylab="",type="n",axes=F,col=1)
  
  if (show.nN.indiv)
  {
    if (reverse.printed.columns.order)
    {
      fractions.left  <- fractions1
      fractions.right <- fractions0
    }
    else
    {
      fractions.left  <- fractions0
      fractions.right <- fractions1
    }
  }
  

  for (i in 1:nstudies)
  {
    # was: y <- (3+nstudies-i+1)*yunit
    y <- yunit*(n.textlines-ylinenumber[i])
    text(0, y, authors[i], adj=0, cex=cex)

    if (show.nN.indiv)
    {
      text((authors.spaces+1+nN.spaces)*xunit,  y, fractions.left[i], adj=1, cex=cex)
      text((authors.spaces+2+2*nN.spaces)*xunit,y, fractions.right[i], adj=1, cex=cex)
    }

    if (print.or.ci)
    {
      # OR ci
      text((authors.spaces+sum(ci.spaces)+(2+2*nN.spaces)*show.nN.indiv+OR.plot.spaces)*xunit, y, ci[i], adj=0, cex=cex)
    }
  }

  if (length(nN.labels))
  {
    nN.labels <- paste('\n', nN.labels, sep='')
    group.labels <- paste(group.labels, nN.labels, sep='')
  }

  y <- (n.textlines-2)*yunit
  text(0, y, study.txt, adj=0, cex=cex)

  if (show.nN.indiv)
  {
    text((authors.spaces+(1+1*nN.spaces))*xunit,y+yunit, group.labels[printed.columns.order[1]], adj=1, cex=cex)
    text((authors.spaces+(2+2*nN.spaces))*xunit,y+yunit, group.labels[printed.columns.order[2]], adj=1, cex=cex)
  }

  if (print.or.ci)
  {
    text((authors.spaces+sum(ci.spaces)+(2+2*nN.spaces)*show.nN.indiv+OR.plot.spaces)*xunit, y, ci.txt, adj=0, cex=cex)
  }

  text((authors.spaces+ci.spaces[1]+(2+2*nN.spaces)*show.nN.indiv+OR.plot.spaces/2)*xunit, y, ci.txt, adj=0.5, cex=cex)

  if (dots.prop2n)
  {
    xcircle <- seq(from=-1,to=1,length=200)
    ycircle <- sqrt(1-xcircle^2)
  }

  for (r in seq(along=x0))
  {
    y <- (n.textlines-ylinenumber[r]) * yunit
    hline(x0[r],x1[r],plotlim,y)

    if (dots.prop2n)
    {
      radius <- f.radius*yunit*sample.size[r]/max(sample.size, na.rm=T)
      if (!is.na(radius))
      {
        symbols(xp[r], y, circles=radius, add=T,inches=F)
        polygon(xp[r]+xcircle*radius, y+ycircle*radius, density=-1)
        polygon(xp[r]+xcircle*radius, y-ycircle*radius, density=-1)
      }
    }
    else
    {
      if (xp[r] <= plotlim1 && xp[r] >= plotlim0) points(xp[r],y,type='p', cex=circles.cex) 
    }
  }


  # vertical line @ ref.vline.at
  if (length(ref.vline.at) == 1)
  {
    if (log.scale) ref.vline.at <- log(ref.vline.at)
    x <- a + b*ref.vline.at
    y <- c(n.textlines.top + n.blanks.top + (n.categories>0) + 1, n.textlines - n.textlines.bottom + report.meta.ci)
    y <- (n.textlines-y)*yunit
    y <- y + 0.5*c(1,-1)*yunit
    y.xaxis <- y[2]
    points(rep(x,2), y, type='l')
  }

  if (log.scale) add.ticks.at <- log(add.ticks.at)
  if (tick.lim) add.ticks.at <- c(plot.lim, add.ticks.at)
  x <- add.ticks.at
  x <- x[x>=plot.lim[1]& x<=plot.lim[2]]
  xx <- a+b*x
  yx <- !report.meta.ci * yunit

  if (log.scale) x <- exp(x)

  ytick <- yunit
  x.text <- x
  text(xx, yx, x.text, adj=0.5, cex=cex)
  points(xx[x!=1], rep(ytick,sum(x!=1)), pch=tick.pch, adj=.5)
  
  # x-axis
  if (plot.xaxis)
  {
    points(xx[x!=1], rep(y.xaxis,2), type='l')
  }

  # Reorganize meta.ci in the expected order [1st column=point estimate, 2nd=2.5% crI limits, 3rd=97.5% crI limit]
  meta.ci <- sort(meta.ci)[c(2,1,3)]

  if (report.meta.ci)
  {
    # Turn CI into character string
    meta.ci.char <- character(length(meta.ci))
    if (all(!is.na(meta.ci)))
    {
      for (i in seq(along=meta.ci.char))
      {
        meta.ci.char[i] <- Round(meta.ci[i], or.ncharacters, or.min.ndecimals)
      }
    }
    else meta.ci.char <- rep("NA", 3)
    meta.ci.char <- paste(meta.ci.char[1], ' (', meta.ci.char[2], ' to ', meta.ci.char[3], ')', sep='')

    y <- (n.textlines.bottom - 1 + blank.after.total*blank.after.total.wt)*yunit
    x.Total <- ifelse(length(category.labels)>0, -categories.indent*xunit, 0)
    text(x.Total, y, 'Total', adj=0, cex=cex)
    text((authors.spaces+ci.spaces[1]+2*(1+nN.spaces)*show.nN.indiv+OR.plot.spaces+print.or.ci*ci.spaces[2])*xunit, y, meta.ci.char, adj=0, cex=cex)
    # for 'n/N' fractions displayed on 'Total' line, at the bottom of the table
    if (show.nN.tot)
    {
      if (reverse.printed.columns.order)
      {
        fraction.left  <- fraction1
        fraction.right <- fraction0
      }
      else
      {
        fraction.left  <- fraction0
        fraction.right <- fraction1
      }

      text((authors.spaces+1+1*nN.spaces)*xunit, y, fraction.left,  adj=1, cex=cex)
      text((authors.spaces+2+2*nN.spaces)*xunit, y, fraction.right, adj=1, cex=cex)
    }
    
    
    if (!test)
    {
      tmp.meta.ci <- meta.ci
      if (log.scale) tmp.meta.ci <- log(tmp.meta.ci)
      tmp.meta.ci <- a+b*tmp.meta.ci
      points(tmp.meta.ci[1],y,type='p',pch=meta.ci.pch, adj=.5)
      hline(tmp.meta.ci[2], tmp.meta.ci[3], plotlim, y)
    }
  }

  # same as above, but for each category
  
  Categories.ci <- categories.ci  
  if (n.categories)
  {
    for (i in seq(n.categories))
    {
      y <- ylinenumber4categorylabel[i]
      y <- (n.textlines-y)*yunit 

      text(-categories.indent*xunit, y, category.labels[i], adj=0, cex=cex)
    
      if (plot.categories.ci[i])
      {
        y <- ylinenumber4categorysubtotal[i]
        y <- (n.textlines-y)*yunit 

        text(0, y, 'Pooled', adj=0, cex=cex)

        # fractions
        if (show.nN.indiv)
        {
          if (reverse.printed.columns.order)
          {
            fraction.left  <- fraction14category
            fraction.right <- fraction04category
          }
          else
          {
            fraction.left  <- fraction14category
            fraction.right <- fraction04category
          }
      
          text((authors.spaces+1+1*nN.spaces)*xunit, y, fraction.left[i],  adj=1, cex=cex)
          text((authors.spaces+2+2*nN.spaces)*xunit, y, fraction.right[i], adj=1, cex=cex)
        }

        # ci's
        
        if (is.character(categories.ci.orig))
        {
          ci.char <- categories.ci.orig[i,]
        }
        else
        {
          ci.char <- character(3)
          for (j in 1:3) ci.char[j] <- Round(categories.ci[i,j], or.ncharacters, or.min.ndecimals)
        }
        
        ci.char <- paste(ci.char[1], ' (', ci.char[2], ' to ', ci.char[3], ')', sep='')    
        text((authors.spaces+ci.spaces[1]+(2+2*nN.spaces)*show.nN.indiv+OR.plot.spaces+print.or.ci*ci.spaces[2])*xunit, y, ci.char, adj=0, cex=cex)

        if (log.scale) categories.ci[i,] <- log(categories.ci[i,])
        categories.ci[i,] <- a + b*categories.ci[i,]

        points(categories.ci[i,1],y,type='p',pch=category.subtotal.pch, adj=.5)
        hline(categories.ci[i,2], categories.ci[i,3], plotlim, y)
      }
    }
  }

  if (length(or.side.labels))
  {
    x.sides.labels <- a + b*(log.scale==F) + 0.05*c(-1,1)*diff(plotlim)
    sides.labels.line <- 0.5
    mtext(or.side.labels[1], at=x.sides.labels[1], line=sides.labels.line, side=1, adj=1, cex=or.side.labels.cex)
    mtext(or.side.labels[2], at=x.sides.labels[2], line=sides.labels.line, side=1, adj=0, cex=or.side.labels.cex)
  }

  if (test) abline(v=plot.limits, lty=10)

  if (standard.or.plot)
  {
    out <- list(meta.ci=meta.ci, categories.ci=Categories.ci, studies.ci=Studies.ci)
  }
  else
  {
    out <- list(categories.ci=Categories.ci, studies.ci=Studies.ci)
  }

  out
}
