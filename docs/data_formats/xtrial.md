# Across Trials for Clinical Data

The clinical data job supports loading associations between the facts and
the Across Trials tree.

## Providing Across Trials Mapping Data

For the across trials data to be loaded, an extra file is needed (henceforth "the across trials file"). The path to this
file must be provided through the `XTRIAL_FILE` parameter. This can either be provided through in the command line with
the `-d` parameter or it can be included in the `clinical.params` file.

The across trials file shall be a tab-delimited file with two columns. The first row will be ignored.  The first column
("study prefix column") shall contain prefixes to concepts in the study (without the top node), while the second column
("across trials prefix column") shall contain prefixes to the concepts in Across Trials tree (as in database, without
the `\Across Trials` prefix. Both column's values should end and start with `\`. If any value in either of the columns
does not end or start with `\`, it will be implicitly added.

## Behavior

For each study concept: the longest matching prefix specified in the study prefix column will be searched.

- If no prefix matches, there will be no across trials mapping to that specific concept.
- If there is one:
    - it will be stripped from the study concept, leaving only the part after the matched prefix
      (could be an empty string).
    - The result will then be appended to the corresponding value of the across trials prefix column.
    - An across trials concept with this value (ignoring the initial `\Across Trials\` part) will be searched for.
        - If it is not found, a warning will be logged and no mapping will be applied to the this study concept.
        - If it is found, then the type if checked.
            - If there is a type mismatch between the study concept and the across trials concept, an error will be
              logged (but the job will not be aborted) and no mapping will be used.
            - If the types match, the study concept in question will be mapped to the found across trials concept.

## Implementation

*(this information presented in this section is subject to change without notice)*

It is only possible to determine the full set of study concepts and their types once the full data has been read, and
therefore it's only possible to determine all the across trials nodes used then. But on the other hand, when each fact
is inserted, its across trials mapping must already be known (lest we go through an expensive update step later). This
means that we must either load the across trials nodes on demand, or we must preload a superset of the across trials
nodes we're going to need. We chose the latter option because it minimizes latency.

In retrospective, it would have been better to implement two passes on the data, one to fully determine the metadata
properties (patient data, concepts, and so on), and another to actually insert the data. But under the current one-pass,
the implementation for across trial follows these lines:

- Read the across trials file at the same time as the other mapping files. Store the information in memory. This will be
  an `allowStartIfComplete` step (has to be executed on restarts).
- Determine the set of across trial prefixes that are not prefixes of other prefixes and load *all* the across trials
  concepts below these prefixes. A more memory friendly strategy would be to join the data in the across trials file
  with that in the column mapping file and load a smaller set of across trials nodes from the data; but even there the
  minimal set would not be determinable (at this point we don't know the needed children for categorical concepts and
  we don't even know which concepts are categorical).
- All the required information is now in memory.
