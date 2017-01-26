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


## License

Copyright &copy; 2017  The Hyve

Licensed under the [Apache License, Version 2.0](apache-2.0.txt) (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
