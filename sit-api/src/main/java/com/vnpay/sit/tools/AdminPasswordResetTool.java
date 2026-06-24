package com.vnpay.sit.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

/**
 * Reset admin password using the same JDBC settings and BCrypt encoder as the API.
 * Usage on VPS: {@code ./scripts/reset-admin.sh sanglv@vnpay.vn 'YourPassword'}
 */
public final class AdminPasswordResetTool {

    private AdminPasswordResetTool() {
    }

    public static void main(String[] args) throws Exception {
        String email = arg(args, 0, env("SIT_ADMIN_EMAIL", null));
        String password = arg(args, 1, env("SIT_ADMIN_PASSWORD", null));
        if (email == null || email.isBlank()) {
            System.err.println("Missing email. Pass as arg or set SIT_ADMIN_EMAIL.");
            System.exit(1);
        }
        if (password == null || password.length() < 6) {
            System.err.println("Missing password (min 6 chars). Pass as arg or set SIT_ADMIN_PASSWORD.");
            System.exit(1);
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        String jdbcUrl = resolveJdbcUrl();
        String dbUser = env("DB_USERNAME", "postgres");
        String dbPass = env("DB_PASSWORD", "");

        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("DB user: " + dbUser);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            System.out.println("Connected OK.");

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sit_user")) {
                rs.next();
                System.out.println("sit_user count before: " + rs.getInt(1));
            }

            int updated;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sit_user SET password_hash = ?, active = true, updated_at = NOW() "
                            + "WHERE LOWER(email) = LOWER(?)")) {
                ps.setString(1, hash);
                ps.setString(2, email);
                updated = ps.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sit_user (full_name, email, password_hash, role, active, created_at, updated_at) "
                                + "VALUES (?, ?, ?, 'ADMIN', true, NOW(), NOW())")) {
                    ps.setString(1, env("SIT_ADMIN_NAME", "System Admin"));
                    ps.setString(2, email);
                    ps.setString(3, hash);
                    ps.executeUpdate();
                    System.out.println("Inserted admin: " + email);
                }
            } else {
                System.out.println("Updated password for: " + email);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT password_hash FROM sit_user WHERE LOWER(email) = LOWER(?)")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.err.println("ERROR: user not found after write");
                        System.exit(2);
                    }
                    String stored = rs.getString(1);
                    if (!encoder.matches(password, stored)) {
                        System.err.println("ERROR: BCrypt verify failed for stored hash");
                        System.exit(2);
                    }
                    System.out.println("BCrypt verify OK (len=" + stored.length() + ")");
                }
            }
        }

        System.out.println("Done — test login with the same email/password.");
    }

    private static String resolveJdbcUrl() {
        String url = env("SPRING_DATASOURCE_URL", null);
        if (url != null) {
            return url;
        }
        url = env("DB_URL", null);
        if (url != null) {
            return url;
        }
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5432");
        String name = env("DB_NAME", "sit_vnpay_db");
        return "jdbc:postgresql://" + host + ":" + port + "/" + name + "?sslmode=disable";
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static String arg(String[] args, int index, String defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        return args[index];
    }
}
