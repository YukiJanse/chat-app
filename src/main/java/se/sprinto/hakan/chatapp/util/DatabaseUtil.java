package se.sprinto.hakan.chatapp.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * DatabaseUtil is a singleton utility class responsible for configuring and providing access
 * to a database connection pool using HikariCP.
 */
public class DatabaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static final String PRODUCT_ENVIRONMENT = "product";
    private static DatabaseUtil instance;
    private final Properties properties;
    private DataSource dataSource;

    /**
     * This allows the application to seamlessly switch between real databases and test databases (such as H2)
     * without modifying the application code.
     * @param environment
     */
    private DatabaseUtil(String environment) {
        properties = new Properties();
        String propertiesFilename = environment.equalsIgnoreCase("test") ? "application-test.properties" : "application.properties";
        try (InputStream input = ClassLoader.getSystemResourceAsStream(propertiesFilename)) {
            properties.load(input);
            logger.info("Successfully loaded application.properties.");
            // Set a DataSource object
            dataSource = createDataSource();
        } catch (IOException e) {
            logger.error("Failed to load application.properties.");
        } catch (RuntimeException e) {
            logger.error("Failed to initialize DatabaseUtil", e);
        }

    }

    /**
     * Returns the singleton using the production configuration.
     * @return An instance of this class with the production configuration
     */
    public static DatabaseUtil getInstance() {
        return getInstance(PRODUCT_ENVIRONMENT);
    }

    /**
     * Returns the singleton using either "product" or "test" mode.
     * @param environment The parameter for the configuration
     * @return An instance of this class with the configuration depending on the parameter
     */
    public static DatabaseUtil getInstance(String environment) {
        if (instance == null) {
            instance = new DatabaseUtil(environment);
            logger.info("Successfully created an instance. Environment: {}", environment);
        }
        return instance;
    }

    /**
     * Property access helper for String values.
     * @param key The key for properties.
     * @return a property value.
     */
    private String getString(String key) {
        return properties.getProperty(key);
    }

    /**
     * Property access helper for int values.
     * @param key The key for properties.
     * @return a property value.
     */
    private int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    /**
     * Property access helper for boolean values.
     * @param key The key for properties.
     * @return a property value.
     */
    private boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    /**
     * Creates HikariDataSource with settings from application.properties.
     * @return HikariDataSource object.
     */
    public HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        try {
            // Basic settings
            config.setJdbcUrl(getString("db.url"));
            config.setUsername(getString("db.username"));
            config.setPassword(getString("db.password"));

            // Pool-settings
            config.setMaximumPoolSize(getInt("hikaricp.max-pool-size"));
            config.setMinimumIdle(getInt("hikaricp.min-idle"));
            config.setConnectionTimeout(getInt("hikaricp.connection-timeout"));
            config.setIdleTimeout(getInt("hikaricp.idle-timeout"));
            config.setMaxLifetime(getInt("hikaricp.max-lifetime"));

            // Performance-settings
            config.setAutoCommit(getBoolean("hikaricp.auto-commit"));
            config.addDataSourceProperty("cachePrepStmts", getString("hikaricp.datasource.cachePrepStmts"));
            config.addDataSourceProperty("prepStmtCacheSize", getString("hikaricp.datasource.prepStmtCacheSize"));
            config.addDataSourceProperty("prepStmtCacheSqlLimit", getString("hikaricp.datasource.prepStmtCacheSqlLimit"));

            // Pool name for logging
            config.setPoolName(getString("hikaricp.pool-name"));

            logger.info("HikariCP DataSource created with pool size: {}", config.getMaximumPoolSize());
        } catch (Exception e) {
            logger.error("Failed to create DataSource", e);
            throw new RuntimeException(e);
        }

        return new HikariDataSource(config);
    }

    /**
     * Returns the active DataSource instance
     * @return the active DataSource instance
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns a live Connection from the pool.
     * @return a live Connection
     * @throws SQLException will be thrown if something went wrong with JDBC
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
