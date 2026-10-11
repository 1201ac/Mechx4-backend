package com.nerlogix;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Skipped during test runs per production safety rules to prevent hitting production database")
@EnabledIfEnvironmentVariable(named = "RUN_TIDB_INTEGRATION_TEST", matches = "true")
class TidbConnectivityTest {

    @Test
    void testTiDbConnectionAndFlywayMigration() {
        String baseUrl = "jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3";
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (username == null || username.isBlank()) {
            username = "2LGgC98nsjSZmT8.root";
        }
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "etiD3bLMPowSlFH2";
        }

        System.out.println("Testing database creation in TiDB Cloud...");
        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE DATABASE IF NOT EXISTS nerlogix");
            System.out.println("Database nerlogix created or already exists!");
        } catch (Exception e) {
            System.err.println("Database creation failed: " + e.getMessage());
            e.printStackTrace();
        }

        String targetUrl = "jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/nerlogix?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3";

        System.out.println("Testing Flyway migration in schema nerlogix against TiDB Cloud...");
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(targetUrl, username, password)
                    .baselineOnMigrate(true)
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();
            System.out.println("Flyway migration in nerlogix schema completed successfully!");
        } catch (Exception e) {
            System.err.println("Flyway migration failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
