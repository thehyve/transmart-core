Upload Data Set Explorer Tags
-----------------------------

Tags appear on popup when you doing right click on node in data set explorer tree.

Parameters
------------
The parameters file should be named `tags.params` and contains:
- `TAGS_FILE` **Mandatory**. Points to the tags file. See below for format.

#####Tags tsv input file format.

|concept_key|tag_title|tag_description|index|
|-----------|---------|---------------|-----|
|\          |ORGANISM |Homo Sapiens   |2    |

Columns map onto these db columns of i2b2metadata.i2b2_tags table:
concept_key=path, tag_title=tag_type, tag_description=tag index=tags_idx

Header names are not strict, but header has to be present because first line is always skipped.
Order of columns is important.

- concept_key - relative concept path pointing to a node the tag is associated with. Use `\` to denote study node.
    e.g. `\Cell-line\Biomarker Data\Cell-line proteomics\LFQ-1\`
    NOTE: It should exactly correspond to path of the node. Otherwise it would be uploaded, but never appears on the tree.
    There is no referential consistency between tags table and i2b2 yet.

- tag_title - title of the tag. e.g. `ORGANISM`.
    TranSMART tries to find user-friendly title by searching using title as key in `messsage.properties` file first.
    It shows title as is if it fails to find it in properties file.

- tag_description - text that appears next to title on popup. e.g. `Homo Sapiens`
- index - detects position of tags on popup relatively to others. A higher position in tags with lower number.

#####Tags upload.

You have two ways to upload tags:

- As part of clinical data upload.

    * Place tags file into `clinical` folder.
    * Specify tags file inside `clinical` folder with `TAGS_FILE` variable inside `clinical.params` file.
    * Run usual clinical data upload.

- As separate tags data type upload.

    * Place tags file into `tags` folder.
    * You must specify tags file inside `tags` folder with `TAGS_FILE` variable inside `tags.params` file only if you
    have several files inside `tags` folder.
    * Run

        ./transmart-batch-capsule.jar -p /path/to/STUDY_NAME/tags.params

#####Tags deletion.
Is not implemented in transmart-batch.
You could delete tags with following sql: `delete from i2b2metadata.i2b2_tags where path like '<path>' and tag_type='<title>'`
