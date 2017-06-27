Here are command line arguments you could specify for transmart-batch upload.

| Argument | Description                                        | Example                               |
|----------|----------------------------------------------------|---------------------------------------|
| -d       | Overrides/supplements params file parameter        | `-d STUDY_ID=GSE8581`                 |
| -p       | Specify params file; uploads data specified in file| `-p studies/GSE8581/clinical.params`  |
| -f       | Specify folder; uploads all data recursively       | `-f studies/GSE8581/`                 |
| -c       | Location of database configuration properties file | `-c batchdb.properties`               |
| -j       | The ID or name of a job instance                   |                                       |
| -r       | Restart the last failed execution                  |                                       |
| -s       | Stop a running execution                           |                                       |
| -a       | Abandon a stopped execution                        |                                       |
| -n       | Start the job despite it was run before            |                                       |
