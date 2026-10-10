package com.nerlogix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseDriverSelectionTest {

    @Test
    void testMysqlJdbcUrlResolvesToMysqlDriver() {
        String mysqlUrl = "jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY";
        DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(mysqlUrl);
        
        assertThat(driver).isEqualTo(DatabaseDriver.MYSQL);
        assertThat(driver.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
    }

    @Test
    void testH2JdbcUrlResolvesToH2Driver() {
        String h2Url = "jdbc:h2:file:./data/nerlogix;MODE=MySQL";
        DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(h2Url);
        
        assertThat(driver).isEqualTo(DatabaseDriver.H2);
        assertThat(driver.getDriverClassName()).isEqualTo("org.h2.Driver");
    }
}
