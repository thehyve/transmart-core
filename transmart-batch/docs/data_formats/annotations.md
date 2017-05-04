Annotations (common)
====================

"Annotations" is a general term for platforms (e.g. mRNA platforms) and other ancillary data that needs to be loaded before the experiment data of a high-dimensional data type. The annotation doesn't have to be specific to one dataset; it can be shared between two or more similar data sets that derive from the same platform.

Parameters
----------

- `PLATFORM` **(Mandatory)** The identifier of the annotation used. Preferably a GPL ID.
- `TITLE` **(Mandatory)** A human readable title for the annotation.
- `ANNOTATIONS_FILE` **(Mandatory)** The name of the file containing the annotation data. If the annotation data file is not in the same folder as the annotation parameters file, a full path to the annotation data file may be provided.
- `ORGANISM` _Default: Homo sapiens_. The scientific name of the species the platform is associated with.
- `GENOME_RELEASE` **(Mandatory if platform contains chromosomal region information)** The genome build used for the platform (e.g. `hg19`).


**Note:** For historical reasons, parameter files named `annotation.params` refer to mRNA
platforms. The recommended name for new mRNA platform parameter files is `mrna_annotation.params`.


<!-- vim: tw=80 et ft=markdown spell:
-->
