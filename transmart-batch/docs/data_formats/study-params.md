Study-specific parameters
-------------------------

Study-specific parameters could be specified either in a data type parameters file (e.g. `clinical.params`, `expression.params`, ...) or in the separate `study.params` file in the study folder.

- `STUDY_ID` **Mandatory** Identifier of the study.
- `SECURITY_REQUIRED` _Default:_ `N`. Defines study as Private (`Y`) or Public (`N`).
- `TOP_NODE` Has to start with `\` (e.g. `\Public Studies\Cell-lines`). _Default:_ `\(Public|Private) Studies\<STUDY_ID>`. The study top node.
