Study-specific parameters
-------------------------

Study-specific parameters should be specified in the `study.params` file placed directly in the study folder.

- `STUDY_ID` **(Mandatory)** Identifier of the study. Will be uppercased.
- `SECURITY_REQUIRED` _Default:_ `Y`. Defines study as Private (`Y`) or Public (`N`).
- `TOP_NODE` The path leading up to the study in tranSMART (including the study node itself). This parameter can be used to provide a study name different from the `STUDY_ID` and/or to load your study to a different folder than the default based on the `SECURITY_REQUIRED` parameter. Has to start with `\` (e.g. `\Public Studies\Cell-lines`). _Default:_ `\(Public|Private) Studies\<STUDY_ID>`.
