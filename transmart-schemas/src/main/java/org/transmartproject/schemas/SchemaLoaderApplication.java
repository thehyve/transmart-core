package org.transmartproject.schemas;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@EnableConfigurationProperties({ LiquibaseProperties.class })
@SpringBootApplication
public class SchemaLoaderApplication implements CommandLineRunner {

    private static final String USAGE = "Usage: transmart-schemas\n\nCreating and updating TranSMART database schemas.";

    public static void main(String[] args) {
        SpringApplication.run(SchemaLoaderApplication.class, args);
    }

    private final Logger log = LoggerFactory.getLogger(SchemaLoaderApplication.class);

    @Override
    public void run(String... args) {
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cl = parser.parse(options, args);
            if (cl.hasOption("help")) {
                System.err.print(USAGE + "\n");
                return;
            }
        } catch (ParseException e) {
            log.error(e.getMessage());
            System.err.print(USAGE + "\n");
            System.exit(1);
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

}
