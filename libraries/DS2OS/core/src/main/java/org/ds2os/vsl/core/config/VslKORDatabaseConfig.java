package org.ds2os.vsl.core.config;

/**
 * This interface contains all configuration methods for the Database.
 *
 * @author liebald
 */
public interface VslKORDatabaseConfig {

    /**
     * Returns whether the Database should be persistent or not.
     *
     * @return True if the database should be persistent, false if not.
     */
    @ConfigDescription(description = "Flag that describes whether the underlying database is "
            + "persistent (1) or reset every restart (0).", id = "kor.db.persist", defaultValue = ""
                    + "0", restrictions = "0 or 1 (boolean), only used at startup")
    Boolean isDatabasePersistent();

    /**
     * Returns if the Archive function is enabled. This value is read from the system once and then
     * cached internally by the config service for faster access. The default value if nothing is
     * configured is false.
     *
     * @return True if Archive is enabled, false if only the newest values should be stored.
     */
    @ConfigDescription(description = "Flag that describes whether the versioning/archive "
            + "functionality is enabled (1) or not (0).", id = "kor.archive.enabled"
                    + "", defaultValue = "0", restrictions = "0 or 1 (boolean)")
    boolean isArchiveEnabled();

    /**
     * Returns the maximum amount of Versions of a node that should be stored at the same time. This
     * value is read from the system once and then cached internally by the config service for
     * faster access. The default value is 10 if not specified otherwise.
     *
     * @return maximum number of versions of one node that can be stored simultaniusly.
     */
    @ConfigDescription(description = "Specifies how many versions of each nodes are stored "
            + "at most. If the limit is reached the older versions may be deleted."
            + "", id = "kor.archive.limit", defaultValue = "10", restrictions = ">0")
    int getArchiveNodeVersionLimit();

    /**
     * Returns the path of the database.
     *
     * @return Database path as String.
     */
    @ConfigDescription(description = "Location of the database on the file system."
            + "", id = "kor.db.location", defaultValue = "hsqldb/db", restrictions = ""
                    + "relative or absolut path")
    String getDatabasePath();

    /**
     * Returns the username of the database.
     *
     * @return Database username as String.
     */
    @ConfigDescription(description = "Username to access the database."
            + "", id = "kor.db.username", defaultValue = "admin"
                    + "", restrictions = "only used at startup")
    String getDatabaseUsername();

    /**
     * Returns the password of the database.
     *
     * @return Database password as String.
     */
    @ConfigDescription(description = "Password to access the database."
            + "", id = "kor.db.password", defaultValue = "password"
                    + "", restrictions = "only used at startup")
    String getDatabasePassword();

    /**
     * Returns the maximum length of values that can be stored in the database in bytes. Values can
     * be either numeric (e.g. 16000000) or mixed, (e.g. 16M where M = 1024*1024). Default value is
     * 16M.
     *
     * @return Maximum length of the length of stored VslNode values.
     */
    @ConfigDescription(description = "The maximum size of values that can be stored in a single"
            + " node by the database in bytes.", id = "kor.db.maxValueLength", defaultValue = ""
                    + "16M", restrictions = "can use K/M/G as abbreviations (kilo, mega, giga, "
                            + "only used at startup")
    String getDatabaseMaxValueLength();

    /**
     * Returns the mode the database tables should be handled. Can be either memory or cached.
     * Changes only work when the database is not persistent (on creation).
     *
     * @return The desired database mode.
     */
    @ConfigDescription(description = "Defines if the datbase runs in memory (MEMORY) or "
            + "in cached (CACHED) mode.", id = "kor.db.memoryMode", defaultValue = "CACHED"
                    + "", restrictions = "only used at startup on non persistent databases")
    String getDatabaseMemoryMode();

    /**
     * Returns the type of the database that should be used as backend.
     * Examples: hsqldb, mongodb
     * Default is hsqldb
     *
     * @return The desired database type as String.
     */
    @ConfigDescription(description = "Defines the database type to be used as backend", id = "kor.db.type", defaultValue = "hsqldb"
            + "", restrictions = "")
    String getDatabaseType();
}
