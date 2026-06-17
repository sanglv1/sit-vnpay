package com.vnpay.sit.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationRegressionTest {

    @Test
    void migrate_shouldPassOnEmptyDatabase() {
        String url = "jdbc:h2:mem:flyway_empty_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        Flyway flyway = flyway(url);

        flyway.migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource(url));
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'partner_config' AND COLUMN_NAME = 'created_by_email'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void migrate_shouldBackfillLegacyNullCreatedByEmail() {
        String url = "jdbc:h2:mem:flyway_legacy_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

        flyway(url, "4").migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource(url));
        jdbc.update("INSERT INTO sit_user(full_name, email, password_hash, role, active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "Admin User", "admin.test@vnpay.vn", "hash", "ADMIN", true);
        jdbc.update("INSERT INTO partner_config(name, flow, tmn_code, secret_key, return_url, ipn_url, note, active, "
                        + "created_at, updated_at, created_by_email) VALUES "
                        + "(?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)",
                "Legacy Merchant", "PAY", "TMN001", "secret", "", "http://merchant/ipn", "legacy", true);

        flyway(url, null).migrate();

        String createdBy = jdbc.queryForObject(
                "SELECT created_by_email FROM partner_config WHERE tmn_code = 'TMN001'",
                String.class
        );
        assertThat(createdBy).isEqualTo("admin.test@vnpay.vn");
    }

    private static Flyway flyway(String url) {
        return flyway(url, null);
    }

    private static Flyway flyway(String url, String target) {
        FluentConfiguration config = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(dataSource(url))
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private static DataSource dataSource(String url) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(url);
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }
}
