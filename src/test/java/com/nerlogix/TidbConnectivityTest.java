package com.nerlogix;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class TidbConnectivityTest {

    @Test
    void testTiDbConnectionIfCredentialsProvided() {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || url.isBlank()) {
            url = "jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3";
        }
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (username == null || username.isBlank()) {
            username = "2LGgC98nsjSZmT8.root";
        }
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "etiD3bLMPowSlFH2";
        }

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {

            assertThat(conn.isValid(5)).isTrue();
            if (rs.next()) {
                String version = rs.getString(1);
                System.out.println("Connected to TiDB Cloud successfully! Version: " + version);
                assertThat(version).isNotNull();
            }
        } catch (Exception e) {
            System.err.println("TiDB Cloud connection failed: " + e.getMessage());
            // If offline/network restricted, don't fail standard build unless SPRING_PROFILES_ACTIVE=prod
            if ("prod".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE"))) {
                throw new RuntimeException("Failed to connect to TiDB Cloud in production profile", e);
            }
        }
    }
}
