package tech.ydb.jdbc;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.jdbc.impl.Validator;
import tech.ydb.jdbc.impl.YdbConnectionImpl;
import tech.ydb.jdbc.settings.ParsedProperty;
import tech.ydb.jdbc.settings.YdbConnectionProperties;
import tech.ydb.jdbc.settings.YdbConnectionProperty;
import tech.ydb.jdbc.settings.YdbOperationProperties;
import tech.ydb.jdbc.settings.YdbProperties;
import tech.ydb.scheme.SchemeClient;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;

import static tech.ydb.jdbc.YdbConst.JDBC_YDB_PREFIX;
import static tech.ydb.jdbc.YdbConst.YDB_DRIVER_USES_SL4J;

/**
 * YDB JDBC driver, basic implementation supporting {@link TableClient} and {@link SchemeClient}
 */
public class YdbDriver implements Driver {
    private static final Logger LOGGER = LoggerFactory.getLogger(YdbDriver.class);

    private static final YdbConnectionsCache CONNECTIONS = new YdbConnectionsCache();

    static {
        YdbDriver driver = new YdbDriver();
        try {
            DriverManager.registerDriver(driver);
        } catch (SQLException sql) {
            throw new RuntimeException(sql);
        }
        LOGGER.info("YDB JDBC Driver registered: {}", driver);
    }

    @Override
    public YdbConnection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        // logging should be after acceptsURL, otherwise we can log properties with secrets of another database
        LOGGER.info("About to connect to [{}] using properties {}", url, info);

        YdbProperties properties = YdbProperties.from(url, info);
        Clients clients = CONNECTIONS.getClients(new ConnectionConfig(url, info), properties);

        YdbOperationProperties operationProperties = properties.getOperationProperties();
        Duration sessionTimeout = operationProperties.getSessionTimeout();

        Validator validator = new Validator(operationProperties);
        Result<Session> session = validator.joinResult(
                LOGGER,
                () -> "Get or create session",
                () -> clients.tableClient.createSession(sessionTimeout));

        // TODO: support scheme client eager initialization
        return new YdbConnectionImpl(
                clients.schemeClient,
                session.getValue(),
                operationProperties,
                validator,
                url, // raw URL
                properties.getConnectionProperties().getDatabase());
    }

    @Override
    public boolean acceptsURL(String url) {
        return YdbProperties.isYdb(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        String targetUrl = acceptsURL(url) ? url : JDBC_YDB_PREFIX;
        return YdbProperties.from(targetUrl, info).toDriverProperties();
    }

    @Override
    public int getMajorVersion() {
        return YdbDriverInfo.DRIVER_MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return YdbDriverInfo.DRIVER_MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false; // YDB is non-compliant
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException(YDB_DRIVER_USES_SL4J);
    }

    public static YdbConnectionsCache getConnectionsCache() {
        return CONNECTIONS;
    }

    public static class ConnectionConfig {
        private final String url;
        private final Properties properties;

        public ConnectionConfig(String url, Properties properties) {
            this.url = Objects.requireNonNull(url);
            this.properties = new Properties();
            this.properties.putAll(Objects.requireNonNull(properties));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConnectionConfig)) {
                return false;
            }
            ConnectionConfig that = (ConnectionConfig) o;
            return Objects.equals(url, that.url) && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, properties);
        }
    }

    private static class Clients implements AutoCloseable {
        private final GrpcTransport grpcTransport;
        private final TableClient tableClient;
        private final Supplier<SchemeClient> schemeClient;

        private Clients(GrpcTransport grpcTransport, TableClient tableClient, Supplier<SchemeClient> schemeClient) {
            this.grpcTransport = Objects.requireNonNull(grpcTransport);
            this.tableClient = Objects.requireNonNull(tableClient);
            this.schemeClient = Objects.requireNonNull(schemeClient);
        }

        @Override
        public void close() {
            try {
                tableClient.close();
                grpcTransport.close();
            } catch (Exception e) {
                LOGGER.error("Unable to close client: " + e.getMessage(), e);
            }
        }
    }

    public static class YdbConnectionsCache {
        private final Map<ConnectionConfig, Clients> cache = new HashMap<>();

        private synchronized Clients getClients(ConnectionConfig config, YdbProperties properties) {
            // TODO: implement cache based on connection and client properties only (excluding operation properties)
            YdbConnectionProperties connProperties = properties.getConnectionProperties();

            Clients clients = properties.getOperationProperties().isCacheConnectionsInDriver() ?
                    cache.get(config) :
                    null; // not cached
            if (clients != null) {
                LOGGER.debug("Reusing YDB connection to {}{}",
                        connProperties.getAddress(),
                        Strings.nullToEmpty(connProperties.getDatabase()));
                return clients;
            }

            ParsedProperty tokenProperty = connProperties.getProperty(YdbConnectionProperty.TOKEN);
            boolean hasAuth = tokenProperty != null && tokenProperty.getParsedValue() != null;
            LOGGER.info("Creating new YDB connection to {}{}{}",
                    connProperties.getAddress(),
                    Strings.nullToEmpty(connProperties.getDatabase()),
                    hasAuth ? " with auth" : " without auth");

            GrpcTransport grpcTransport = connProperties.toGrpcTransport();

            TableClient tableClient = properties.getClientProperties().toTableClient(grpcTransport);

            Supplier<SchemeClient> schemeClient = Suppliers.memoize(() -> {
                return SchemeClient.newClient(grpcTransport).build();
            })::get;

            clients = new Clients(grpcTransport, tableClient, schemeClient);
            cache.put(config, clients);
            return clients;
        }

        public synchronized int getConnectionCount() {
            return cache.size();
        }

        public synchronized void close() {
            LOGGER.info("Closing {} cached connection(s)...", cache.size());
            cache.values().forEach(Clients::close);
            cache.clear();
        }
    }
}
