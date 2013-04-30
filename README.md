**tranSMART Core API** is a library with Groovy interfaces that define
the interactions and calls between the various componements of [tranSMART](http://transmartproject.org).

# Clinical data API

## I2B2-based query tool

To facilitate communications with i2b2 and i2b2-like clinical data resources,
two API's are defined which follow the design philosophy behind i2b2 and resemble
the i2b2 Ontology Management (ONT) and Data Repository (CRC) RESTful API's.

### Clinical Data Ontology 

A [ConceptsResource](org/transmartproject/core/ontology/ConceptsResource.html)
is defined which can be implemented by clinical data sources to expose ontology
trees with concepts. See the [i2b2 ONT messaging](https://www.i2b2.org/software/files/PDF/current/Ontology_Messaging.pdf)
documentation for more background information. Only a few calls are defined,
the ones which happen to be used in the legacy tranSMART codebase.

### Clinical Data Query API

A [QueriesResource](org/transmartproject/core/querytool/QueriesResource.html)
is defined which can be exposed by clinical data sources as a means of querying
clinical data. This resource exposes methods to run queries and retrieve query results. The API is very closely
modelled to the [i2b2 CRC query message API](https://community.i2b2.org/wiki/display/DevForum/Query+Building+from+Ontology).

