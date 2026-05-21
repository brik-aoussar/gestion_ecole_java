package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Point d'entrée UNIQUE pour toute connexion à la base de données.
 * Pattern Singleton — HikariCP gère le pool de connexions.
 *
 * UTILISATION dans les DAO :
 *   try (Connection conn = DatabaseConnection.getConnection()) { ... }
 */
public final class DatabaseConnection {

    // ── Paramètres de connexion ───────────────────────────────────────────────
    private static final String URL      = "jdbc:mysql://localhost:3306/gestion_ecole" +
                                           "?useUnicode=true&characterEncoding=utf8mb4" +
                                           "&serverTimezone=Africa/Casablanca" +
                                           "&rewriteBatchedStatements=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";          // à configurer

    private static HikariDataSource dataSource;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private DatabaseConnection() {}

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);    // 30s
        config.setIdleTimeout(600_000);         // 10min
        config.setMaxLifetime(1_800_000);       // 30min

        // Validation
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("GestionEcolePool");

        dataSource = new HikariDataSource(config);
    }

    /**
     * Retourne une connexion depuis le pool.
     * Doit être fermée dans un try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Ferme le pool proprement à l'arrêt de l'application. */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
