Upload Data Set Explorer Tags
-----------------------------
The tags upload pipeline allows you to add additional information to any data node in your study.
Metadata tags appear in a pop-up when you right click on a node in the data set explorer tree.

Parameters
------------
The parameters file should be named `tags.params` and contains:
- `TAGS_FILE` **(Mandatory)** The name of the file that contains the tags. See below for format.

TAGS_FILE format
------------

|Concept path|Tag title|Tag description|Index|
|------------|---------|---------------|-----|
|\           |Organism |Homo sapiens   |2    |

Table, tab separated, txt file. Header must be present, but is not interpreted (i.e. column order is fixed). The `Concept path`, `Tag title` combination has to be unique.

- `Concept path` **(Mandatory)** Relative concept path pointing to a node the tag is associated with. Path nodes should be separated by a `\` and do not include the top node (study name). Use `\` to denote study node.
    e.g. `\Biomarker Data\Cell-line proteomics\LFQ-1`
    NOTE: It should exactly correspond to the path of the node. Otherwise it will be uploaded, but never appears on the tree.

- `Tag title` **(Mandatory)** Title of the tag (e.g. `ORGANISM`).

- `Tag description` **(Mandatory)** Text that appears next to the tag title on pop-up (e.g. `Homo sapiens`)

- `Index` **(Mandatory)** Integer that determines the position of the tag in the pop-up relative to the other tags. The `Index` itself is not shown in the pop-up, but all tags of a node are sorted in ascending order based on their index.

Tags upload.
------------

There are two ways to upload tags:

- As separate tags data type upload (recommended).

    * Place tags file into `tags` folder.
    * You must specify tags file inside `tags` folder with `TAGS_FILE` parameter in `tags.params` file.
    * Run

        ./transmart-batch-capsule.jar -p /path/to/STUDY_NAME/tags/tags.params
        
- As part of clinical data upload.

    * Place tags file into `clinical` folder.
    * Specify tags file inside `clinical` folder with `TAGS_FILE` variable in `clinical.params` file.
    * Run usual clinical data upload.

#### Tags deletion
Is not currently implemented in transmart-batch. If only the `Tag description` or `Index` needs to change, but not the `Concept path` or `Tag title`, a reupload will suffice.
Otherwise you can delete tags with following sql: `delete from i2b2metadata.i2b2_tags where path like '<path>' and tag_type='<title>'`
