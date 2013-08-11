required.packages <- c("reshape2", "ggplot2", "data.table", "Cairo",
		"snowfall", "WGCNA", "gplots", "Rserve");
new.packages <- required.packages[
		!(required.packages %in% installed.packages()[,"Package"])];
if (length(new.packages))
	install.packages(new.packages, repos=Sys.getenv("CRAN_MIRROR"));
