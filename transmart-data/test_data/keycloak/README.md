# Keycloak database with test users and roles

Users included in this database are used in [rest-api e2e tests](../../../transmart-rest-api-e2e)

In order to import the database into Keycloak instance use the following properties during when starting the Keycloak server:
```
<keycloak folder path>/bin/standalone.sh -Dkeycloak.migration.action=import
-Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=keycloak-test-resource.json
-Dkeycloak.migration.strategy=OVERWRITE_EXISTING
```

Default e2e tests configuration matches properties of this database.

For more information check [Keycloak Export and Import documentation](https://www.keycloak.org/docs/2.5/server_admin/topics/export-import.html).



