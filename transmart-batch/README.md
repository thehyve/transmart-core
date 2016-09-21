<img src=images/batch_logo.png width="50" height="80"> tranSMART Batch
============================
[![Build Status](https://travis-ci.org/thehyve/transmart-batch.svg?branch=master)](https://travis-ci.org/thehyve/transmart-batch)

tranSMART pipeline alternative to ETL, using Spring Batch.

If interested, see [Developer documentation](docs/developer_docs.md).

# Quick start: Setting up transmart-batch

## Prepare database
**This step is only required if you're using an Oracle database**. To prepare an Oracle tranSMART database you need to have admin rights. Transmart-batch requires the *ts-batch* schema, which is used for job tracking. To add this schema to the database you need to `git clone` the transmart git repository. Make sure you add a file named [**batchdb.properties**](#properties-file) with the database connection information to the directory and execute `./gradlew setupSchema`. If you look at the schemas you should see the *ts-batch* has now been added.

You **cannot** use the user _tm\_cz_ to run this step, it must be a user with appropriate permissions (like _system_ in a default installation). After you run this step, you must change _batchdb.properties_ to start using _tm\_cz_ instead.

This step only needs to be run once per database.

## Properties file
The properties file contains information as the location of the database, the username and password that are used to upload the data to the database. The properties is build up of four lines indicating which database is being used, either PostgreSQL or Oracle, the location of the database and the user.

    batch.jdbc.driver=<DRIVER_TO_USE>  
    batch.jdbc.url=<PREFIX>:<DATABASE>  
    batch.jdbc.user=<USERNAME>
    batch.jdbc.password=<PASSWORD>  

The **DRIVER_TO_USE** indicates which database the data is being loaded to, either PostgreSQL or Oracle.  
  * `PostgreSQL` - **org.postgresql.Driver**  
  * `Oracle` - **oracle.jdbc.driver.OracleDriver**

The **PREFIX** is an extension of the **DRIVER_TO_USE** and again is database dependent:  
  * `PostgreSQL` - **jdbc:postgresql**
  * `Oracle` - **jdbc:oracle:thin**  

**DATABASE** indicates the actual URL of the database. It is build up of the IP address, the port to use and for Oracle an additional database name. (driver specific format)  
  * `PostgreSQL` - **//&lt;host>:&lt;port>/&lt;database>**
  * `Oracle` - **@&lt;host>:&lt;port>:&lt;SID>**

If you have a default installation of tranSMART, the **USERNAME** and **PASSWORD** will both be `tm_cz`.  
See example property files:  
*PostgreSQL*
```
    batch.jdbc.driver=org.postgresql.Driver
    batch.jdbc.url=jdbc:postgresql://localhost:5432/transmart
    batch.jdbc.user=tm_cz
    batch.jdbc.password=tm_cz
```
*Oracle*
```
    batch.jdbc.driver=oracle.jdbc.driver.OracleDriver
    batch.jdbc.url=jdbc:oracle:thin:@localhost:1521:ORCL
    batch.jdbc.user=tm_cz
    batch.jdbc.password=tm_cz
```

## Build commands from source
Next to stable releases you can also use the development version of transmart-batch. This means you will have to build the tool from the source files available on github. First you get the source files with `git clone <url_to_transmart-batch_github>`, then, from the transmart-batch folder, you can execute `./gradlew capsule` which will generate a **.jar** file in `transmart-batch/build/libs/`. The **.jar** file can be used to run transmart-batch.

There is also the possibility of making a distributable zip. To generate this zip, build transmart-batch with `./gradlew distZip`. Note: keep in mind that the Java major version used to build transmart-batch cannot be more recent than the Java version used to execute the loading pipeines. For instance, you cannot build the binaries with Java 8 and execute them with Java 7.

# Loading Data
To load the data to tranSMART using transmart-batch, you need 1) either the **.jar** file you built with the instructions in the previous section or the pre-built release files of transmart-batch, 2) a file with the database connection information (generally called *batchdb.properties*) and 3) study files with corresponding mapping and parameter files.

To succesfully load the data, it is important to keep in mind the following assumptions made by transmart-batch:

  1. A file with database connection settings, named *batchdb.properties*, is available in the directory were the command is run from (the working directory).

  2. The study files follow a certain structure
    - Data files follow the format requirements (see [docs/data_formats](docs/data_formats/)).
    - Mapping files refer to the data files
    - Parameter files next to the data and mapping files

The first assumption can be overriden with `-c` parameter, the second by including path components (relative or absolute) in the file references.

## Transmart-batch commands
Assuming the **batchdb.properties** file is in the directory transmart-batch is run from and that the clinical data has all the mapping and parameter files, the command `<path_to>/transmart-batch.jar -p <path_to>/study_folder/clinical/clinical.params` loads the clinical data to tranSMART.

#### Possible flags
- `-c` - (Optional) Overwrite default behaviour and specify a file to be used as **batchdb.properties**.
- `-p` - (**Mandatory**) Path to the parameter file indicating which data should be uploaded.
- `-n` - (Optional) Forces a rerun of the job. Needed if you want to reload a dataset.
- `-r` - (Optional) Rerun a failed job using the execution id.
- `-j` - (Optional, in combination with -r) Execution id for the job.

## Examples:

* Loading clinical data with a gene expression data set  
```
    <path_to>/transmart-batch.jar -p <path_to>/study_folder/clinical/clinical.params
    <path_to>/transmart-batch.jar -p <path_to>/study_folder/mRNA/expression.params
```

* Use a different **batchdb.properties**
```
    <path_to>/transmart-batch.jar -p <path_to>/study/clinical/clinical.params -c <path_to>/<file_name>
```

* Restart a failed job
```
    At the start of the failed job retrieve the execution id:  
        org...BetterExitMessageJobExecutionListener - Job id is 1186, execution id is 1271
    <path_to>/transmart-batch.jar -p <path_to>/study_folder/clinical/clinical.params -r -j 1271
```

## Expected file structure
Below is the file structure that transmart-batch expects. Note that only the params files have set names, any of the other files and folders can be named freely.

```
<STUDY_FOLDER>
│   study.params
├── <folder_name>
    |   clinical.params
    |   Clinical_data.txt
    |   Clinical_wordmap.txt
    |   Clinical_columnmap.txt
├── <another_folder>
    |   expression.params
    |   dataset1.txt
    └── subjectSampleMapping.txt
├── <folder_with_nested_folders>
    |   ├── <dataset_1>
    |   |   |   rnaseq.params
    |   |   |   rnaseq_data.txt
    |   |   |   subjectmapping.txt
    |   ├── <dataset_2>
    |   └── <annotations_for_dataset_1>
    |   |   |   rnaseq_annotation.params
    |   |   |   gene_platform.txt
└── <meta_data_tags>
```

### For a specific example study:
with most supported datatypes have a look at [How to load the TraIT Cell line use case](docs/how_to_load_trait_cluc.md).

![Natasha](images/natasha_full_no_solar.png)
