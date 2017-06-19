#########################################################################################
## R-script to produce summary statistics (mean, median, etc.,) and static boxplot image
## for the data loading and data preprocessing tab
#########################################################################################
######
# Expected input data:
# Only data from high dimensional nodes. Either:
# * variable "loaded_variables" - a list of data.frames, containing one or more data.frames. Present as a variable in
#                                 the global environment.
#   Data for multiple high dimensional nodes can be provided. Per high dimensional node 1 or 2 dataframes are to be
#   passed on, depending on whether 1 or 2 patient subsets are created.
#   Descriptive names (labels) are given to the data.frames so that it can be recognized which data.frame was derived
#   from which data node and subset.
#   Format of the labels: an unique identifier for the node (numerical identifier appended behind the letter "n"),
#   followed by _s1 or _s2 depending on subset (actually, subset number can be anything, as long as it is numerical),
#   format: n<numerical_id_node>_s<numerical_id_subset>, e.g. n0_s1, n0_s2, n1_s1, n1_s2.
#
# or
# * variable "preprocessed" -  containing one single data.frame with the preprocessed data. Present as a variable in
#   the global environment. It contains all the preprocessed data from the different nodes merged into a single
#   data.frame.
#
# The data.frames (coming from high dimensional nodes) in 'loaded_variables' and 'preprocessed' have columns:
# 'Row.Label' and 'Bio.marker' (Bio.marker column isoptional), followed by the sample columns with the measurements
# (for example columns named ASSAY_0001, ASSAY_0002 ...)
#
#
# Expected arguments to main() function:
# * phase parameter. Expected argument: "fetch" or "preprocess".  This parameter specifies whether the script is run for
#   the 'fetch data' or 'preprocess data' tab, and it is used to fetch the corresponding data from the appropriate
#   variable (loaded_variables/preprocessed) and in the names of the output files of this script
#
# * projection of the data: "default_real_projection" (= intensity values/ counts) or "log_intensity"
#   (log2 of intensity values/counts).
######
# Output:
# * 1 boxplot image per data node, png format. Name: <phase>_box_plot_node_<node identifier>.png.
# * 1 textfile per node with the summary statistics per subset, in json format.
#     Name: <phase>_summary_stats_node_<node identifier>.json.
# for the "fetch" data output files the node identifier is extracted from the labels of loaded_variables, format:
#   n<numerical node identifier> (e.g. "n0"), for "preprocess data" the data from all nodes has been merged and
#   the word "all" is used for the node identifier in the output file names
#
# Note: If the data node is not high dimensional or the dataset is empty, no boxplot will be returned - only an image
#       with the text "No data points to plot", also no mean, median etc. will be returned in the summary statistics:
#       only variableLabel, node name and subset name are returned, and totalNumberOfValues = 1 and
#       numberOfMissingValues = 1.
######
# If this script gets extended for clinical data:
#     ** right now this summary stats script is only adapted for high dimensional data nodes, later the functionality
#         might be extended for clinical data. In that case it might be possible to recognize if it is high or low dim
#         data based on the column names of the data.frame (assuming low dim data will also be passed on in the form of
#         data.frames and will not contain a "Row.Label" column)
#
# NOTE:  the script is now assuming that the second column of high dim nodes is called "Bio.marker", and that data
# coming from low dim nodes NEVER have a second column named "Bio.marker"".
#
###########
library(jsonlite)  #  During execution via smartR jsonlite is included automatically but this statement isNeeded for
                   #  automated unit tests to run successfully as they do not relay on smartR services for execution.
library(gplots)


main <- function(phase = NA, projection = NA)
{
  input_data <- get_input_data(phase)
  check_input(input_data, projection)

  data_measurements <- extract_measurements(input_data)

  # for preprocessed data the data from the two different subsets is merged
  # the summary stats and boxplot functions expect one data.frame per subset per node.
  # preprocessed data.frame therefore needs to be split
  if (phase == "preprocess")
  {
    data_measurements <- split_on_subsets(data_measurements)
  }

  summary_stats_json <-
    produce_summary_stats(data_measurements, phase)
  write_summary_stats(summary_stats_json)
  produce_boxplot(data_measurements, phase, projection)

  return(list(messages = "Finished successfully"))
}

get_input_data <- function(phase)
{
  #check if the phase parameter
  if (is.na(phase))
  {
    stop(
      "Supply phase parameter to function \'main()\'. Expected input: \'fetch\' or \'preprocess\'"
    )
  }

  if (phase != "fetch" & phase != "preprocess" & !is.na(phase))
  {
    error_msg <-
      paste(
        "Incorrect value for phase parameter. Given input: \'", phase, "\'.",
        "Expected input: \'fetch\' or \'preprocess\'", sep = ""
      )
    stop(error_msg)
  }

  # for fetch data tab the data from 'loaded_variables' should be used
  if (phase == "fetch")
  {
    if (!exists("loaded_variables"))
    {
      stop(
        "Summary stats is run for phase \'fetch\', but variable \'loaded_variables\' does not exist in the environment."
      )
    }
    input_data <- loaded_variables
  }

  # for preprocess data tab the data from 'preprocessed' should be used
  if (phase == "preprocess")
  {
    if (!exists("preprocessed"))
    {
      stop(
        "Summary stats is run for phase \'preprocess\', but variable \'preprocessed\' does not exist in the environment."
      )
    }
    input_data <- list("preprocessed" = preprocessed)
  }

  return(input_data)
}


#check if provided variables and phase info are in line with expected input as described at top of this script
check_input <- function(datasets, projection)
{
  #expected input: list of data.frames
  items_list <- sapply(datasets, class)
  if (class(datasets) != "list" |
      !all(items_list == "data.frame"))
    #for a data.frame is.list() also returns TRUE. Class returns "data.frame" in that case
  {
    stop("Unexpected input. Expected input: a list, containing one or more data.frames")
  }

  # all items in the list are expected to have some unique identifier for the node (numerical identifier appended behind
  # the letter "n"), followed by an underscore and a subset identifier - s<numerical id> - depending on subset,
  # e.g. n0_s1, n0_s2, n1_s1, n1_s2.
  dataset_names <- names(datasets)
  expected_format_names <- "^[[:alpha:]]+[[:digit:]]+_n[[:digit:]]+_s[[:digit:]]+$"
  names_in_correct_format <-
    grepl(expected_format_names, dataset_names)
  if (any(!names_in_correct_format &
          dataset_names !=  "preprocessed"))
  {
    stop(
      paste(
        "One or more labels of the datasets do not have the expected format.",
        "\nExpected format: either the label should be \'preprocessed\' or it should be an unique numerical identifier",
        "for the node appended behind the letter \n\',",
        "\nfollowed by an underscore and an unique numerical identifier for the subset appended behind an \'s\',",
        "\ne.g. n0_s1, n0_s2, n1_s1, n1_s2. "
      )
    )
  }

  #all labels of the data.frames should be unique
  dataset_names <- names(datasets)
  if (any(duplicated(dataset_names)))
  {
    stop(
      "Not all data.frame labels are unique; one or more labels in the \'loaded_variables\' or \'preprocessed\' variable are duplicated"
    )
  }

  # check if data.frame "preprocessed" has the right column names
  # expected format of column names for sample columns is <sample_name>_n<numerical node id>_s<numerical subset id>,
  # eg. sample234_n1_s1
  if (dataset_names[1] == "preprocessed")
    #for preprocessed data there is only 1 data.frame
  {
    column_names <- colnames(datasets$preprocessed)
    expected_format_names <- ".+_n[[:digit:]]+_s[[:digit:]]+$"  # I am leaving this unchanged, preprocessing for boxplot
    names_in_correct_format <-                                  # Does not have a defined for the input yet. 
      grepl(expected_format_names, column_names)
    if (!all(
      names_in_correct_format |
      column_names == "Row.Label" | column_names == "Bio.marker"
    ))
    {
      stop(
        paste(
          "The column names of the sample columns of the data.frame \'preprocessed\' do not have the expected format.",
          "\nExpected column names for the feature columns (probes/genes/proteins/etc): \'Row.Label\'",
          "and (optional) \'Bio.marker\' ",
          "\nSample columns should adhere to the format: <sample_name>_n<numerical node id>_s<numerical subset id>,",
          "eg. sample234_n1_s1"
        )
      )
    }
  }


  if (is.na(projection))
  {
    stop(
      "Supply projection parameter to function \'main()\'. Expected input:  \'default_real_projection\' or \'log_intensity\'"
    )
  }

  if (!is.na(projection) &
      projection != "default_real_projection" &
      projection != "log_intensity")
  {
    stop(
      "Incorrect value for projection parameter - expected input:  \'default_real_projection\' or \'log_intensity\'"
    )
  }





}


# preprocessed data contains one big data.frame. Summary stats and boxplot should be made per subset, so the dataset
# gets split on subsets
split_on_subsets <- function(preprocessed_measurements)
{
  preprocessed_measurements <- preprocessed_measurements$preprocessed

  split_measurements <- list()
  used_columns <- c()


  # get possible subset identifiers. Format of column names for sample columns is
  #   <sample_name>_n<numerical node id>_s<numerical subset id>, eg. sample234_n1_s1
  column_names <- colnames(preprocessed_measurements)

  subsets_match_indices <- regexpr("s[[:digit:]]+$", column_names)
  subsets_matches <-
    regmatches(x = column_names, m = subsets_match_indices)
  subset_names <- unique (subsets_matches)

  if (length(subset_names) == 0)
  {
    stop(
      "Something went wrong when splitting the preprocessed dataset into separate datasets per cohort (subset).
      Check the format of the column names of variable \'preprocessed\'.
      Expected format for column labels (of sample columns) of data.frame \'preprocessed\' is:
      <sample_name>_n<numerical identifier for the node>_s<numerical identifier for the subset>
      for example sample23341_n1_s1"
    )

  }

  #extract for each subset the corresponding data
  for (subset_name in subset_names)
  {
    subset_columns <-
      grep(paste(subset_name, "$", sep = ""), column_names) #strict matching to avoid accidental matching
    #(e.g. if sample name contains "s1" as well)

    item_name <- paste("preprocessed_", subset_name, sep = "")

    split_measurements[[item_name]] <-
      preprocessed_measurements[, subset_columns]
    used_columns <- c(used_columns, subset_columns)
  }

  # each column should only be present in one of the two split datasets,
  # and all columns should be present in one or the other split dataset
  if (any(duplicated(used_columns)) |
      !all(1:ncol(preprocessed_measurements) %in% used_columns))
  {
    stop(
      "Something went wrong when splitting the preprocessed dataset into separate datasets per cohort (subset).
      Maybe the column names of variable \'preprocessed\' are not of the correct format?
      Expected format for column labels of data.frame \'preprocessed\' is:",
      "<sample_name>_n<numerical identifier for the node>_s<numerical identifier for the subset>, e.g. sample1_n1_s1"
    )
  }
  return(split_measurements)
}



# Extract the measurement values from the data.frames.
#  The data.frames (coming from high dimensional nodes) have columns:
#   Row.Label, Bio.marker (optional), ASSAY_0001, ASSAY_0002 ...
#  All columns except Row.Label and Bio.marker contain the measurement values.
extract_measurements <- function(datasets)
{
  for (i in 1:length(datasets))
  {
    dataset <- datasets[[i]]
    dataset_id <- names(datasets)[[i]]
    colNames <- colnames(dataset)

    # test if the data.frame contains data from a high dimensional data node
    #   the first column of the data from high dimensional nodes should be "Row.Label",
    #   a second column called "Bio.marker" is optional.
    #   For low dimensional data these columns will contain different names
    is_highDim <- (colNames[2] == "Bio.marker")
    is_numeric <- (!is_highDim & is.numeric(dataset[[2]]) )  # is a clinical data of type numeric

    # if it is not high dim, then it is clinical data. Right now this script is only written to handle high dimensional
    #   data.
    if (!is_highDim & !is_numeric)
    {
      datasets[[i]] <- NA
    }

    # only keep the measurement values, by removing the row label and biomarker columns
    if (is_highDim | is_numeric)
    {
      non_measurement_columns <-
        which(colNames %in% c("Row.Label","Bio.marker"))
      datasets[[i]] <-
        dataset[,-non_measurement_columns, drop = F]

      #drop empty data columns for check if measurements are numeric.
      tmp_measurements <- dataset[,-non_measurement_columns, drop = F]
      empty_columns <- apply(tmp_measurements, allNA, MARGIN = 2)
      tmp_measurements <- tmp_measurements[,!empty_columns, drop = F]
      ##TEST IF GOES WELL IF ALL EMPTY
      if (ncol(tmp_measurements) > 0 & !all(sapply(tmp_measurements, FUN = is.numeric)))
      {
        stop(
          paste(
            "Correct extraction of data columns was not possible for dataset ",dataset_id,
            ". It seems that, aside from the Row.Label and Bio.marker column,",
            " there are one or more non numeric data columns in the data.frame.", sep = ""
          )
        )
      }

    }
  }

  return(datasets)
}

allNA <- function(v1)
{
  return(all(is.na(v1)))
}

# Function to produce one JSON file per data node containing the summary stats per subset for that node.
# Summary stats include: the number of values, number of missing values quartiles, min, max, mean, std deviation, median
# NOTE: this function converts the data.frame to a vector containing all datapoints from the dataframe and calculates
#       the statistics over this entire vector.
#       The statistics are thus not calculated per data column or row, but over the whole data.frame
# NOTE 2: quartiles, mean and other statistics can only be calculated on numeric data.
#         If this method is extended for clinical data:
#         * Are the desired summary statistics the same for clinical data (ie. mean, sd, median, etc.)
#           or should different stats be calculated?
#         * extend this method to recognize low dim data and split up the data.frame or apply this method to each data
#           column separately to calculate the statistics per clinical variable separately
#           (ie. supply the clinical data as a list containing a separate data.frame/vector for each clinical variable)
#         * build in  a test to determine if a variable is numeric or categorical and only calculate the statistics if
#           numeric.
#         * If a variable is categorical: are missing values in that case NA or  "" (empty string?)
produce_summary_stats <- function(measurement_tables, phase)
{
  # construct data.frame to store the results from the summary statistics in
  table_columns <-
    c(
      "variableLabel", "node", "subset", "totalNumberOfValuesIncludingMissing", "numberOfMissingValues",
      "numberOfSamples", "min", "max", "mean", "standardDeviation", "q1", "median", "q3"
    )

  result_table <-
    as.data.frame(matrix(
      NA, nrow = length(measurement_tables), ncol = length(table_columns),
      dimnames = list(names(measurement_tables), table_columns)
    ))

  # add information about node and subset identifiers
  result_table$subset <-
    gsub(".*_","", rownames(result_table)) #take everything after _
  result_table$node <-
    discardSubset(rownames(result_table))

  nodes <-  result_table$node
  result_table$node[nodes == "preprocessed"] <-
    "preprocessed_allNodesMerged"


  # calculate summary stats per data.frame
  for (i in 1:length(measurement_tables))
  {
    # get the name of the data.frame, identifying the node and subset
    identifier <- names(measurement_tables)[i]
    result_table[identifier, "variableLabel"] <- identifier

    if (!all(is.na(measurement_tables[[i]])))
    {
      result_table[identifier, "numberOfSamples"]  <-
        ncol(measurement_tables[[i]])
    }

    # convert data.frame to a vector containing all values of that data.frame, a vector remains a vector
    measurements <- unlist(measurement_tables[[i]])

    # determine total number of values and number of missing values
    is.missing <- is.na(measurements)
    result_table[identifier, "totalNumberOfValuesIncludingMissing"] <-
      length(measurements)
    result_table[identifier, "numberOfMissingValues"] <-
      length(which(is.missing))

    # calculate descriptive statistics, only for numerical data.
    # the 50% quantile is the median. 0 and 100% quartiles are min and max respectively

    result_table[identifier, "mean"] <- mean(measurements, na.rm = T)
    result_table[identifier, "standardDeviation"] <-
      sd(measurements, na.rm = T)

    quartiles <- quantile(measurements, na.rm = T)
    result_table[identifier, "min"] <- quartiles["0%"]
    result_table[identifier, "max"] <- quartiles["100%"]
    result_table[identifier, "q1"] <- quartiles["25%"]
    result_table[identifier, "median"] <- quartiles["50%"]
    result_table[identifier, "q3"] <- quartiles["75%"]
  }

  # remove rownames so that rownames are not included in the JSON output.
  rownames(result_table) <- 1:nrow(result_table)

  # write the summary statistics for each node to a separate file, in JSON format
  summary_stats_all_nodes <- list()

  unique_nodes <- unique(result_table$node)
  for (node in unique_nodes)
  {
    partial_table <-
      result_table[which(result_table$node == node), ,drop = F]
    rownames(partial_table) <-
      1:nrow(partial_table) #does not influence json result, however is needed for unit testing (matching rownumbers).
    if (node != "preprocessed_allNodesMerged") {
      fileName <-
        paste(phase,"_summary_stats_node_", node, ".json", sep = "")
    }
    if (node == "preprocessed_allNodesMerged") {
      fileName <- paste(phase,"_summary_stats_node_all.json", sep = "")
    }
    summary_stats_all_nodes[[fileName]] <- partial_table
  }
  return(summary_stats_all_nodes)
}



write_summary_stats <- function(summary_stats)
{
  for (i in 1:length(summary_stats))
  {
    summary_stats_JSON <-
      toJSON(summary_stats[[i]], dataframe = "rows", pretty = T, digits = I(17))
    fileName <- names(summary_stats)[[i]]
    write(summary_stats_JSON, fileName)
  }
}


# Function that outputs one box plot image per data node
produce_boxplot <- function(measurement_tables, phase, projection)
{
  #get node identifiers
  nodes <- unique( discardSubset(names(measurement_tables)) )

  if (projection == "default_real_projection") {
    projection <- "intensity"
  }
  if (projection == "log_intensity") {
    projection <- "log2(intensity)"
  }

  # convert the tables to vectors for use with the boxplot function
  # this converts a data.frame to a vector containing all values from the data.frame, a vector remains a vector
  measurement_vectors <- measurement_tables
  for (i in 1:length(measurement_vectors))
  {
    measurement_vectors[[i]] <- unlist(measurement_vectors[[i]])
  }

  boxplot_results_all_nodes <- list()


  #make a separate boxplot for each node and write to a PNG file.
  for (node in nodes)
  {
    # grab the data.frames corresponding to the selected node
    identifiers_single_node <-
      grep(paste("^",node, sep = ""), names(measurement_vectors), value = T)
    single_node_data <- measurement_vectors[identifiers_single_node]

    #remove node prefix from the names (labels) of the data.frame
    names(single_node_data) <-
      gsub(".*_","",names(single_node_data))

    #make sure the subsets are always ordered s1, s2, ...
    single_node_data <-
      single_node_data[order(names(single_node_data))]

    ## create box plot, output to PNG file
    if (phase != "preprocess") {
      fileName <-
        paste(phase, "_box_plot_node_", node, ".png", sep = "")
    }
    if (phase == "preprocess") {
      fileName <- paste(phase, "_box_plot_node_all.png", sep = "")
    }
    png(filename = fileName)

    # in case there is data present: create box plot
    if (!all(is.na(single_node_data)))
    {
      if (phase != "preprocess") {
        plot_title <- paste("Box plot node:", node)
      }
      if (phase == "preprocess") {
        plot_title <- "Box plot node: preprocessed - all nodes merged"
      }

      boxplot_results_all_nodes[[fileName]] <-
        boxplot(
          single_node_data, col = "grey", show.names = T, ylab = projection,
          main = plot_title, outline = F, pch = 20, cex =
            0.2
        )
    }

    # if there are no data values: create image with text "No data points to plot"
    if (all(is.na(single_node_data)))
    {
      sinkplot("start")
      write("No data points\n\   to plot","")
      sinkplot("plot")
      box("outer", lwd = 2)
      boxplot_results_all_nodes[[fileName]] <-
        "No data points to plot"
    }

    dev.off()
  }
  return(boxplot_results_all_nodes)
}

discardSubset <- function(labels) {
  gsub("_s[[:digit:]]","",labels)
}
