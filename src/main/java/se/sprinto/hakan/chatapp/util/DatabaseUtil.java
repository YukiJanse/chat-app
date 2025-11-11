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

public class DatabaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static final String PRODUCT_ENVIRONMENT = "product";
    private static DatabaseUtil instance;
    private final Properties properties;
    private DataSource dataSource;

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

    public static DatabaseUtil getInstance() {
        return getInstance(PRODUCT_ENVIRONMENT);
    }

    public static DatabaseUtil getInstance(String environment) {
        if (instance == null) {
            instance = new DatabaseUtil(environment);
            logger.info("Successfully created an instance. Environment: {}", environment);
        }
        return instance;
    }

    private String getString(String key) {
        return properties.getProperty(key);
    }
    
    private int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
    
    private boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

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

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
