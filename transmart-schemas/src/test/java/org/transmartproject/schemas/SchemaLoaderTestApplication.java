package org.transmartproject.schemas;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Profile("test")
@EnableTransactionManagement
@EnableConfigurationProperties({ LiquibaseProperties.class })
@SpringBootApplication
public class SchemaLoaderTestApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SchemaLoaderTestApplication.class, args);
    }

    SchemaLoaderTestApplication() {
    }

    @Override
    public void run(String... args) {
    }

}
