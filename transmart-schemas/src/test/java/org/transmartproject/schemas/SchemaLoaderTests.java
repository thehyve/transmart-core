package org.transmartproject.schemas;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = NONE, classes = SchemaLoaderTestApplication.class)
public class SchemaLoaderTests {

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    private Logger log = LoggerFactory.getLogger(SchemaLoaderTests.class);

    private List<String> getColumns(String schema, String table) throws SQLException {
        Connection connection = dataSourceTransactionManager.getDataSource().getConnection();
        ResultSet resultSet;
        resultSet = connection.getMetaData().getColumns(null, schema, table, null);
        List<String> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(resultSet.getString("COLUMN_NAME"));
        }
        return result;
    }

    private static final List<String> EXPECTED_COLUMNS = Arrays.asList(
            "ENCOUNTER_NUM",
            "PATIENT_NUM",
            "CONCEPT_CD",
            "PROVIDER_ID",
            "START_DATE",
            "MODIFIER_CD",
            "INSTANCE_NUM",
            "VALTYPE_CD",
            "TVAL_CHAR",
            "NVAL_NUM",
            "VALUEFLAG_CD",
            "QUANTITY_NUM",
            "UNITS_CD",
            "END_DATE",
            "LOCATION_CD",
            "OBSERVATION_BLOB",
            "CONFIDENCE_NUM",
            "UPDATE_DATE",
            "DOWNLOAD_DATE",
            "IMPORT_DATE",
            "SOURCESYSTEM_CD",
            "UPLOAD_ID",
            "SAMPLE_CD",
            "TRIAL_VISIT_NUM"
    );
    static {
        Collections.sort(EXPECTED_COLUMNS);
    }

    @Test
    public void testSchemasCreated() throws SQLException {
        List<String> columns = getColumns("I2B2DEMODATA", "OBSERVATION_FACT");
        Collections.sort(columns);
        Assert.assertEquals(EXPECTED_COLUMNS, columns);
    }

}
