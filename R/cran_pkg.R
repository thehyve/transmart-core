required.packages <- c("reshape2", "ggplot2", "data.table", "Cairo",
		"snowfall", "gplots", "Rserve", "foreach", "doParallel");
new.packages <- required.packages[
		!(required.packages %in% installed.packages()[,"Package"])];
if (length(new.packages))
	install.packages(new.packages, repos=Sys.getenv("CRAN_MIRROR"));
