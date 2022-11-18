package tech.ydb.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nullable;

import tech.ydb.jdbc.settings.YdbOperationProperties;
import tech.ydb.scheme.SchemeClient;
import tech.ydb.table.Session;

public interface YdbConnection extends Connection {

    /**
     * Returns class with some type conversion capabilities
     *
     * @return ydb types converter
     */
    YdbTypes getYdbTypes();

    /**
     * Returns scheme client, initialized during a first access
     *
     * @return scheme client in memoized mode
     */
    SchemeClient getYdbScheme();

    /**
     * Returns current YDB session for this connection
     *
     * @return YDB session
     */
    Session getYdbSession();

    /**
     * Return current YDB transaction, if exists
     *
     * @return YDB transaction ID or null, if no transaction started
     */
    @Nullable
    String getYdbTxId();


    /**
     * Returns operation properties, configured for this connection
     *
     * @return default YDB operation properties
     */
    YdbOperationProperties getYdbProperties();


    /**
     * Returns current database
     *
     * @return database if configured
     */
    @Nullable
    String getDatabase();

    //


    @Override
    YdbDatabaseMetaData getMetaData() throws SQLException;

    @Override
    YdbStatement createStatement() throws SQLException;

    @Override
    YdbStatement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException;

    @Override
    YdbPreparedStatement prepareStatement(String sql, int resultSetType,
                                          int resultSetConcurrency) throws SQLException;

    @Override
    YdbStatement createStatement(int resultSetType, int resultSetConcurrency,
                                 int resultSetHoldability) throws SQLException;

    @Override
    YdbPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                          int resultSetHoldability) throws SQLException;

    @Override
    YdbPreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException;

    @Override
    YdbPreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException;

    @Override
    YdbPreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException;


    /**
     * Prepares statement depending on driver settings
     *
     * @param sql sql to execute
     * @return statement
     * @throws SQLException in case of any internal error
     */
    @Override
    YdbPreparedStatement prepareStatement(String sql) throws SQLException;

    /**
     * Prepares statement with explicit configuration
     *
     * @param sql  sql to prepare
     * @param mode prepare mode
     * @return prepared statement
     * @throws SQLException in case of any internal error
     */
    YdbPreparedStatement prepareStatement(String sql, PreparedStatementMode mode) throws SQLException;

    enum PreparedStatementMode {
        DEFAULT,
        IN_MEMORY,
        DATA_QUERY,
        DATA_QUERY_BATCH
    }
}
