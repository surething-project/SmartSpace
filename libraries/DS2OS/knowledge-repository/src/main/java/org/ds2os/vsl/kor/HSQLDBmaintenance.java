package org.ds2os.vsl.kor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimerTask;

import org.ds2os.vsl.core.config.VslKORDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for regular cleanup operations on the database.
 *
 * @author liebald
 */
public class HSQLDBmaintenance extends TimerTask {

    /**
     * The {@link Connection} to the database.
     */
    private final Connection con;

    /**
     * The name of the table containing version information in the database.
     */
    private final String tableVersion;

    /**
     * The name of the table containing data/value information in the database.
     */
    private final String tableData;

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HSQLDBmaintenance.class);

    /**
     * Constructor, taking a {@link Connection} to the database this class should maintain.
     *
     * @param con
     *            The {@link Connection} to the database.
     * @param configService
     *            The {@link VslKORDatabaseConfig} for the database.
     * @param tableVersion
     *            The name of the table containing version information in the database.
     * @param tableData
     *            The name of the table containing data/value information in the database.
     */
    public HSQLDBmaintenance(final Connection con, final VslKORDatabaseConfig configService,
            final String tableData, final String tableVersion) {
        this.con = con;
        this.tableData = tableData;
        this.tableVersion = tableVersion;
    }

    @Override
    public final void run() {
        try {
            con.setAutoCommit(false);
            maintainTables();
            con.commit();
            con.setAutoCommit(true);
        } catch (final SQLException e) {
            LOGGER.error("Error on running database maintainance", e);
        }
    }

    /**
     * Method to maintain all relevant tables of the database (delete entries that are to old,
     * etc.).
     */
    private void maintainTables() {

        PreparedStatement pst = null;
        StringBuilder sql;
        try {
            sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableData)
                    .append(" WHERE (address, timestamp) NOT IN ")
                    .append("(SELECT address, max(timestamp) FROM ").append(tableData)
                    .append(" group by address)");

            pst = con.prepareStatement(sql.toString());
            pst.execute();
            closeResource(pst);
            sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableVersion).append(" WHERE timestamp not in ")
                    .append("(SELECT timestamp FROM ").append(tableData).append(")");
            pst = con.prepareStatement(sql.toString());
            pst.execute();
        } catch (final SQLException e) {
            LOGGER.error("Error removing archived Nodes: {}", e.getMessage());
        } finally {
            closeResource(pst);
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
                LOGGER.warn("Couldn't clean up a prepared statement: {}", e.getMessage());
            }
        }
    }

}
