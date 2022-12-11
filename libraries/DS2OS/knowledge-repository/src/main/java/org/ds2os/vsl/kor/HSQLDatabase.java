package org.ds2os.vsl.kor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.config.VslKORDatabaseConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.statistics.VslStatisticsDatapoint;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the access/storing of the nodetree data like nodes and their
 * types/values/access rights.
 *
 * @author liebald
 */
public class HSQLDatabase implements VslNodeDatabase {

    /**
     * The jdbc protocol identifier.
     */
    private static final String HSQLDB_PROTOCOL = "jdbc:hsqldb:file:";

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HSQLDatabase.class);

    /**
     * JDBC connection.
     */
    private Connection con = null;

    /**
     * JDBC driver for the hsqldb.
     */
    private final String dbDriver = "org.hsqldb.jdbcDriver";

    /**
     * The configuration service.
     */
    private final VslKORDatabaseConfig configService;

    /**
     * Stores the last extended timeStamp used for an set operation. See
     * {@link HSQLDatabase#getExtendedTimestamp()}.
     */
    private BigDecimal lastTimestampSet;

    /**
     * Name of the table inside the database that holds our data.
     */
    private final String tableData;

    /**
     * Name of the table inside the database that holds our structure information.
     */
    private final String tableStructure;

    /**
     * Name of the table inside the database that holds version related Information.
     */
    private final String tableVersion;

    /**
     * The {@link VslStatisticsProvider} for accessing the KA internal statistics mechanism.
     */
    private final VslStatisticsProvider statisticsProvider;

    /**
     * A {@link Timer} used for regularly running the {@link HSQLDBmaintenance} task for db cleanup.
     */
    Timer maintentanceTimer;

    /**
     * Instantiate a new database. This creates/connects the underlying database and table.
     *
     * @param configService
     *            The initial configuration.
     * @param statisticsProvider
     *            The {@link VslStatisticsProvider} for accessing the KA internal statistics
     *            mechanism.
     */
    public HSQLDatabase(final VslKORDatabaseConfig configService,
            final VslStatisticsProvider statisticsProvider) {
        tableStructure = "kor_structure";
        tableVersion = "kor_version";
        tableData = "kor_data";
        this.configService = configService;
        lastTimestampSet = new BigDecimal(System.currentTimeMillis());
        lastTimestampSet = lastTimestampSet.setScale(5);
        this.statisticsProvider = statisticsProvider;
        maintentanceTimer = new Timer();
    }

    @Override
    public final void activate() {
        final VslStatisticsDatapoint dp = statisticsProvider
                .getStatistics(this.getClass(), "activate").begin();
        try {
            Class.forName(dbDriver);
            synchronized (LOGGER) {
                if (con == null) {
                    con = DriverManager.getConnection(
                            HSQLDB_PROTOCOL + this.configService.getDatabasePath(),
                            this.configService.getDatabaseUsername(),
                            this.configService.getDatabasePassword());
                    con.setAutoCommit(true);
                    con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    setDatabaseFlags();
                    initKnowledgeTables();
                }
            }
            final HSQLDBmaintenance maintentanceThread = new HSQLDBmaintenance(con, configService,
                    tableData, tableVersion);
            maintentanceTimer.schedule(maintentanceThread, 5000, 3000);
        } catch (final SQLException e) {
            LOGGER.error("Cannot connect to database at {}: {}",
                    HSQLDB_PROTOCOL + configService.getDatabasePath(), e.getMessage());
        } catch (final ClassNotFoundException e) {
            LOGGER.error("Could not instantiate the database driver class: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        dp.end();
    }

    @Override
    public final void addNode(final String address, final List<String> types,
            final List<String> readerIds, final List<String> writerIds, final String restriction,
            final String cacheParameters) {

        final List<String> wellEscapedTypes = new ArrayList<String>();

        if (types != null) {
            for (final String type : types) {
                wellEscapedTypes.add(AddressParser.makeWellFormedAddress(type));
            }
        }
        // LOGGER.debug("Inserting new node at {}", address);
        PreparedStatement pst = null;
        try {
            final StringBuilder sql = new StringBuilder();
            LOGGER.trace("Inserting new node at {}", address);

            sql.append("INSERT INTO ").append(tableStructure);
            sql.append(" (address, type, reader, writer, restriction, cacheParameters) VALUES ");
            sql.append("(?,?,?,?,?,?)");

            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            pst.setString(2, StringUtils.join(wellEscapedTypes, LIST_SEPARATOR));
            pst.setString(3, StringUtils.join(readerIds, LIST_SEPARATOR));
            pst.setString(4, StringUtils.join(writerIds, LIST_SEPARATOR));
            pst.setString(5, restriction);
            pst.setString(6, cacheParameters);
            pst.execute();
            LOGGER.trace("Created a new node at {}", address);
        } catch (final SQLException e) {
            LOGGER.error("Error adding a new node at address {}: ", address, e);
        } finally {
            closeResource(pst);
        }
    }

    /**
     * Adds a table to the database.
     *
     * @param tableName
     *            the name of the table.
     * @param sqlCreateStmt
     *            the sql statement to create the table.
     */
    private void addTable(final String tableName, final String sqlCreateStmt) {
        Statement st = null;
        ResultSet rs = null;

        try {
            final DatabaseMetaData meta = con.getMetaData();
            rs = meta.getTables(null, null, tableName.toUpperCase(), null);

            if (!rs.next()) {
                if (sqlCreateStmt != null && !sqlCreateStmt.isEmpty()) {
                    st = con.createStatement();
                    st.execute(sqlCreateStmt);
                    con.commit();
                }
            } else {
                LOGGER.debug("Table {} already exists.", tableName);
            }
        } catch (final SQLException e) {
            LOGGER.info("Could not create new table {} with statement: {}, Error: {}", tableName,
                    sqlCreateStmt, e.getMessage());
        } finally {
            closeResource(st);
            closeResource(rs);
        }
    }

    /**
     * Tries to gracefully close the prepared statement.
     *
     * @param pst
     *            The prepared statement object to close.
     */
    private void closeResource(final PreparedStatement pst) {
        if (pst != null) {
            try {
                pst.close();
            } catch (final SQLException e) {
                LOGGER.warn("Couldn't clean up a prepared statement: {}", e.getLocalizedMessage());
            }
        }
    }

    /**
     * Tries to gracefully close the result set.
     *
     * @param rs
     *            The result set object to close.
     */
    private void closeResource(final ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (final SQLException e) {
                LOGGER.warn("Couldn't clean up a result set object: {}", e.getLocalizedMessage());
            }
        }
    }

    /**
     * Tries to gracefully close the statement.
     *
     * @param st
     *            The statement object to close.
     */
    private void closeResource(final Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (final SQLException e) {
                LOGGER.warn("Couldn't clean up a statement: {}", e.getLocalizedMessage());
            }
        }
    }

    @Override
    public final List<String> getAddressesOfType(final String rootAddress, final String type) {
        final LinkedList<String> resultingAddresses = new LinkedList<String>();
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT address, type FROM ").append(tableStructure).append(" ");
        sql.append("WHERE address = ? OR address LIKE ? ");
        sql.append("AND type LIKE ?");

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, rootAddress);
            if (rootAddress.endsWith("/")) {
                pst.setString(2, rootAddress + "%");
            } else {
                pst.setString(2, rootAddress + "/%");
            }
            // NOTE this wildcard matches a/b/c as well as /b/c/d when looking for /b/c
            // NOTE it also matches /b/cde, need later filtering
            pst.setString(3, "%" + type + "%");
            rs = pst.executeQuery();

            while (rs.next()) {
                // check that the actual type is contained in the result
                final List<String> types = new ArrayList<String>(
                        Arrays.asList(StringUtils.split(rs.getString("type"), LIST_SEPARATOR)));
                if (types.contains(type)) {
                    // LOGGER.debug("Type {} found at {}", type, rs.getString("address"));
                    resultingAddresses.add(rs.getString("address"));
                }
            }
            return resultingAddresses;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeResource(rs);
            closeResource(pst);
        }
    }

    // @Override
    // public final List<String> getAllChildAddresses(final String address)
    // throws NodeNotExistingException {
    // if (address == null) {
    // return null;
    // }
    // if (!nodeExists(address)) {
    // throw new NodeNotExistingException("No node at: " + address);
    // }
    // PreparedStatement pst = null;
    // ResultSet rs = null;
    // try {
    // String like;
    // if (address.equals("/")) {
    // like = address + "%";
    // } else {
    // like = address + "/%";
    // }
    // final List<String> result = new LinkedList<String>();
    // final StringBuilder sql = new StringBuilder();
    // sql.append("SELECT address FROM ").append(tableStructure);
    // sql.append(" WHERE address LIKE ?");
    //
    // pst = con.prepareStatement(sql.toString());
    // pst.setString(1, like);
    // rs = pst.executeQuery();
    // while (rs.next()) {
    // final String resultAddress = rs.getString("address");
    // if (!resultAddress.equals(address)) {
    // result.add(resultAddress);
    // }
    // }
    // return result;
    // } catch (final SQLException e) {
    // throw new RuntimeException(e);
    // } finally {
    // closeResource(pst);
    // closeResource(rs);
    // }
    // }
    //
    // @Override
    // public final List<String> getDirectChildrenAddresses(final String address)
    // throws NodeNotExistingException {
    // if (address == null) {
    // return null;
    // }
    //
    // if (!nodeExists(address)) {
    // throw new NodeNotExistingException("No node at: " + address);
    // }
    // PreparedStatement pst = null;
    // ResultSet rs = null;
    // try {
    // String like;
    // String notLike;
    // if (address.equals("/")) {
    // like = address + "%";
    // notLike = address + "%/%";
    // } else {
    // like = address + "/%";
    // notLike = address + "%/%/%";
    // }
    // final List<String> result = new LinkedList<String>();
    // final StringBuilder sql = new StringBuilder();
    // sql.append("SELECT address FROM ").append(tableStructure);
    // sql.append(" WHERE address LIKE ?");
    // sql.append(" AND address NOT LIKE ?");
    // pst = con.prepareStatement(sql.toString());
    // pst.setString(1, like);
    // pst.setString(2, notLike);
    // rs = pst.executeQuery();
    // while (rs.next()) {
    // final String resultAddress = rs.getString("address");
    // if (!resultAddress.equals(address)) {
    // result.add(resultAddress);
    // }
    // }
    // return result;
    // } catch (final SQLException e) {
    // throw new RuntimeException(e);
    // } finally {
    // closeResource(pst);
    // closeResource(rs);
    // }
    // }

    /**
     * Returns an BigDecimal which can be used as TimeStamp. Creation is
     * {@link System#currentTimeMillis()}+offset. The offset is 0 except in case the timeStamp part
     * of the last extendedTimeStamp used ( {@link HSQLDatabase#lastTimestampSet}) is equal to the
     * current extendedTimeStamp, in which case the offset is the offset of the last
     * extendedTimeStamp + 1/10000. Result is e.g. 1234567890.00001 where 1234567890 is the current
     * time and 00001 is the offset.
     *
     * @return the current Time as BigDecimal with offset.
     */
    private BigDecimal getExtendedTimestamp() {
        final Long currentTime = System.currentTimeMillis();

        final int scale = 5;
        BigDecimal extendedTimestamp = new BigDecimal(currentTime);
        extendedTimestamp = extendedTimestamp.setScale(scale);
        if (lastTimestampSet.longValue() == extendedTimestamp.longValue()) {
            // get the current offset
            BigDecimal offset = lastTimestampSet.remainder(BigDecimal.ONE);

            // shouldn'tbe necessesary with a scale of 5 (10000 set operations per millisecond can
            // be distinguished:
            // take care of overflow (if we have e.g. 123.9 we want to go to 123.91 and not 124)
            // if (offset.add(BigDecimal.ONE.divide(new BigDecimal(Math.pow(10, scale)))).compareTo(
            // BigDecimal.ONE) == 0) {
            // scale++;
            // }
            // increment the offset by 1/(10*scale)
            offset = offset.add(BigDecimal.ONE.divide(new BigDecimal(Math.pow(10, scale))));
            extendedTimestamp = extendedTimestamp.add(offset);
        }
        // LOGGER.debug("extendedTimestamp: {}", extendedTimestamp);
        return extendedTimestamp;
    }

    @Override
    public final String getHashOfSubtree(final String rootAddr, final List<String> exludeSubtrees) {
        String treeRoot = rootAddr;
        if (treeRoot == null || treeRoot.isEmpty()) {
            treeRoot = "/";
        }
        long result = 17;
        final String[] hashComponents = { "address", "type" };
        // , "reader", "writer", "restriction" };
        // reader, writer and restrictions are based/fixed on/per the model, which is stored in the
        // type. Therefore its's not necessary to include them in the hash.
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            String like;
            if (treeRoot.equals("/") || treeRoot.endsWith("/")) {
                like = treeRoot + "%";
            } else {
                like = treeRoot + "/%";
            }
            final StringBuilder sql = new StringBuilder();
            sql.append("SELECT address, reader, writer, restriction, type, cacheParameters FROM ")
                    .append(tableStructure);
            sql.append(" WHERE (address = ? OR address LIKE ?) ");

            for (int i = 0; i < exludeSubtrees.size(); i++) {
                sql.append(" AND address <> ? AND address NOT LIKE ? ");
            }
            sql.append(" ORDER BY address");
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, treeRoot);
            pst.setString(2, like);

            for (int i = 0; i < exludeSubtrees.size(); i++) {
                if (treeRoot.endsWith("/")) {
                    pst.setString(2 * i + 3, treeRoot + exludeSubtrees.get(i));
                    pst.setString(2 * i + 4, treeRoot + exludeSubtrees.get(i) + "/%");
                } else {
                    pst.setString(2 * i + 3, treeRoot + "/" + exludeSubtrees.get(i));
                    pst.setString(2 * i + 4, treeRoot + "/" + exludeSubtrees.get(i) + "/%");
                }
            }

            rs = pst.executeQuery();

            while (rs.next()) {
                for (final String component : hashComponents) {
                    result = 37 * result + rs.getString(component).hashCode();
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeResource(pst);
            closeResource(rs);
        }
        return Long.toHexString(result);
    }

    @Override
    public final TreeMap<String, MetaNode> getNodeMetaData(final String address,
            final boolean includeSubtree) throws NodeNotExistingException {
        PreparedStatement pst = null;
        ResultSet rs = null;
        StringBuilder sql;
        final TreeMap<String, MetaNode> results = new TreeMap<String, MetaNode>();
        if (address != null) {
            try {
                sql = new StringBuilder();
                sql.append(
                        "SELECT address, type, reader, writer, restriction, cacheParameters FROM ");
                sql.append(tableStructure).append(" WHERE address = ?");
                if (includeSubtree) {
                    sql.append(" OR address LIKE ?");
                }
                sql.append(" ORDER BY address asc");
                pst = con.prepareStatement(sql.toString());
                pst.setString(1, address);
                if (includeSubtree) {
                    if (address.equals("/")) {
                        pst.setString(2, address + "%");
                    } else {
                        pst.setString(2, address + "/%");

                    }
                }

                rs = pst.executeQuery();

                try {

                    while (rs.next()) {
                        results.put(rs.getString("address"), new MetaNode(
                                new ArrayList<String>(Arrays.asList(
                                        StringUtils.split(rs.getString("type"), LIST_SEPARATOR))),
                                new ArrayList<String>(Arrays.asList(
                                        StringUtils.split(rs.getString("reader"), LIST_SEPARATOR))),
                                new ArrayList<String>(Arrays.asList(
                                        StringUtils.split(rs.getString("writer"), LIST_SEPARATOR))),
                                rs.getString("restriction"), rs.getString("cacheParameters")));

                    }
                } catch (final SQLException e) {
                    // exception happens after we went through all entries, means no more
                    // childs, nothing to do
                    if (results.isEmpty()) {
                        throw new NodeNotExistingException("Node not found: " + address);
                    }
                }
                if (results.isEmpty()) {
                    throw new NodeNotExistingException("Node not found: " + address);
                }

                return results;

            } catch (final SQLException e) {
                LOGGER.debug("Error: {}", e.getMessage());

                throw new NodeNotExistingException(
                        "Sql Exception for node :" + address + ", " + e.getMessage());
            } finally {
                closeResource(pst);
                closeResource(rs);
            }

        } else {
            // if address was null
            throw new NodeNotExistingException("address was null");
        }
    }

    /**
     * General purpose getNodeRecord method that is used for the different kinds of public
     * getNodeRecordFunctions. They build a prepared Statement, this function executes it and
     * returns the result.
     *
     * @param pst
     *            The prepared Statement to execute.
     * @return The TreeMap with the resulting nodes.
     */
    private TreeMap<String, InternalNode> getNodeRecord(final PreparedStatement pst) {
        final TreeMap<String, InternalNode> results = new TreeMap<String, InternalNode>();
        ResultSet rs = null;
        try {

            rs = pst.executeQuery();
            while (rs.next()) {
                Date ts = null;
                if (hasColumn(rs, "timestamp")) {
                    ts = parseExtendedTimestamp(rs.getBigDecimal("timestamp"));
                }

                List<String> type = null;
                if (hasColumn(rs, "type")) {
                    type = new ArrayList<String>(
                            Arrays.asList(StringUtils.split(rs.getString("type"), LIST_SEPARATOR)));
                }
                List<String> readers = null;
                if (hasColumn(rs, "reader")) {
                    readers = new ArrayList<String>(Arrays
                            .asList(StringUtils.split(rs.getString("reader"), LIST_SEPARATOR)));
                }
                List<String> writers = null;
                if (hasColumn(rs, "writer")) {
                    writers = new ArrayList<String>(Arrays
                            .asList(StringUtils.split(rs.getString("writer"), LIST_SEPARATOR)));
                }
                String value = null;
                if (hasColumn(rs, "value")) {
                    value = rs.getString("value");
                }
                int version = -1;
                if (hasColumn(rs, "version")) {
                    version = rs.getInt("version");
                }
                String restrictions = null;
                if (hasColumn(rs, "restriction")) {
                    restrictions = rs.getString("restriction");
                }
                String cacheParameters = null;
                if (hasColumn(rs, "cacheParameters")) {
                    cacheParameters = rs.getString("cacheParameters");
                }
                results.put(rs.getString("address"), new InternalNode(type, value, readers, writers,
                        version, ts, restrictions, cacheParameters));
            }
        } catch (final SQLException e) {
            // exception happens after we went through all entries, means no more
            // childs, nothing to do
            LOGGER.debug(e.getMessage());
            return results;
        } finally {
            closeResource(pst);
            closeResource(rs);
        }
        return results;
    }

    /**
     * Helperclass to test if a resultset contains a columnname.
     *
     * @param rs
     *            The resultset.
     * @param columnName
     *            The columnName to check.
     * @return True if the columnName exists in the resultset, false otherwise.
     * @throws SQLException
     *             if something unexpected happens.
     */
    private boolean hasColumn(final ResultSet rs, final String columnName) throws SQLException {
        final ResultSetMetaData rsmd = rs.getMetaData();
        final int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.toLowerCase().equals(rsmd.getColumnName(x).toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final TreeMap<String, InternalNode> getNodeRecord(final String address,
            final VslAddressParameters params) throws NodeNotExistingException {
        PreparedStatement pst = null;

        if (address == null) {
            throw new NodeNotExistingException("address was null");
        }

        switch (params.getNodeInformationScope()) {
        case VALUE:
            pst = getPreparedStatementValue(address, params.getDepth());
            break;
        case METADATA:
            pst = getPreparedStatementComplete(address, params.getDepth(), false);
            break;
        default: // == case COMPLETE
            pst = getPreparedStatementComplete(address, params.getDepth(), true);
            break;
        }
        final TreeMap<String, InternalNode> results = getNodeRecord(pst);
        if (results.isEmpty()) {
            throw new NodeNotExistingException("Node not found: " + address);
        }
        closeResource(pst);
        return results;
    }

    /**
     * Internal method to create a prepared statement for retrieving a value from the database with
     * complete Information.
     *
     * @param address
     *            The address to retrieve
     * @param depth
     *            The depth of children that should be returned (e.g. 0 = only the node, 1 = include
     *            direct children, -1 = all children,...)
     * @param includeValue
     *            Boolean to indicate if the value should be included in the db query or if only
     *            metadata is requested.
     * @return A prepared Statement to be used with the database.
     * @throws NodeNotExistingException
     *             Thrown if the query can't be created or fails
     */
    private PreparedStatement getPreparedStatementComplete(final String address, final int depth,
            final boolean includeValue) throws NodeNotExistingException {
        PreparedStatement pst = null;
        StringBuilder sql;

        VslStatisticsDatapoint dp;

        if (includeValue) {
            dp = statisticsProvider.getStatistics(this.getClass(), "normalGetRequestComplete")
                    .begin();
        } else {
            dp = statisticsProvider.getStatistics(this.getClass(), "normalGetRequestMetaOnly")
                    .begin();
        }
        try {
            // create query
            final StringBuilder data = new StringBuilder();

            // select the newest value/timestamp
            if (includeValue) {
                data.append("SELECT * FROM ");
            } else {
                data.append("SELECT address, timestamp FROM ");
            }
            data.append(tableData).append(" WHERE (address,timestamp) IN ")
                    .append("(SELECT address, max(timestamp) FROM ").append(tableData)
                    .append(" GROUP by address)");

            // select the version of the newest value
            final StringBuilder version = new StringBuilder();
            version.append("SELECT address, max(version) as version").append(" FROM ")
                    .append(tableVersion).append(" GROUP BY address");

            sql = new StringBuilder();
            sql.append("SELECT ts.address, ts.type, ts.reader, ts.writer, tv.version,");
            sql.append(" ts.restriction, ts.cacheParameters, td.timestamp");
            if (includeValue) {
                sql.append(", td.value ");
            }
            sql.append(" FROM ").append(tableStructure);
            sql.append(" ts ").append("FULL JOIN (").append(data.toString()).append(")");
            sql.append(" td ON (td.address=ts.address)").append("FULL JOIN (")
                    .append(version.toString()).append(")");
            sql.append(" tv ON (tv.address=ts.address)");

            if (depth < 0) {
                sql.append(" WHERE ts.address=? OR ts.address LIKE ? ");
            } else if (depth == 0) {
                sql.append(" WHERE ts.address=? ");
            } else {
                sql.append(" WHERE ts.address=? ");
                sql.append(" OR REGEXP_MATCHES(ts.address, ?) ");
            }

            sql.append(" ORDER BY address asc");

            // prepare statement, add address
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            String wellformedAddress = address;
            if (!address.equals("/")) {
                wellformedAddress = address;
            } else {
                wellformedAddress = "";
            }
            if (depth < 0) {
                pst.setString(2, wellformedAddress + "/%");
            } else if (depth > 0) {
                // use a regex to match all child addresses of a specific depth.
                pst.setString(2, getRegexForTreeDepth(wellformedAddress, depth));
            }
            return pst;

        } catch (final SQLException e) {
            LOGGER.debug("Error: {}", e.getMessage());
            throw new NodeNotExistingException(
                    "SQL Exception for node " + address + ", " + e.getMessage());
        } finally {
            dp.end();
        }
    }

    /**
     * Helper function to create the regex for requests of a certain tree depth.
     *
     * @param address
     *            The address that is queried.
     * @param depth
     *            The depth of the requested tree.
     * @return The regex that can be used to filter whether a address is matched by the request
     *         depth.
     */
    private String getRegexForTreeDepth(final String address, final int depth) {
        String regex;
        if (depth < 0) {
            regex = "^" + address.replace("/", "\\/") + "(\\/[A-Za-z0-9]*)*$";
        } else if (depth == 0) {
            regex = "^" + address.replace("/", "\\/") + "\\/[A-Za-z0-9]*$";
        } else {
            regex = "^" + address.replace("/", "\\/") + "(\\/[A-Za-z0-9]*){0," + depth + "}$";
        }
        return regex;
    }

    /**
     * Internal method to create a prepared statement for retrieving a value from the database
     * without metainformations.
     *
     * @param address
     *            The address to retrieve
     * @param depth
     *            The depth of children that should be returned (e.g. 0 = only the node, 1 = include
     *            direct children, -1 = all children,...)
     * @return A prepared Statement to be used with the database.
     * @throws NodeNotExistingException
     *             Thrown if the query can't be created or fails
     */
    private PreparedStatement getPreparedStatementValue(final String address, final int depth)
            throws NodeNotExistingException {

        PreparedStatement pst = null;
        StringBuilder sql;

        final VslStatisticsDatapoint dp = statisticsProvider
                .getStatistics(this.getClass(), "normalGetRequestValueOnly").begin();
        try {
            // create query
            final StringBuilder data = new StringBuilder();

            // select the newest value/timestamp
            data.append("SELECT address, value FROM ");

            data.append(tableData).append(" WHERE (address,timestamp) IN ")
                    .append("(SELECT address, max(timestamp) FROM ").append(tableData)
                    .append(" GROUP by address)");

            sql = new StringBuilder();
            sql.append("SELECT ts.address, ts.reader, ts.writer, td.value");

            sql.append(" FROM ").append(tableStructure);
            sql.append(" ts ").append("FULL JOIN (").append(data.toString()).append(")");
            sql.append(" td ON (td.address=ts.address)");

            if (depth < 0) {
                sql.append(" WHERE ts.address=? OR ts.address LIKE ? ");
            } else if (depth == 0) {
                sql.append(" WHERE ts.address=? ");
            } else {
                sql.append(" WHERE ts.address=? ");
                sql.append(" OR REGEXP_MATCHES(ts.address, ?) ");
            }

            sql.append(" ORDER BY address asc");

            // prepare statement, add address
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            String wellformedAddress;
            if (!address.equals("/")) {
                wellformedAddress = address;
            } else {
                wellformedAddress = "";
            }
            if (depth < 0) {
                pst.setString(2, wellformedAddress + "/%");
            } else if (depth > 0) {
                // use a regex to match all child addresses of a specific depth.
                pst.setString(2, getRegexForTreeDepth(wellformedAddress, depth));
            }

            return pst;

        } catch (final SQLException e) {
            LOGGER.debug("Error: {}", e.getMessage());
            throw new NodeNotExistingException(
                    "SQL Exception for node " + address + ", " + e.getMessage());
        } finally {
            dp.end();
        }

    }

    // @Deprecated
    // @Override
    // public final TreeMap<String, InternalNode> getNodeRecord(final String address,
    // final boolean includeSubtree) throws NodeNotExistingException {
    // PreparedStatement pst = null;
    // StringBuilder sql;
    // TreeMap<String, InternalNode> results;
    //
    // if (address != null) {
    // final VslStatisticsDatapoint dp = statisticsProvider
    // .getStatistics(this.getClass(), "normalGetRequest").begin();
    // try {
    // // create query
    // final StringBuilder data = new StringBuilder();
    //
    // // select the newest value/timestamp
    // data.append("SELECT * FROM ").append(tableData)
    // .append(" WHERE (address,timestamp) IN ")
    // .append("(SELECT address, max(timestamp) FROM ").append(tableData)
    // .append(" GROUP by address)");
    //
    // // select the version of the newest value
    // final StringBuilder version = new StringBuilder();
    // version.append("SELECT address, max(version) as version").append(" FROM ")
    // .append(tableVersion).append(" GROUP BY address");
    //
    // sql = new StringBuilder();
    // sql.append("SELECT ts.address, ts.type, ts.reader, ts.writer, tv.version,");
    // sql.append(" ts.restriction, ts.cacheParameters, td.timestamp, td.value FROM ")
    // .append(tableStructure);
    // sql.append(" ts ").append("FULL JOIN (").append(data.toString()).append(")");
    // sql.append(" td ON (td.address=ts.address)").append("FULL JOIN (")
    // .append(version.toString()).append(")");
    // sql.append(" tv ON (tv.address=ts.address)");
    // sql.append(" WHERE ts.address = ?");
    //
    // if (includeSubtree) {
    // sql.append(" OR ts.address LIKE ?");
    // }
    // sql.append(" ORDER BY address asc");
    //
    // // prepare statement, add address
    // pst = con.prepareStatement(sql.toString());
    // pst.setString(1, address);
    // if (includeSubtree) {
    // pst.setString(2, address + "/%");
    // }
    //
    // results = getNodeRecord(pst);
    // if (results.isEmpty()) {
    // dp.end();
    // throw new NodeNotExistingException("Node not found: " + address);
    // }
    // return results;
    //
    // } catch (final SQLException e) {
    // LOGGER.debug("Error: {}", e.getMessage());
    // throw new NodeNotExistingException(
    // "SQL Exception for node " + address + ", " + e.getMessage());
    // } finally {
    // closeResource(pst);
    // dp.end();
    // }
    // } else {
    // // if address was null
    // throw new NodeNotExistingException("address was null");
    // }
    // }
    //
    // @Deprecated
    // @Override
    // public final TreeMap<String, InternalNode> getNodeRecord(final String address,
    // final boolean includeSubtree, final int treeVersion) throws NodeNotExistingException {
    // PreparedStatement pst = null;
    // StringBuilder sql;
    // TreeMap<String, InternalNode> results;
    //
    // if (address != null) {
    //
    // try {
    // // create query
    // final StringBuilder data = new StringBuilder();
    //
    // data.append("SELECT * FROM ").append(tableData)
    // .append(" WHERE (address,timestamp) IN ")
    // .append("(SELECT address, max(timestamp) FROM ").append(tableData)
    // .append(" WHERE timestamp <= ")
    // .append("(SELECT max(timestamp) as timestamp FROM ").append(tableVersion)
    // .append(" WHERE address = ? AND version = ?)").append(" GROUP by address)");
    //
    // final StringBuilder version = new StringBuilder();
    // version.append(
    // "SELECT address, max(timestamp) as timestamp, max(version) as version")
    // .append(" FROM ").append(tableVersion).append(" WHERE timestamp <= ")
    // .append("(SELECT max(timestamp) as timestamp FROM ").append(tableVersion)
    // .append(" WHERE address = ? AND version = ?)").append(" GROUP BY address");
    //
    // sql = new StringBuilder();
    // sql.append("SELECT ts.address, ts.type, ts.reader, ts.writer, tv.version,");
    // sql.append(" ts.restriction, ts.cacheParameters, td.timestamp, td.value FROM ")
    // .append(tableStructure);
    // sql.append(" ts FULL JOIN (").append(data.toString()).append(")");
    // sql.append(" td ON (td.address=ts.address) FULL JOIN (").append(version.toString())
    // .append(")");
    // sql.append(" tv ON (tv.address=ts.address) WHERE ( ts.address = ? ");
    // if (includeSubtree) {
    // sql.append(" OR ts.address LIKE ?");
    // }
    // sql.append(") AND tv.version IS NOT NULL ORDER BY address asc");
    // // IS NOT NULL makes sure we don't get results for not existing versions
    //
    // // prepare statement, add address
    // pst = con.prepareStatement(sql.toString());
    //
    // pst.setString(1, address);
    // pst.setInt(2, treeVersion);
    // pst.setString(3, address);
    // pst.setInt(4, treeVersion);
    // pst.setString(5, address);
    //
    // if (includeSubtree) {
    // pst.setString(6, address + "/%");
    // }
    //
    // results = getNodeRecord(pst);
    // if (results.isEmpty()) {
    // throw new NodeNotExistingException("Node or version not found: " + address);
    // }
    // return results;
    //
    // } catch (final SQLException e) {
    // LOGGER.debug("Error: {}", e.getMessage());
    // throw new NodeNotExistingException(
    // "SQl exception for node: " + address + ", " + e.getMessage());
    // } finally {
    // closeResource(pst);
    // }
    // } else {
    // // if address was null
    // throw new NodeNotExistingException("address was null");
    // }
    // }
    //
    // @Deprecated
    // @Override
    // public final TreeMap<String, InternalNode> getNodeRecord(final String address,
    // final boolean includeSubtree, final Date fromTime, final Date toTime)
    // throws NodeNotExistingException {
    // PreparedStatement pst = null;
    // StringBuilder sql;
    // TreeMap<String, InternalNode> results;
    //
    // final Timestamp from;
    // if (fromTime == null) {
    // from = null;
    // } else {
    // from = new Timestamp(fromTime.getTime());
    // }
    // final Timestamp to;
    // if (toTime == null) {
    // to = null;
    // } else {
    // to = new Timestamp(toTime.getTime());
    // }
    // BigDecimal fromD;
    // BigDecimal toD;
    // if (fromTime == null) {
    // fromD = BigDecimal.ZERO;
    // } else {
    // fromD = new BigDecimal(from.getTime());
    // }
    // if (toTime == null) {
    // toD = new BigDecimal(System.currentTimeMillis());
    // } else {
    // toD = new BigDecimal(to.getTime());
    // }
    // // fromD = fromD.multiply(maxExtendedTimeOffset);
    // // toD = toD.multiply(maxExtendedTimeOffset);
    // // .add(maxExtendedTimeOffset.subtract(new BigDecimal(1)));
    //
    // // LOGGER.debug("from {}, to {}", parseExtendedTimestamp(fromD),
    // // parseExtendedTimestamp(toD));
    // // LOGGER.debug("from {}, to {}", fromD, toD);
    //
    // if (address != null) {
    //
    // try {
    // // create query
    // final StringBuilder data = new StringBuilder();
    //
    // data.append("SELECT * FROM ").append(tableData)
    // .append(" WHERE (address,timestamp) IN ")
    // .append("(SELECT address, max(timestamp) FROM ").append(tableData)
    // .append(" WHERE timestamp <= ")
    // .append("(SELECT max(timestamp) as timestamp FROM ").append(tableVersion)
    // .append(" WHERE address = ? AND timestamp >= ? AND timestamp <= ?)")
    // .append(" GROUP by address)");
    //
    // final StringBuilder version = new StringBuilder();
    // version.append(
    // "SELECT address, max(timestamp) as timestamp, max(version) as version")
    // .append(" FROM ").append(tableVersion).append(" WHERE timestamp <= ")
    // .append("(SELECT max(timestamp) as timestamp FROM ").append(tableVersion)
    // .append(" WHERE address = ? AND timestamp >= ? AND timestamp <= ?)")
    // .append(" GROUP BY address");
    //
    // sql = new StringBuilder();
    // sql.append("SELECT ts.address, ts.type, ts.reader, ts.writer, tv.version,");
    // sql.append(" ts.restriction, ts.cacheParameters, td.timestamp, td.value FROM ")
    // .append(tableStructure);
    // sql.append(" ts ").append("FULL JOIN (").append(data.toString()).append(")");
    // sql.append(" td ON (td.address=ts.address)").append("FULL JOIN (")
    // .append(version.toString()).append(")");
    // sql.append(" tv ON (tv.address=ts.address)");
    // sql.append(" WHERE ( ts.address = ? ");
    //
    // if (includeSubtree) {
    // sql.append(" OR ts.address LIKE ?");
    // }
    // sql.append(") AND tv.version IS NOT NULL ORDER BY address asc");
    // // IS NOT NULL makes sure we don't get results for not existing versions
    //
    // // prepare statement, add address
    // pst = con.prepareStatement(sql.toString());
    //
    // pst.setString(1, address);
    // pst.setBigDecimal(2, fromD);
    // pst.setBigDecimal(3, toD);
    //
    // pst.setString(4, address);
    // pst.setBigDecimal(5, fromD);
    // pst.setBigDecimal(6, toD);
    //
    // pst.setString(7, address);
    //
    // if (includeSubtree) {
    // pst.setString(8, address + "/%");
    // }
    // results = getNodeRecord(pst);
    // if (results.isEmpty()) {
    // throw new NodeNotExistingException("No Node found in given Range: " + address
    // + " from " + from + " to " + to);
    // }
    // return results;
    //
    // } catch (final SQLException e) {
    // LOGGER.debug("Error: {}", e.getMessage());
    // throw new NodeNotExistingException(
    // "SQL Exception for node: " + address + ", " + e.getMessage());
    // } finally {
    // closeResource(pst);
    // }
    // } else {
    // // if address was null
    // throw new NodeNotExistingException("address was null");
    // }
    // }

    // @Override
    // public final int getNumberOfNodes() {
    // int result = -1;
    // PreparedStatement pst = null;
    // ResultSet rs = null;
    //
    // try {
    // final StringBuilder sql = new StringBuilder();
    // sql.append("SELECT COUNT(*) as total FROM ").append(tableStructure);
    // pst = con.prepareStatement(sql.toString());
    // rs = pst.executeQuery();
    //
    // if (rs.next()) {
    // result = rs.getInt("total");
    // } else {
    // LOGGER.warn("Could not determine number of nodes.");
    // }
    // } catch (final SQLException e) {
    // throw new RuntimeException(e);
    // } finally {
    // closeResource(pst);
    // closeResource(rs);
    // }
    // return result;
    // }

    /**
     * Initializes the knowledge Table: create Tables if they don't exist.
     *
     */
    private void initKnowledgeTables() {
        StringBuilder sql = new StringBuilder();

        // wipe and (re-)create database tables.
        if (!configService.isDatabasePersistent()) {
            wipe();
        }
        final String memoryMode = configService.getDatabaseMemoryMode();
        // tables are only added when they don't already exist, checked at addTable()
        sql = new StringBuilder();
        sql.append("CREATE ").append(memoryMode).append(" TABLE ").append(tableStructure)
                .append(" (");
        sql.append("address LONGVARCHAR,");
        sql.append("type LONGVARCHAR,");
        sql.append("reader LONGVARCHAR,");
        sql.append("writer LONGVARCHAR,");
        sql.append("restriction LONGVARCHAR,");
        sql.append("cacheParameters LONGVARCHAR,");
        sql.append("PRIMARY KEY (address))");

        addTable(tableStructure, sql.toString());

        sql = new StringBuilder();
        sql.append("CREATE ").append(memoryMode).append(" TABLE ").append(tableVersion)
                .append(" (");
        sql.append("address LONGVARCHAR,");
        sql.append("version BIGINT,");
        sql.append("timestamp DECIMAL(64,5))");
        // sql.append("PRIMARY KEY (address, version))");

        addTable(tableVersion, sql.toString());

        // create the table that will hold the actual values stored at certain nodes.
        sql = new StringBuilder();
        sql.append("CREATE ").append(memoryMode).append(" TABLE ").append(tableData).append(" (");
        sql.append("address LONGVARCHAR,");
        sql.append("timestamp DECIMAL(64,5),");
        sql.append("value VARCHAR(" + configService.getDatabaseMaxValueLength() + "))");
        // sql.append("FOREIGN KEY (address) REFERENCES ");
        // sql.append(tableStructure).append("(address))");

        addTable(tableData, sql.toString());

        // add rootnode if he doesn't exist already
        // try {
        // getNodeMetaData("/", false);
        // } catch (final NodeNotExistingException e) {
        // PreparedStatement pst = null;
        // try {
        // sql = new StringBuilder();
        // sql.append("INSERT INTO ").append(tableStructure);
        // sql.append(
        // " (address, type, reader, writer, restriction, cacheParameters) VALUES ");
        // sql.append("(?,?,?,?,?,?)");
        // pst = con.prepareStatement(sql.toString());
        // pst.setString(1, "/");
        // pst.setString(2, VslNodeTree.TYPE_TREE_ROOT);
        // pst.setString(3, VslNodeTree.SYSTEM_USER_ID);
        // pst.setString(4, VslNodeTree.SYSTEM_USER_ID);
        // pst.setString(5, "regularExpression=''");
        // pst.setString(6, "");
        // pst.execute();
        // con.commit();
        // } catch (final SQLException e1) {
        // LOGGER.error("Error adding the rootNode: {}", e1);
        // } finally {
        // closeResource(pst);
        // }
        // }
    }

    /**
     * helper function to check the size of different database tables.
     *
     * @param table
     *            The table to check
     */
    @SuppressWarnings("unused")
    private void logTableSize(final String table) {
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            final StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) as total FROM ").append(table);
            pst = con.prepareStatement(sql.toString());
            rs = pst.executeQuery();

            if (rs.next()) {
                LOGGER.debug("size of table {} : {}", table, rs.getString("total"));
            } else {
                LOGGER.warn("Could not determine number of nodes.");
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeResource(pst);
            closeResource(rs);
        }
    }

    @Override
    public final boolean nodeExists(final String address) {
        boolean result = true;
        try {
            getNodeMetaData(address, false);
            // getNodeRecord(address, false).get(address);
        } catch (final NodeNotExistingException e) {
            result = false;
        }
        return result;
    }

    /**
     * Parses an extendedTimeStamp (BigDecimal) and returns a normal TimeStamp object.
     *
     * @param extendedTimestamp
     *            The extendedTimeStamp to parse.
     * @return New TimeStamp object based on extendedTimeStamp
     */
    private Timestamp parseExtendedTimestamp(final BigDecimal extendedTimestamp) {
        if (extendedTimestamp == null) {
            return null;
        }
        return new Timestamp(extendedTimestamp.longValue());
    }

    // /**
    // * Removes all but the newest version of a specific node.
    // *
    // * @param address
    // * address of the node.
    // */
    // private void removeArchivedVersionsOf(final String address) {
    // // TODO: test!
    // // LOGGER.debug("removeArchivedVersionsOf called");
    // PreparedStatement pst = null;
    // StringBuilder sql;
    // try {
    // startTransaction();
    // sql = new StringBuilder();
    // sql.append("DELETE FROM ").append(tableData).append(" t1 WHERE address = ? AND ");
    // sql.append("timestamp <> (SELECT max(timestamp) from ").append(tableData);
    // sql.append(" t2 WHERE t2.address = t1.address)");
    // pst = con.prepareStatement(sql.toString());
    // pst.setString(1, address);
    // pst.execute();
    // closeResource(pst);
    //
    // sql = new StringBuilder();
    // sql.append("DELETE FROM ").append(tableVersion).append(" t1 WHERE address = ? AND ");
    // sql.append("version <> (SELECT max(version) from ").append(tableVersion);
    // sql.append(" t2 WHERE t2.address = t1.address)");
    // pst = con.prepareStatement(sql.toString());
    // pst.setString(1, address);
    // pst.execute();
    // } catch (final SQLException e) {
    // LOGGER.error("Error removing archived Nodes: {}", e.getMessage());
    // } finally {
    // commitTransaction();
    // closeResource(pst);
    // }
    // }

    @Override
    public final void removeNode(final String address) {
        PreparedStatement pst = null;

        try {
            startTransaction();
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableData).append(" ");
            sql.append("WHERE address=? ");
            sql.append("OR address LIKE ?");
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            pst.setString(2, address + "/%");
            pst.executeUpdate();
            closeResource(pst);

            sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableVersion).append(" ");
            sql.append("WHERE address=? ");
            sql.append("OR address LIKE ?");
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            pst.setString(2, address + "/%");
            pst.executeUpdate();
            closeResource(pst);

            sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableStructure).append(" ");
            sql.append("WHERE address=? ");
            sql.append("OR address LIKE ?");
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            pst.setString(2, address + "/%");
            pst.executeUpdate();

        } catch (final SQLException e) {
            LOGGER.error("Error Deleting a node: {}", e.getMessage());
        } finally {
            commitTransaction();
            closeResource(pst);
        }

    }

    // @Override
    // public final void setValue(final String address, final String value)
    // throws NodeNotExistingException {
    // final Map<String, String> map = new HashMap<String, String>();
    // map.put(address, value);
    // setValueTree(map);
    // }

    @Override
    public final void setValueTree(final Map<String, String> values)
            throws NodeNotExistingException {

        if (values.size() == 0) {
            return;
        }
        final VslStatisticsDatapoint dp = statisticsProvider
                .getStatistics(this.getClass(), "Set" + Integer.toString(values.size()) + "Nodes")
                .begin();
        PreparedStatement pst = null;

        final BigDecimal timeStamp = getExtendedTimestamp();
        // LOGGER.debug("{},{}", address, parseExtendedTimestamp(timeStamp));
        // LOGGER.debug("{},{}", address, timeStamp);

        final StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableData);
        sql.append(" (address, value, timestamp) VALUES ");
        sql.append("(?,?,?)");

        try {
            startTransaction();
            pst = con.prepareStatement(sql.toString());

            for (final Entry<String, String> entry : values.entrySet()) {
                pst.setString(1, entry.getKey());
                pst.setString(2, entry.getValue());
                pst.setBigDecimal(3, timeStamp);
                pst.addBatch();
            }
            pst.executeBatch();
            // update version number and timestamp of this node and his parents (up to service
            // level)
            updateVersion(values.keySet(), timeStamp);
            commitTransaction();
            // updateArchiveSize(values.keySet());

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            commitTransaction();
            closeResource(pst);
            lastTimestampSet = timeStamp;
            dp.end();
        }
    }

    // /**
    // * Makes sure the archive doesn't hold to many entries for a node after adding a new one.
    // Oldest
    // * ones are deleted until the desired amount of maximum archived versions is archived.
    // *
    // * @param changedNodes
    // * The nodes that were changed and should be checked.
    // */
    // private void updateArchiveSize(final Collection<String> changedNodes) {
    // // remove older versions if archive is disabled.
    // if (!configService.isArchiveEnabled()) {
    // for (final String address : changedNodes) {
    // removeArchivedVersionsOf(address);
    // }
    // } else {
    // // else check that we don't store more historical data than configured (delete older
    // // versions)
    // for (final String address : changedNodes) {
    // removeOvermuchArchivedVersionsOf(address);
    // }
    // }
    // }

    /**
     * Adds new entries to the version table of the database with the new versionnumbers of the
     * changed nodes and all their parents. If a node is a parent to more then one changed node,
     * it's versionnumber will still only be incremented by 1. Timestamp of the new entries is set
     * to the given Timestamp.
     *
     * @param changedAddresses
     *            Addresses of all nodes that changed.
     * @param timeStamp
     *            Timestamp to set.
     */
    private void updateVersion(final Collection<String> changedAddresses,
            final BigDecimal timeStamp) {
        final List<String> affectedNodes = new LinkedList<String>();

        // gather all affected nodes. Every node will be only counted once, even if he is the parent
        // of more then one changed node.
        for (final String address : changedAddresses) {
            if (!affectedNodes.contains(address)) {
                affectedNodes.add(address);
            }
            for (final String parent : AddressParser.getAllParentsOfAddress(address, 2)) {
                if (!affectedNodes.contains(parent)) {
                    affectedNodes.add(parent);
                }
            }
        }

        PreparedStatement pst = null;
        final StringBuilder sql = new StringBuilder();
        // BEWARE: "FROM ").append(tableStructure);" is only used since a non empty table must be
        // specified in order for generic selects to work (can't use SELECT without FROM in hsqldb).
        // Since the structure Table is never empty, this is used here.
        // however, it is not really involved in the query.
        sql.append("INSERT INTO ").append(tableVersion).append(" (address, version, timestamp) ")
                .append(" SELECT ?, (SELECT IFNULL(max(version)+1,0) FROM ").append(tableVersion)
                .append(" WHERE address=?), ? FROM ").append(tableStructure)
                .append(" WHERE address=?");

        try {
            pst = con.prepareStatement(sql.toString());
            for (final String address : affectedNodes) {
                pst.setString(1, address);
                pst.setString(2, address);
                pst.setBigDecimal(3, timeStamp);
                pst.setString(4, address);
                pst.addBatch();
            }
            pst.executeBatch();

        } catch (final SQLException e) {
            LOGGER.error("Error updating the Versiontable: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            closeResource(pst);
        }
    }

    // /**
    // * Helper function to remove old nodes if the number of archived versions for a node would
    // grow
    // * to big.
    // *
    // * @param address
    // * address of the affected node.
    // */
    // private void removeOvermuchArchivedVersionsOf(final String address) {
    // PreparedStatement pstSELECT = null;
    // PreparedStatement pstDELETEdata = null;
    // PreparedStatement pstDELETEversion = null;
    //
    // ResultSet rs = null;
    //
    // final List<String> parentNodes = KORUtility.getAllParentsOfAddress(address, 2);
    // try {
    // StringBuilder sql = new StringBuilder();
    // sql.append("SELECT address, timestamp, value FROM ").append(tableData)
    // .append(" WHERE address = ? AND timestamp NOT IN ")
    // .append("(SELECT TOP ? timestamp FROM ").append(tableData)
    // .append(" WHERE address = ? ORDER BY timestamp desc)");
    // pstSELECT = con.prepareStatement(sql.toString());
    // pstSELECT.setString(1, address);
    // pstSELECT.setInt(2, configService.getArchiveNodeVersionLimit());
    // pstSELECT.setString(3, address);
    // rs = pstSELECT.executeQuery();
    //
    // sql = new StringBuilder();
    // sql.append("DELETE FROM ").append(tableData)
    // .append(" WHERE address = ? AND timestamp = ?");
    // pstDELETEdata = con.prepareStatement(sql.toString());
    //
    // sql = new StringBuilder();
    // sql.append("DELETE FROM ").append(tableVersion)
    // .append(" WHERE timestamp = ? AND (address = ? ");
    // for (int i = 0; i < parentNodes.size(); i++) {
    // sql.append(" OR address = ? ");
    // }
    // sql.append(")");
    // pstDELETEversion = con.prepareStatement(sql.toString());
    //
    // // in case there are more then one nodes to drop, drop them all (e.g. because the
    // // archive size was lowered.
    // boolean rsEmpty = true;
    // while (rs.next()) {
    // rsEmpty = false;
    // // LOGGER.debug("{}: {} set at {}", rs.getString("address"), rs.getString("value"),
    // // rs.getString("timestamp"));
    //
    // pstDELETEdata.setString(1, rs.getString("address"));
    // pstDELETEdata.setBigDecimal(2, rs.getBigDecimal("timestamp"));
    // pstDELETEdata.addBatch();
    //
    // pstDELETEversion.setBigDecimal(1, rs.getBigDecimal("timestamp"));
    // pstDELETEversion.setString(2, rs.getString("address"));
    // int index = 3;
    // for (final String parent : parentNodes) {
    // pstDELETEversion.setString(index++, parent);
    // }
    // pstDELETEversion.addBatch();
    // }
    // if (!rsEmpty) {
    // pstDELETEdata.executeBatch();
    // pstDELETEversion.executeBatch();
    // }
    //
    // } catch (final SQLException e) {
    // throw new RuntimeException(e);
    // } finally {
    // closeResource(pstSELECT);
    // closeResource(pstDELETEdata);
    // closeResource(pstDELETEversion);
    // closeResource(rs);
    // }
    //
    // }

    /**
     * Drops all the tables from the database.
     */
    private void wipe() {
        final StringBuilder sqlStructure = new StringBuilder();
        final StringBuilder sqlVersion = new StringBuilder();
        final StringBuilder sqlData = new StringBuilder();

        sqlStructure.append("DROP TABLE IF EXISTS ").append(tableStructure);
        sqlVersion.append("DROP TABLE IF EXISTS ").append(tableVersion);
        sqlData.append("DROP TABLE IF EXISTS ").append(tableData);

        Statement st = null;
        try {
            startTransaction();
            st = con.createStatement();
            st.execute(sqlData.toString());
            st.execute(sqlVersion.toString());
            st.execute(sqlStructure.toString());
            commitTransaction();
        } catch (final SQLException e) {
            LOGGER.error("Error wiping the Database: {}", e.getMessage());
        } finally {
            closeResource(st);
        }
    }

    @Override
    public final void shutdown() {
        maintentanceTimer.cancel();
        Statement pst = null;
        try {
            pst = con.createStatement();
            pst.execute("SHUTDOWN COMPACT");
            con.commit();
            pst.close();
            con.close();
        } catch (final SQLException e) {
            closeResource(pst);
            LOGGER.debug(e.getMessage());
        }
    }

    /**
     * Disables logging.
     */
    private void setDatabaseFlags() {
        Statement st = null;
        try {
            st = con.createStatement();
            // if the size of the LOG file gets >10 MB, execute a checkpoint and delete the log
            // file.
            st.execute("SET FILES LOG SIZE 10");
            con.commit();
            closeResource(st);

            st = con.createStatement();
            // if the amount of empty space in the .data file is >20% of total filesize when a
            // checkpoint happens, defrag and shrink it.
            st.execute("SET FILES DEFRAG 20");
            con.commit();
        } catch (final SQLException e) {
            LOGGER.debug(e.getMessage());
        } finally {
            closeResource(st);
        }
    }

    /**
     * Starts a transaction by disabling autocommit.
     */
    private void startTransaction() {
        try {
            con.setAutoCommit(false);
        } catch (final SQLException e) {
            LOGGER.error("An error occured on starting an transaction", e);
        }
    }

    /**
     * Commits the current transaction and sets back to autocommit.
     */
    private void commitTransaction() {
        try {
            con.commit();
            con.setAutoCommit(true);
        } catch (final SQLException e) {
            LOGGER.error("An error occured on commiting an transaction, rollback initiated", e);
            rollbackTransaction();
        }
    }

    /**
     * Rollback to the last commit.
     */
    private void rollbackTransaction() {
        try {
            con.rollback();
        } catch (final SQLException e) {
            LOGGER.error("An error occured on transaction rollback", e);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (final SQLException e) {
                LOGGER.error("An error occured on transaction rollback", e);
            }
        }
    }

    @Override
    public final void cacheVslNode(final String address, final VslNode node) {
        PreparedStatement pstData = null;
        PreparedStatement pstVersion = null;

        final StringBuilder sqlData = new StringBuilder();
        sqlData.append(" MERGE INTO ").append(tableData).append(" dt USING (VALUES(?,?,?))")
                .append(" AS vals(address, value, timestamp)").append(" ON dt.address=vals.address")
                .append(" WHEN MATCHED THEN UPDATE SET ")
                .append(" dt.value=vals.value, dt.timestamp=vals.timestamp")
                .append(" WHEN NOT MATCHED THEN INSERT (address, value, timestamp) VALUES ")
                .append(" vals.address, vals.value, vals.timestamp");

        final StringBuilder sqlVersion = new StringBuilder();
        sqlVersion.append(" MERGE INTO ").append(tableVersion).append(" vt USING (VALUES(?,?,?))")
                .append(" AS vals(address, version, timestamp)")
                .append(" ON (vt.address=vals.address)").append(" WHEN MATCHED THEN UPDATE SET")
                .append(" vt.version=vals.version, vt.timestamp=vals.timestamp")
                .append(" WHEN NOT MATCHED THEN INSERT (address, version, timestamp) VALUES")
                .append(" vals.address, vals.version, vals.timestamp");

        try {
            pstData = con.prepareStatement(sqlData.toString());
            pstVersion = con.prepareStatement(sqlVersion.toString());

            // rootNode
            if (!(node.getValue() == null || node.getVersion() == -1 || node.getTimestamp() == null
                    || node.getTypes().isEmpty())) {
                final BigDecimal time = new BigDecimal(node.getTimestamp().getTime());
                pstData.setString(1, address);
                pstData.setString(2, node.getValue());
                pstData.setBigDecimal(3, time);
                pstData.addBatch();

                pstVersion.setString(1, address);
                pstVersion.setLong(2, node.getVersion());
                pstVersion.setBigDecimal(3, time);
                pstVersion.addBatch();
            }
            // children
            for (final Entry<String, VslNode> entry : node.getAllChildren()) {
                // omit empty nodes
                if (entry.getValue().getValue() == null || entry.getValue().getVersion() == -1
                        || entry.getValue().getTimestamp() == null
                        || entry.getValue().getTypes().isEmpty()) {
                    continue;
                }
                final BigDecimal time = new BigDecimal(entry.getValue().getTimestamp().getTime());
                pstData.setString(1, address + "/" + entry.getKey());
                pstData.setString(2, entry.getValue().getValue());

                pstData.setBigDecimal(3, time);
                pstData.addBatch();

                pstVersion.setString(1, address + "/" + entry.getKey());
                pstVersion.setLong(2, entry.getValue().getVersion());
                pstVersion.setBigDecimal(3, time);
                pstVersion.addBatch();
            }
            startTransaction();
            pstData.executeBatch();
            pstVersion.executeBatch();
            commitTransaction();

        } catch (final SQLException e) {
            LOGGER.debug(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            commitTransaction();
            closeResource(pstData);
            closeResource(pstVersion);

        }

    }

    @Override
    public final void removeCachedNode(final String address) {
        PreparedStatement pst = null;
        try {
            startTransaction();
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableData).append(" ");
            sql.append("WHERE address=?");
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            pst.executeUpdate();
            closeResource(pst);

            sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableVersion).append(" ");
            sql.append("WHERE address=?");
            pst = con.prepareStatement(sql.toString());
            pst.setString(1, address);
            pst.executeUpdate();
            closeResource(pst);

        } catch (final SQLException e) {
            LOGGER.error("Error Deleting a cached node: {}", e.getMessage());
        } finally {
            commitTransaction();
            closeResource(pst);
        }
    }

}
