package org.ds2os.vsl.core.config;

/**
 * Configuration values belonging to the KOR-sync service.
 *
 * @author liebald
 */
public interface VslKORSyncConfig extends VslAgentName {

    /**
     * Returns the maximum amount of time in milliseconds the kor-Sync will cache
     * {@link org.ds2os.vsl.core.VslKORUpdate}s which couldn't be applied instantly when receiving
     * the {@link org.ds2os.vsl.core.VslKORUpdate}. Default value if not changed in config or KOR is
     * 60000 (1 minute).
     *
     * @return maximum cache time of KOR updates in milliseconds
     */
    @ConfigDescription(description = "Time in milliseconds that the korSync module will cache "
            + "korSync packets from other KAs he couldn't apply directly on receiving."
            + "", id = "korSync.updateCacheTimeout"
                    + "", defaultValue = "60000", restrictions = ">0")
    long getMaxKORUpdateCacheTime();

}
