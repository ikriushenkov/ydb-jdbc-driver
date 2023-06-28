package tech.ydb.jdbc.impl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tech.ydb.jdbc.YdbConst;
import tech.ydb.jdbc.YdbDatabaseMetaData;
import tech.ydb.jdbc.YdbDriverInfo;
import tech.ydb.jdbc.YdbStatement;
import tech.ydb.jdbc.impl.helper.ExceptionAssert;
import tech.ydb.jdbc.impl.helper.JdbcConnectionExtention;
import tech.ydb.jdbc.impl.helper.TableAssert;
import tech.ydb.jdbc.impl.helper.TestResources;
import tech.ydb.test.junit5.YdbHelperExtension;

public class YdbDatabaseMetaDataImplTest {
    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @RegisterExtension
    private static final JdbcConnectionExtention jdbc = new JdbcConnectionExtention(ydb);

    private static final String TABLE_TYPE = "TABLE";
    private static final String SYSTEM_TABLE_TYPE = "SYSTEM TABLE";

    private static final String ALL_TYPES_TABLE = "all_types";
    private static final String INDEXES_TABLE = "table_keys";

    private DatabaseMetaData metaData;

    @BeforeAll
    public static void createTables() throws SQLException {
        try (Statement statement = jdbc.connection().createStatement()) {
            // create simple tables
            statement.execute("--jdbc:SCHEME\n"
                    + "create table t1 (id Int32, value Int32, primary key (id));\n"
                    + "create table `dir1/t1` (id Int32, value Int32, primary key (id));\n"
                    + "create table `dir1/t2` (id Int32, value Int32, primary key (id));\n"
                    + "create table `dir2/t1` (id Int32, value Int32, primary key (id));\n"
                    + "create table `dir2/dir1/t1` (id Int32, value Int32, primary key (id));\n"
                    + "create table " + INDEXES_TABLE + "("
                            + "key1 Int32, key2 Text, value1 Int32, value2 Text, value3 Int32, "
                            + "primary key(key1, key2), "
                            + "index idx_2 global on (value1, value2),"
                            + "index idx_1 global on (value3));\n"
                    + TestResources.createTableSql(ALL_TYPES_TABLE)
            );
        }
    }

    @AfterAll
    public static void dropTables() throws SQLException {
        try (Statement statement = jdbc.connection().createStatement();) {
            // drop simple tables
            statement.execute("--jdbc:SCHEME\n"
                    + "drop table t1;\n"
                    + "drop table `dir1/t1`;\n"
                    + "drop table `dir1/t2`;\n"
                    + "drop table `dir2/t1`;\n"
                    + "drop table `dir2/dir1/t1`;\n"
                    + "drop table " + INDEXES_TABLE + ";\n"
                    + "drop table " + ALL_TYPES_TABLE + ";\n"
            );
        }
    }

    @BeforeEach
    public void loadMetaData() throws SQLException {
        this.metaData = jdbc.connection().getMetaData();
    }

    @Test
    public void isReadOnly() throws SQLException {
        Assertions.assertFalse(jdbc.connection().getMetaData().isReadOnly());
        jdbc.connection().setTransactionIsolation(YdbConst.ONLINE_CONSISTENT_READ_ONLY);
        Assertions.assertTrue(jdbc.connection().getMetaData().isReadOnly());
    }

    @Test
    public void jdbcConnectionTest() throws SQLException {
        Assertions.assertSame(jdbc.connection(), metaData.getConnection());

        String url = metaData.getURL();
        Assertions.assertNotNull(url);
        Assertions.assertEquals(jdbc.jdbcURL(), url);
    }

    @Test
    public void metaDataUnwrapTest() throws SQLException {
        Assertions.assertTrue(metaData.isWrapperFor(YdbDatabaseMetaData.class));
        Assertions.assertSame(metaData, metaData.unwrap(YdbDatabaseMetaData.class));

        Assertions.assertFalse(metaData.isWrapperFor(YdbStatement.class));

        ExceptionAssert.sqlException("Cannot unwrap to interface tech.ydb.jdbc.YdbStatement",
                () -> metaData.unwrap(YdbStatement.class));
    }

    @Test
    public void metaDataValuesTest() throws SQLException {
        Assertions.assertFalse(metaData.allProceduresAreCallable());
        Assertions.assertTrue(metaData.allTablesAreSelectable());
        Assertions.assertEquals("", metaData.getUserName());
        Assertions.assertTrue(metaData.nullsAreSortedHigh());
        Assertions.assertFalse(metaData.nullsAreSortedLow());
        Assertions.assertFalse(metaData.nullsAreSortedAtStart());
        Assertions.assertFalse(metaData.nullsAreSortedAtEnd());
        Assertions.assertEquals("YDB", metaData.getDatabaseProductName());
        Assertions.assertEquals("unspecified", metaData.getDatabaseProductVersion());

        Assertions.assertEquals(YdbDriverInfo.DRIVER_NAME, metaData.getDriverName());
        Assertions.assertEquals(YdbDriverInfo.DRIVER_VERSION, metaData.getDriverVersion());
        Assertions.assertEquals(YdbDriverInfo.DRIVER_MAJOR_VERSION, metaData.getDriverMajorVersion());
        Assertions.assertEquals(YdbDriverInfo.DRIVER_MINOR_VERSION, metaData.getDriverMinorVersion());

        Assertions.assertEquals(JdbcDriverVersion.getInstance().getFullVersion(), metaData.getDriverVersion());

        Assertions.assertEquals(0, metaData.getDatabaseMajorVersion());
        Assertions.assertEquals(0, metaData.getDatabaseMinorVersion());

        Assertions.assertEquals(YdbDriverInfo.JDBC_MAJOR_VERSION, metaData.getJDBCMajorVersion());
        Assertions.assertEquals(YdbDriverInfo.JDBC_MINOR_VERSION, metaData.getJDBCMinorVersion());

        Assertions.assertFalse(metaData.usesLocalFiles());
        Assertions.assertFalse(metaData.usesLocalFilePerTable());
        Assertions.assertTrue(metaData.supportsMixedCaseIdentifiers());
        Assertions.assertFalse(metaData.storesUpperCaseIdentifiers());
        Assertions.assertFalse(metaData.storesLowerCaseIdentifiers());
        Assertions.assertTrue(metaData.storesMixedCaseIdentifiers());
        Assertions.assertTrue(metaData.supportsMixedCaseQuotedIdentifiers());
        Assertions.assertFalse(metaData.storesUpperCaseQuotedIdentifiers());
        Assertions.assertFalse(metaData.storesLowerCaseQuotedIdentifiers());
        Assertions.assertTrue(metaData.storesMixedCaseQuotedIdentifiers());

        Assertions.assertEquals("`", metaData.getIdentifierQuoteString());
        Assertions.assertEquals("", metaData.getSQLKeywords());

        Assertions.assertSame(YdbFunctions.NUMERIC_FUNCTIONS, metaData.getNumericFunctions());
        Assertions.assertSame(YdbFunctions.STRING_FUNCTIONS, metaData.getStringFunctions());
        Assertions.assertSame(YdbFunctions.SYSTEM_FUNCTIONS, metaData.getSystemFunctions());
        Assertions.assertSame(YdbFunctions.DATETIME_FUNCTIONS, metaData.getTimeDateFunctions());

        Assertions.assertNotEquals(metaData.getStringFunctions(), metaData.getSystemFunctions());
        Assertions.assertNotEquals(metaData.getStringFunctions(), metaData.getNumericFunctions());
        Assertions.assertNotEquals(metaData.getSystemFunctions(), metaData.getNumericFunctions());
        Assertions.assertNotEquals(metaData.getNumericFunctions(), metaData.getTimeDateFunctions());
        Assertions.assertNotEquals(metaData.getStringFunctions(), metaData.getTimeDateFunctions());
        Assertions.assertNotEquals(metaData.getSystemFunctions(), metaData.getTimeDateFunctions());

        Assertions.assertEquals("\\", metaData.getSearchStringEscape());
        Assertions.assertEquals("", metaData.getExtraNameCharacters());
        Assertions.assertTrue(metaData.supportsAlterTableWithAddColumn());
        Assertions.assertTrue(metaData.supportsAlterTableWithDropColumn());
        Assertions.assertTrue(metaData.supportsColumnAliasing());
        Assertions.assertTrue(metaData.nullPlusNonNullIsNull());
        Assertions.assertFalse(metaData.supportsConvert());
        Assertions.assertFalse(metaData.supportsConvert(Types.INTEGER, Types.BIGINT));
        Assertions.assertTrue(metaData.supportsTableCorrelationNames());
        Assertions.assertFalse(metaData.supportsDifferentTableCorrelationNames());
        Assertions.assertTrue(metaData.supportsExpressionsInOrderBy());
        Assertions.assertFalse(metaData.supportsOrderByUnrelated());

        Assertions.assertTrue(metaData.supportsGroupBy());
        Assertions.assertTrue(metaData.supportsGroupByUnrelated());
        Assertions.assertTrue(metaData.supportsGroupByBeyondSelect());
        Assertions.assertTrue(metaData.supportsLikeEscapeClause());
        Assertions.assertTrue(metaData.supportsMultipleResultSets());
        Assertions.assertTrue(metaData.supportsMultipleTransactions());

        Assertions.assertFalse(metaData.supportsNonNullableColumns());

        Assertions.assertTrue(metaData.supportsMinimumSQLGrammar());
        Assertions.assertFalse(metaData.supportsCoreSQLGrammar());
        Assertions.assertFalse(metaData.supportsExtendedSQLGrammar());
        Assertions.assertFalse(metaData.supportsANSI92EntryLevelSQL());
        Assertions.assertFalse(metaData.supportsANSI92IntermediateSQL());
        Assertions.assertFalse(metaData.supportsANSI92FullSQL());
        Assertions.assertFalse(metaData.supportsIntegrityEnhancementFacility());
        Assertions.assertTrue(metaData.supportsOuterJoins());
        Assertions.assertTrue(metaData.supportsFullOuterJoins());
        Assertions.assertTrue(metaData.supportsLimitedOuterJoins());

        Assertions.assertEquals("Database", metaData.getSchemaTerm());
        Assertions.assertEquals("", metaData.getProcedureTerm());
        Assertions.assertEquals("Path", metaData.getCatalogTerm());
        Assertions.assertTrue(metaData.isCatalogAtStart());
        Assertions.assertEquals("/", metaData.getCatalogSeparator());

        Assertions.assertFalse(metaData.supportsSchemasInDataManipulation());
        Assertions.assertFalse(metaData.supportsSchemasInProcedureCalls());
        Assertions.assertFalse(metaData.supportsSchemasInTableDefinitions());
        Assertions.assertFalse(metaData.supportsSchemasInIndexDefinitions());
        Assertions.assertFalse(metaData.supportsSchemasInPrivilegeDefinitions());

        Assertions.assertTrue(metaData.supportsCatalogsInDataManipulation());
        Assertions.assertTrue(metaData.supportsCatalogsInProcedureCalls());
        Assertions.assertTrue(metaData.supportsCatalogsInTableDefinitions());
        Assertions.assertTrue(metaData.supportsCatalogsInIndexDefinitions());
        Assertions.assertTrue(metaData.supportsCatalogsInPrivilegeDefinitions());

        Assertions.assertFalse(metaData.supportsPositionedDelete());
        Assertions.assertFalse(metaData.supportsPositionedUpdate());
        Assertions.assertFalse(metaData.supportsSelectForUpdate());
        Assertions.assertFalse(metaData.supportsStoredProcedures());

        Assertions.assertTrue(metaData.supportsSubqueriesInComparisons());
        Assertions.assertTrue(metaData.supportsSubqueriesInExists());
        Assertions.assertTrue(metaData.supportsSubqueriesInIns());
        Assertions.assertTrue(metaData.supportsSubqueriesInQuantifieds());
        Assertions.assertTrue(metaData.supportsCorrelatedSubqueries());
        Assertions.assertFalse(metaData.supportsUnion());
        Assertions.assertTrue(metaData.supportsUnionAll());
        Assertions.assertTrue(metaData.supportsOpenCursorsAcrossCommit());
        Assertions.assertTrue(metaData.supportsOpenCursorsAcrossRollback());
        Assertions.assertTrue(metaData.supportsOpenStatementsAcrossCommit());
        Assertions.assertTrue(metaData.supportsOpenStatementsAcrossRollback());
        Assertions.assertEquals(YdbConst.MAX_COLUMN_SIZE, metaData.getMaxBinaryLiteralLength());
        Assertions.assertEquals(YdbConst.MAX_COLUMN_SIZE, metaData.getMaxCharLiteralLength());
        Assertions.assertEquals(YdbConst.MAX_COLUMN_NAME_LENGTH, metaData.getMaxColumnNameLength());
        Assertions.assertEquals(YdbConst.MAX_COLUMNS, metaData.getMaxColumnsInGroupBy());
        Assertions.assertEquals(YdbConst.MAX_COLUMNS_IN_PRIMARY_KEY, metaData.getMaxColumnsInIndex());
        Assertions.assertEquals(YdbConst.MAX_COLUMNS, metaData.getMaxColumnsInOrderBy());
        Assertions.assertEquals(YdbConst.MAX_COLUMNS, metaData.getMaxColumnsInSelect());
        Assertions.assertEquals(YdbConst.MAX_COLUMNS, metaData.getMaxColumnsInTable());
        Assertions.assertEquals(YdbConst.MAX_CONNECTIONS, metaData.getMaxConnections());
        Assertions.assertEquals(YdbConst.MAX_ELEMENT_NAME_LENGTH, metaData.getMaxCursorNameLength());
        Assertions.assertEquals(YdbConst.MAX_PRIMARY_KEY_SIZE, metaData.getMaxIndexLength());
        Assertions.assertEquals(YdbConst.MAX_ELEMENT_NAME_LENGTH, metaData.getMaxSchemaNameLength());
        Assertions.assertEquals(YdbConst.MAX_ELEMENT_NAME_LENGTH, metaData.getMaxProcedureNameLength());
        Assertions.assertEquals(YdbConst.MAX_ELEMENT_NAME_LENGTH, metaData.getMaxCatalogNameLength());
        Assertions.assertEquals(YdbConst.MAX_ROW_SIZE, metaData.getMaxRowSize());
        Assertions.assertTrue(metaData.doesMaxRowSizeIncludeBlobs());
        Assertions.assertEquals(YdbConst.MAX_STATEMENT_LENGTH, metaData.getMaxStatementLength());
        Assertions.assertEquals(0, metaData.getMaxStatements());
        Assertions.assertEquals(YdbConst.MAX_ELEMENT_NAME_LENGTH, metaData.getMaxTableNameLength());
        Assertions.assertEquals(0, metaData.getMaxStatements());
        Assertions.assertEquals(YdbConst.MAX_ELEMENT_NAME_LENGTH, metaData.getMaxTableNameLength());
        Assertions.assertEquals(
                YdbConst.TRANSACTION_SERIALIZABLE_READ_WRITE, metaData.getDefaultTransactionIsolation());

        Assertions.assertTrue(metaData.supportsTransactions());
        Assertions.assertTrue(metaData.supportsTransactionIsolationLevel(YdbConst.TRANSACTION_SERIALIZABLE_READ_WRITE));
        Assertions.assertTrue(metaData.supportsTransactionIsolationLevel(YdbConst.ONLINE_CONSISTENT_READ_ONLY));
        Assertions.assertTrue(metaData.supportsTransactionIsolationLevel(YdbConst.ONLINE_INCONSISTENT_READ_ONLY));
        Assertions.assertTrue(metaData.supportsTransactionIsolationLevel(YdbConst.STALE_CONSISTENT_READ_ONLY));

        Assertions.assertFalse(metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED));
        Assertions.assertFalse(metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_NONE));

        Assertions.assertTrue(metaData.supportsDataDefinitionAndDataManipulationTransactions());
        Assertions.assertTrue(metaData.supportsDataManipulationTransactionsOnly());
        Assertions.assertFalse(metaData.dataDefinitionCausesTransactionCommit());
        Assertions.assertTrue(metaData.dataDefinitionIgnoredInTransactions());

        Assertions.assertTrue(metaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
        Assertions.assertTrue(metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));

        Assertions.assertFalse(metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE));

        Assertions.assertTrue(
                metaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
        Assertions.assertTrue(
                metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));

        Assertions.assertFalse(
                metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY));
        Assertions.assertFalse(
                metaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE));
        Assertions.assertFalse(
                metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE));

        Assertions.assertFalse(metaData.ownUpdatesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.ownDeletesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.ownInsertsAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.othersUpdatesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.othersDeletesAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.othersInsertsAreVisible(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.updatesAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.deletesAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));
        Assertions.assertFalse(metaData.insertsAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));

        Assertions.assertTrue(metaData.supportsBatchUpdates());
        Assertions.assertFalse(metaData.supportsSavepoints());
        Assertions.assertFalse(metaData.supportsNamedParameters());
        Assertions.assertFalse(metaData.supportsMultipleOpenResults());
        Assertions.assertFalse(metaData.supportsGetGeneratedKeys());
        Assertions.assertFalse(metaData.generatedKeyAlwaysReturned());

        Assertions.assertTrue(metaData.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT));
        Assertions.assertFalse(metaData.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT));
        Assertions.assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, metaData.getResultSetHoldability());

        Assertions.assertEquals(DatabaseMetaData.sqlStateSQL, metaData.getSQLStateType());
        Assertions.assertFalse(metaData.locatorsUpdateCopy());
        Assertions.assertTrue(metaData.supportsStatementPooling());

        Assertions.assertEquals(RowIdLifetime.ROWID_UNSUPPORTED, metaData.getRowIdLifetime());

        Assertions.assertFalse(metaData.supportsStoredFunctionsUsingCallSyntax());
        Assertions.assertFalse(metaData.autoCommitFailureClosesAllResultSets());
    }

    @Test
    public void unsupportedListsTest() throws SQLException {
        TableAssert.assertEmpty(metaData.getAttributes(null, null, null, null));
        TableAssert.assertEmpty(metaData.getClientInfoProperties());

        TableAssert.assertEmpty(metaData.getSchemas());
        TableAssert.assertEmpty(metaData.getCatalogs());

        TableAssert.assertEmpty(metaData.getProcedures(null, null, null));
        TableAssert.assertEmpty(metaData.getProcedureColumns(null, null, null, null));

        TableAssert.assertEmpty(metaData.getSuperTypes(null, null, null));
        TableAssert.assertEmpty(metaData.getSuperTables(null, null, null));

        TableAssert.assertEmpty(metaData.getFunctions(null, null, null));
        TableAssert.assertEmpty(metaData.getFunctionColumns(null, null, null, null));

        TableAssert.assertEmpty(metaData.getPseudoColumns(null, null, null, null));
        TableAssert.assertEmpty(metaData.getVersionColumns(null, null, null));

        TableAssert.assertEmpty(metaData.getUDTs(null, null, null, null));

        TableAssert.assertEmpty(metaData.getImportedKeys(null, null, null));
        TableAssert.assertEmpty(metaData.getExportedKeys(null, null, null));

        TableAssert.assertEmpty(metaData.getCrossReference(null, null, null, null, null, null));

        TableAssert.assertEmpty(metaData.getColumnPrivileges(null, null, null, null));
        TableAssert.assertEmpty(metaData.getTablePrivileges(null, null, null));
    }

    @Test
    public void getTableTypes() throws SQLException {
        TableAssert types = new TableAssert();
        TableAssert.TextColumn tableType = types.addTextColumn("TABLE_TYPE", "Text");

        types.check(metaData.getTableTypes())
                .assertMetaColumns()
                .nextRow(tableType.eq(TABLE_TYPE)).assertAll()
                .nextRow(tableType.eq(SYSTEM_TABLE_TYPE)).assertAll()
                .assertNoRows();
    }

    @Test
    public void getTypeInfo() throws SQLException {
        short searchNone = (short)DatabaseMetaData.typePredNone;
        short searchBasic = (short)DatabaseMetaData.typePredBasic;
        short searchFull = (short)DatabaseMetaData.typeSearchable;

        TableAssert types = new TableAssert();
        TableAssert.TextColumn name = types.addTextColumn("TYPE_NAME", "Text");
        TableAssert.IntColumn type = types.addIntColumn("DATA_TYPE", "Int32");
        TableAssert.IntColumn precision = types.addIntColumn("PRECISION", "Int32");
        TableAssert.TextColumn prefix = types.addTextColumn("LITERAL_PREFIX", "Text").defaultNull();
        TableAssert.TextColumn suffix = types.addTextColumn("LITERAL_SUFFIX", "Text").defaultNull();
        /* createParams = */types.addTextColumn("CREATE_PARAMS", "Text").defaultNull();
        /* nullable = */types.addIntColumn("NULLABLE", "Int32").defaultValue(DatabaseMetaData.typeNullable);
        /* caseSensitive = */types.addBoolColumn("CASE_SENSITIVE", "Bool").defaultValue(true);
        TableAssert.ShortColumn searchable = types.addShortColumn("SEARCHABLE", "Int16").defaultValue(searchBasic);
        TableAssert.BoolColumn unsigned = types.addBoolColumn("UNSIGNED_ATTRIBUTE", "Bool");
        TableAssert.BoolColumn fixedPrec = types.addBoolColumn("FIXED_PREC_SCALE", "Bool").defaultValue(false);
        /* autoIncrement = */types.addBoolColumn("AUTO_INCREMENT", "Bool").defaultValue(false);
        /* localName = */types.addTextColumn("LOCAL_TYPE_NAME", "Text").defaultNull();
        TableAssert.IntColumn minScale = types.addIntColumn("MINIMUM_SCALE", "Int32").defaultValue(0);
        TableAssert.IntColumn maxScale = types.addIntColumn("MAXIMUM_SCALE", "Int32").defaultValue(0);
        /* sqlDataType = */types.addIntColumn("SQL_DATA_TYPE", "Int32").defaultValue(0);
        /* sqlDatetimeSub = */types.addIntColumn("SQL_DATETIME_SUB", "Int32").defaultValue(0);
        /* numPrecRadix = */types.addIntColumn("NUM_PREC_RADIX", "Int32").defaultValue(10);

        TableAssert.ResultSetAssert rs = types.check(metaData.getTypeInfo())
                .assertMetaColumns();

        rs.nextRow(name.eq("Int64"), type.eq(Types.BIGINT), precision.eq(8), unsigned.eq(false)).assertAll();
        rs.nextRow(name.eq("Uint32"), type.eq(Types.BIGINT), precision.eq(4), unsigned.eq(true)).assertAll();
        rs.nextRow(name.eq("Uint64"), type.eq(Types.BIGINT), precision.eq(8), unsigned.eq(true)).assertAll();
        rs.nextRow(name.eq("Interval"), type.eq(Types.BIGINT), precision.eq(8), unsigned.eq(false)).assertAll();

        rs.nextRow(name.eq("Bytes"), type.eq(Types.BINARY), precision.eq(YdbConst.MAX_COLUMN_SIZE),
                prefix.eq("'"), suffix.eq("'"), unsigned.eq(false), searchable.eq(searchFull)).assertAll();
        rs.nextRow(name.eq("Yson"), type.eq(Types.BINARY), precision.eq(YdbConst.MAX_COLUMN_SIZE),
                prefix.eq("'"), suffix.eq("'"), unsigned.eq(false), searchable.eq(searchNone)).assertAll();
        rs.nextRow(name.eq("Decimal(22, 9)"), type.eq(Types.DECIMAL), precision.eq(16),
                unsigned.eq(false), fixedPrec.eq(true), minScale.eq(9), maxScale.eq(9)).assertAll();

        rs.nextRow(name.eq("Int32"), type.eq(Types.INTEGER), precision.eq(4), unsigned.eq(false)).assertAll();
        rs.nextRow(name.eq("Uint8"), type.eq(Types.INTEGER), precision.eq(1), unsigned.eq(true)).assertAll();
        rs.nextRow(name.eq("Float"), type.eq(Types.FLOAT), precision.eq(4), unsigned.eq(false)).assertAll();
        rs.nextRow(name.eq("Double"), type.eq(Types.DOUBLE), precision.eq(8), unsigned.eq(false)).assertAll();

        rs.nextRow(name.eq("Text"), type.eq(Types.VARCHAR), precision.eq(YdbConst.MAX_COLUMN_SIZE),
                prefix.eq("'"), suffix.eq("'"), unsigned.eq(false), searchable.eq(searchFull)).assertAll();
        rs.nextRow(name.eq("Json"), type.eq(Types.VARCHAR), precision.eq(YdbConst.MAX_COLUMN_SIZE),
                prefix.eq("'"), suffix.eq("'"), unsigned.eq(false), searchable.eq(searchNone)).assertAll();
        rs.nextRow(name.eq("JsonDocument"), type.eq(Types.VARCHAR), precision.eq(YdbConst.MAX_COLUMN_SIZE),
                prefix.eq("'"), suffix.eq("'"), unsigned.eq(false), searchable.eq(searchNone)).assertAll();

        rs.nextRow(name.eq("Bool"), type.eq(Types.BOOLEAN), precision.eq(1), unsigned.eq(false)).assertAll();
        rs.nextRow(name.eq("Date"), type.eq(Types.DATE), precision.eq(10), unsigned.eq(false)).assertAll();
        rs.nextRow(name.eq("Datetime"), type.eq(Types.TIME), precision.eq(19), unsigned.eq(false)).assertAll();
        rs.nextRow(name.eq("Timestamp"), type.eq(Types.TIMESTAMP), precision.eq(26), unsigned.eq(false)).assertAll();

        rs.assertNoRows();
    }

    @Test
    public void getTables() throws SQLException {
        List<String> simpleTables = Arrays.asList(
                "dir1/t1",
                "dir1/t2",
                "dir2/dir1/t1",
                "dir2/t1",
                "t1",
                "table_keys",
                "all_types"
        );

        TableAssert tables  = new TableAssert();
        /* tableCatalog = */ tables.addTextColumn("TABLE_CAT", "Text").defaultNull();
        /* tableSchema = */  tables.addTextColumn("TABLE_SCHEM", "Text").defaultNull();
        TableAssert.TextColumn tableName = tables.addTextColumn("TABLE_NAME", "Text");
        TableAssert.TextColumn tableType = tables.addTextColumn("TABLE_TYPE", "Text");
        /* remarks = */ tables.addTextColumn("REMARKS", "Text").defaultNull();
        /* typeCatalog = */ tables.addTextColumn("TYPE_CAT", "Text").defaultNull();
        /* typeSchema = */ tables.addTextColumn("TYPE_SCHEM", "Text").defaultNull();
        /* typeName = */ tables.addTextColumn("TYPE_NAME", "Text").defaultNull();
        /* selfRefColName = */ tables.addTextColumn("SELF_REFERENCING_COL_NAME", "Text").defaultNull();
        /* refGeneration = */ tables.addTextColumn("REF_GENERATION", "Text").defaultNull();

        // wrong filters
        TableAssert.assertEmpty(metaData.getTables("-", null, null, null));
        TableAssert.assertEmpty(metaData.getTables(null, "-", null, null));
        TableAssert.assertEmpty(metaData.getTables(null, "-", "unknown-table", null));
        TableAssert.assertEmpty(metaData.getTables(null, "-", null, asArray("U-1")));
        TableAssert.assertEmpty(metaData.getTables(null, "-", null, new String[0]));

        // fetch system tables
        List<String> systemTables = new ArrayList<>();

        ResultSet fetchSystem = metaData.getTables(null, null, null, asArray(SYSTEM_TABLE_TYPE));
        tables.check(fetchSystem).assertMetaColumns();
        while (fetchSystem.next()) {
            systemTables.add(fetchSystem.getString(tableName.name()));
        }

        // read all tables
        TableAssert.ResultSetAssert rs = tables.check(metaData.getTables(null, null, null, null))
                .assertMetaColumns();
        // system tables are first
        for (String name: systemTables) {
            rs.nextRow(tableName.eq(name), tableType.eq(SYSTEM_TABLE_TYPE));
        }
        for (String name: simpleTables) {
            rs.nextRow(tableName.eq(name), tableType.eq(TABLE_TYPE));
        }
        // TODO: fix
        //rs.assertNoRows();

        // read only non system tables
        rs = tables.check(metaData.getTables(null, null, null, asArray(TABLE_TYPE)))
                .assertMetaColumns();
        for (String name: simpleTables) {
            rs.nextRow(tableName.eq(name), tableType.eq(TABLE_TYPE));
        }
        // TODO: fix
        //rs.assertNoRows();

        // read multipli filters
        rs = tables.check(metaData.getTables(null, null, null, asArray(TABLE_TYPE, "some string", SYSTEM_TABLE_TYPE)))
                .assertMetaColumns();
        // system tables are first
        for (String name: systemTables) {
            rs.nextRow(tableName.eq(name), tableType.eq(SYSTEM_TABLE_TYPE));
        }
        for (String name: simpleTables) {
            rs.nextRow(tableName.eq(name), tableType.eq(TABLE_TYPE));
        }
        // TODO: fix
        //rs.assertNoRows();

        // filter by name
        rs = tables.check(metaData.getTables(null, null, ALL_TYPES_TABLE, asArray(TABLE_TYPE)))
                .assertMetaColumns();
        rs.nextRow(tableName.eq(ALL_TYPES_TABLE), tableType.eq(TABLE_TYPE));
        rs.assertNoRows();

        // filter by name
        TableAssert.assertEmpty(metaData.getTables(null, null, "dir1/t1", asArray(SYSTEM_TABLE_TYPE)));
    }

    @Test
    public void getColumns() throws SQLException {
        TableAssert.assertEmpty(metaData.getColumns("-", null, null, null));
        TableAssert.assertEmpty(metaData.getColumns(null, "-", null, null));
        TableAssert.assertEmpty(metaData.getColumns(null, "-", "unknown-table", null));
        TableAssert.assertEmpty(metaData.getColumns(null, "-", null, "x-column-unknown"));

        TableAssert columns = new TableAssert();
        columns.addTextColumn("TABLE_CAT", "Text").defaultNull();
        columns.addTextColumn("TABLE_SCHEM", "Text").defaultNull();
        columns.addTextColumn("TABLE_NAME", "Text").defaultValue(ALL_TYPES_TABLE);
        TableAssert.TextColumn columnName = columns.addTextColumn("COLUMN_NAME", "Text");
        TableAssert.IntColumn dataType = columns.addIntColumn("DATA_TYPE", "Int32");
        TableAssert.TextColumn typeName = columns.addTextColumn("TYPE_NAME", "Text");
        TableAssert.IntColumn columnSize = columns.addIntColumn("COLUMN_SIZE", "Int32");
        columns.addIntColumn("BUFFER_LENGTH", "Int32").defaultValue(0);
        TableAssert.ShortColumn decimalDigits = columns.addShortColumn("DECIMAL_DIGITS", "Int16")
                .defaultValue((short)0);
        columns.addIntColumn("NUM_PREC_RADIX", "Int32").defaultValue(10);
        columns.addIntColumn("NULLABLE", "Int32").defaultValue(DatabaseMetaData.columnNullable);
        columns.addTextColumn("REMARKS", "Text").defaultNull();
        columns.addTextColumn("COLUMN_DEF", "Text").defaultNull();
        columns.addIntColumn("SQL_DATA_TYPE", "Int32").defaultValue(0);
        columns.addIntColumn("SQL_DATETIME_SUB", "Int32").defaultValue(0);
        columns.addIntColumn("CHAR_OCTET_LENGTH", "Int32").defaultValue(0);
        TableAssert.ShortColumn ordinal = columns.addShortColumn("ORDINAL_POSITION", "Int16");
        columns.addTextColumn("IS_NULLABLE", "Text").defaultValue("YES");
        columns.addTextColumn("SCOPE_CATALOG", "Text").defaultNull();
        columns.addTextColumn("SCOPE_SCHEMA", "Text").defaultNull();
        columns.addTextColumn("SCOPE_TABLE", "Text").defaultNull();
        columns.addShortColumn("SOURCE_DATA_TYPE", "Int16").defaultValue((short)0);
        columns.addTextColumn("IS_AUTOINCREMENT", "Text").defaultValue("NO");
        columns.addTextColumn("IS_GENERATEDCOLUMN", "Text").defaultValue("NO");

        // get all columns for ALL_TYPES_TABLE
        TableAssert.ResultSetAssert rs = columns.check(metaData.getColumns(null, null, ALL_TYPES_TABLE, null))
                .assertMetaColumns();

        rs.nextRow(columnName.eq("key"), dataType.eq(Types.INTEGER), typeName.eq("Int32"),
                columnSize.eq(4), ordinal.eq((short)1)).assertAll();

        rs.nextRow(columnName.eq("c_Bool"), dataType.eq(Types.BOOLEAN), typeName.eq("Bool"),
                columnSize.eq(1), ordinal.eq((short)2)).assertAll();
        rs.nextRow(columnName.eq("c_Int32"), dataType.eq(Types.INTEGER), typeName.eq("Int32"),
                columnSize.eq(4), ordinal.eq((short)3)).assertAll();
        rs.nextRow(columnName.eq("c_Int64"), dataType.eq(Types.BIGINT), typeName.eq("Int64"),
                columnSize.eq(8), ordinal.eq((short)4)).assertAll();
        rs.nextRow(columnName.eq("c_Uint8"), dataType.eq(Types.INTEGER), typeName.eq("Uint8"),
                columnSize.eq(1), ordinal.eq((short)5)).assertAll();
        rs.nextRow(columnName.eq("c_Uint32"), dataType.eq(Types.BIGINT), typeName.eq("Uint32"),
                columnSize.eq(4), ordinal.eq((short)6)).assertAll();
        rs.nextRow(columnName.eq("c_Uint64"), dataType.eq(Types.BIGINT), typeName.eq("Uint64"),
                columnSize.eq(8), ordinal.eq((short)7)).assertAll();
        rs.nextRow(columnName.eq("c_Float"), dataType.eq(Types.FLOAT), typeName.eq("Float"),
                columnSize.eq(4), ordinal.eq((short)8)).assertAll();
        rs.nextRow(columnName.eq("c_Double"), dataType.eq(Types.DOUBLE), typeName.eq("Double"),
                columnSize.eq(8), ordinal.eq((short)9)).assertAll();
        rs.nextRow(columnName.eq("c_Bytes"), dataType.eq(Types.BINARY), typeName.eq("Bytes"),
                columnSize.eq(YdbConst.MAX_COLUMN_SIZE), ordinal.eq((short)10)).assertAll();
        rs.nextRow(columnName.eq("c_Text"), dataType.eq(Types.VARCHAR), typeName.eq("Text"),
                columnSize.eq(YdbConst.MAX_COLUMN_SIZE), ordinal.eq((short)11)).assertAll();
        rs.nextRow(columnName.eq("c_Json"), dataType.eq(Types.VARCHAR), typeName.eq("Json"),
                columnSize.eq(YdbConst.MAX_COLUMN_SIZE), ordinal.eq((short)12)).assertAll();
        rs.nextRow(columnName.eq("c_JsonDocument"), dataType.eq(Types.VARCHAR), typeName.eq("JsonDocument"),
                columnSize.eq(YdbConst.MAX_COLUMN_SIZE), ordinal.eq((short)13)).assertAll();
        rs.nextRow(columnName.eq("c_Yson"), dataType.eq(Types.BINARY), typeName.eq("Yson"),
                columnSize.eq(YdbConst.MAX_COLUMN_SIZE), ordinal.eq((short)14)).assertAll();
        rs.nextRow(columnName.eq("c_Date"), dataType.eq(Types.DATE), typeName.eq("Date"),
                columnSize.eq(10), ordinal.eq((short)15)).assertAll();
        rs.nextRow(columnName.eq("c_Datetime"), dataType.eq(Types.TIME), typeName.eq("Datetime"),
                columnSize.eq(19), ordinal.eq((short)16)).assertAll();
        rs.nextRow(columnName.eq("c_Timestamp"), dataType.eq(Types.TIMESTAMP), typeName.eq("Timestamp"),
                columnSize.eq(26), ordinal.eq((short)17)).assertAll();
        rs.nextRow(columnName.eq("c_Interval"), dataType.eq(Types.BIGINT), typeName.eq("Interval"),
                columnSize.eq(8), ordinal.eq((short)18)).assertAll();
        rs.nextRow(columnName.eq("c_Decimal"), dataType.eq(Types.DECIMAL), typeName.eq("Decimal(22, 9)"),
                columnSize.eq(16), ordinal.eq((short)19), decimalDigits.eq((short)22)).assertAll();

        rs.assertNoRows();

        // find only one column
        rs = columns.check(metaData.getColumns(null, null, ALL_TYPES_TABLE, "c_JsonDocument"))
            .assertMetaColumns();
        rs.nextRow(columnName.eq("c_JsonDocument"), dataType.eq(Types.VARCHAR), typeName.eq("JsonDocument"),
                columnSize.eq(YdbConst.MAX_COLUMN_SIZE), ordinal.eq((short)13)).assertAll();
        rs.assertNoRows();
    }

    @Test
    public void getPrimaryKeys() throws SQLException {
        TableAssert.assertEmpty(metaData.getPrimaryKeys("-", null, null));
        TableAssert.assertEmpty(metaData.getPrimaryKeys(null, "-", null));
        TableAssert.assertEmpty(metaData.getPrimaryKeys(null, null, "-"));

        // table name is a must
        TableAssert.assertEmpty(metaData.getPrimaryKeys(null, null, null));

        TableAssert primaryKeys = new TableAssert();
        primaryKeys.addTextColumn("TABLE_CAT", "Text").defaultNull();
        primaryKeys.addTextColumn("TABLE_SCHEM", "Text").defaultNull();
        TableAssert.TextColumn table = primaryKeys.addTextColumn("TABLE_NAME", "Text");
        TableAssert.TextColumn name = primaryKeys.addTextColumn("COLUMN_NAME", "Text");
        TableAssert.ShortColumn keySeq = primaryKeys.addShortColumn("KEY_SEQ", "Int16");
        primaryKeys.addTextColumn("PK_NAME", "Text").defaultNull();

        // ALL_TYPES_TABLE has simple primary key
        primaryKeys.check(metaData.getPrimaryKeys(null, null, ALL_TYPES_TABLE))
                .assertMetaColumns()
                .nextRow(table.eq(ALL_TYPES_TABLE), name.eq("key"), keySeq.eq((short)1)).assertAll()
                .assertNoRows();

        // INDEXES_TABLE has composite primary key
        primaryKeys.check(metaData.getPrimaryKeys(null, null, INDEXES_TABLE))
                .assertMetaColumns()
                .nextRow(table.eq(INDEXES_TABLE), name.eq("key1"), keySeq.eq((short)1)).assertAll()
                .nextRow(table.eq(INDEXES_TABLE), name.eq("key2"), keySeq.eq((short)2)).assertAll()
                .assertNoRows();
    }

    @Test
    public void getIndexInfo() throws SQLException {
        TableAssert.assertEmpty(metaData.getIndexInfo("-", null, null, false, false));
        TableAssert.assertEmpty(metaData.getIndexInfo(null, "-", null, false, false));
        TableAssert.assertEmpty(metaData.getIndexInfo(null, null, "-", false, false));

        // no unique indexes
        TableAssert.assertEmpty(metaData.getIndexInfo(null, null, null, true, false));

        // table name is a must
        TableAssert.assertEmpty(metaData.getIndexInfo(null, null, null, false, false));

        TableAssert indexes = new TableAssert();
        indexes.addTextColumn("TABLE_CAT", "Text").defaultNull();
        indexes.addTextColumn("TABLE_SCHEM", "Text").defaultNull();
        TableAssert.TextColumn tableName = indexes.addTextColumn("TABLE_NAME", "Text");
        indexes.addBoolColumn("NON_UNIQUE", "Bool").defaultValue(true);
        indexes.addTextColumn("INDEX_QUALIFIER", "Text").defaultNull();
        TableAssert.TextColumn indexName = indexes.addTextColumn("INDEX_NAME", "Text");
        TableAssert.ShortColumn type = indexes.addShortColumn("TYPE", "Int16");
        TableAssert.ShortColumn ordinal = indexes.addShortColumn("ORDINAL_POSITION", "Int16");
        TableAssert.TextColumn columnName = indexes.addTextColumn("COLUMN_NAME", "Text");
        indexes.addTextColumn("ASC_OR_DESC", "Text").defaultNull();
        indexes.addIntColumn("CARDINALITY", "Int32").defaultValue(0);
        indexes.addIntColumn("PAGES", "Int32").defaultValue(0);
        indexes.addTextColumn("FILTER_CONDITION", "Text").defaultNull();

        indexes.check(metaData.getIndexInfo(null, null, INDEXES_TABLE, false, false))
                .assertMetaColumns()
                .nextRow(tableName.eq(INDEXES_TABLE), indexName.eq("idx_1"), columnName.eq("value3"),
                        type.eq(DatabaseMetaData.tableIndexHashed), ordinal.eq((short)1)).assertAll()
                .nextRow(tableName.eq(INDEXES_TABLE), indexName.eq("idx_2"), columnName.eq("value1"),
                        type.eq(DatabaseMetaData.tableIndexHashed), ordinal.eq((short)1)).assertAll()
                .nextRow(tableName.eq(INDEXES_TABLE), indexName.eq("idx_2"), columnName.eq("value2"),
                        type.eq(DatabaseMetaData.tableIndexHashed), ordinal.eq((short)2)).assertAll()
                .assertNoRows();

        indexes.check(metaData.getIndexInfo(null, null, ALL_TYPES_TABLE, false, false))
                .assertNoRows();
    }

    @Test
    public void getBestRowIdentifier() throws SQLException {
        TableAssert.assertEmpty(metaData.getBestRowIdentifier("-", null, null, DatabaseMetaData.bestRowSession, true));
        TableAssert.assertEmpty(metaData.getBestRowIdentifier(null, "-", null, DatabaseMetaData.bestRowSession, true));
        TableAssert.assertEmpty(metaData.getBestRowIdentifier(null, null, "-", DatabaseMetaData.bestRowSession, true));

        // expect exact column name
        TableAssert.assertEmpty(metaData.getBestRowIdentifier(null, null, null, DatabaseMetaData.bestRowSession, true));

        // only nullable columns supported
        TableAssert.assertEmpty(metaData
                .getBestRowIdentifier(null, null, ALL_TYPES_TABLE, DatabaseMetaData.bestRowSession, false));

        TableAssert rowIdentifiers = new TableAssert();
        TableAssert.ShortColumn scope = rowIdentifiers.addShortColumn("SCOPE", "Int16");
        TableAssert.TextColumn name = rowIdentifiers.addTextColumn("COLUMN_NAME", "Text");
        TableAssert.IntColumn dataType = rowIdentifiers.addIntColumn("DATA_TYPE", "Int32");
        TableAssert.TextColumn typeName = rowIdentifiers.addTextColumn("TYPE_NAME", "Text");
        rowIdentifiers.addIntColumn("COLUMN_SIZE", "Int32").defaultValue(0);
        rowIdentifiers.addIntColumn("BUFFER_LENGTH", "Int32").defaultValue(0);
        rowIdentifiers.addShortColumn("DECIMAL_DIGITS", "Int16").defaultValue((short)0);
        rowIdentifiers.addIntColumn("PSEUDO_COLUMN", "Int32").defaultValue(DatabaseMetaData.bestRowNotPseudo);

        rowIdentifiers.check(metaData
                .getBestRowIdentifier(null, null, ALL_TYPES_TABLE, DatabaseMetaData.bestRowSession, true))
                .assertMetaColumns()
                .nextRow(name.eq("key"), dataType.eq(Types.INTEGER), typeName.eq("Int32"),
                        scope.eq((short)DatabaseMetaData.bestRowSession)).assertAll()
                .assertNoRows();

        rowIdentifiers.check(metaData
                .getBestRowIdentifier(null, null, ALL_TYPES_TABLE, DatabaseMetaData.bestRowTransaction, true))
                .assertMetaColumns()
                .nextRow(name.eq("key"), dataType.eq(Types.INTEGER), typeName.eq("Int32"),
                        scope.eq((short)DatabaseMetaData.bestRowTransaction)).assertAll()
                .assertNoRows();

        rowIdentifiers.check(metaData
                .getBestRowIdentifier(null, null, INDEXES_TABLE, DatabaseMetaData.bestRowTemporary, true))
                .assertMetaColumns()
                .nextRow(name.eq("key1"), dataType.eq(Types.INTEGER), typeName.eq("Int32"),
                        scope.eq((short)DatabaseMetaData.bestRowTemporary)).assertAll()
                .nextRow(name.eq("key2"), dataType.eq(Types.VARCHAR), typeName.eq("Text"),
                        scope.eq((short)DatabaseMetaData.bestRowTemporary)).assertAll()
                .assertNoRows();
    }

    private static String[] asArray(String... args) {
        return args;
    }
}
