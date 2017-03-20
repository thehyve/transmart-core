# Open API specification for the tranSMART platform
[![Validation status](http://online.swagger.io/validator?url=https://raw.githubusercontent.com/thehyve/transmart-upgrade/master/open-api/swagger.json)](http://online.swagger.io/validator/debug?url=https://raw.githubusercontent.com/thehyve/transmart-upgrade/master/open-api/swagger.json)

## Overview

The REST API of _tranSMART_ is implemented in the [transmart-rest-api](../transmart-rest-api) module.
This directory contains the API documentation, written in [Swagger]((https://swagger.io/)) specification format.
The specification is in the file [swagger.yaml](swagger.yaml).

This directory also contains a copy of [Swagger UI](https://github.com/swagger-api/swagger-ui)
(released under Apache 2.0 license), that provides a UI to explore the API specification.
You can open the UI [locally](index.html), or visit a
[public copy](http://transmart-pro-test.thehyve.net/open-api).



## Development

To generate `swagger.json` and `swagger_spec.js` from `swagger.yaml`, run:
```bash
js-yml swagger.yaml > swagger.json
{ echo "var spec ="; cat swagger.json; echo ";"; } > swagger_spec.js
```

The `js-yml` tool can be obtained by:

```bash
npm -g install js-yml
```
