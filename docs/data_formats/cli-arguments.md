Here are command line arguments you could specify for transmart batch upload.

| Argument | Description                                        | Example                               |
|----------|----------------------------------------------------|---------------------------------------|
| -d       | overrides/supplements params file parameter        | `-d STUDY_ID=GSE8581`                 |
| -p       | specify params file                                | `-p studies/GSE8581/clinical.params`  |
| -f       | specify folder with params files on any depth      | `-f studies/GSE8581/`                 |
| -c       | location of database configuration properties file | `-f batchdb.properties`               |
| -j       | the id or name of a job instance                   |                                       |
| -r       | restart the last failed execution                  |                                       |
| -s       | stop a running execution                           |                                       |
| -a       | abandon a stopped execution                        |                                       |
| -n       | start the job despite it was run before            |                                       |
