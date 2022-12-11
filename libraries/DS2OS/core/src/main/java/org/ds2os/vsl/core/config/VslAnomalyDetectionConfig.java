package org.ds2os.vsl.core.config;

/**
 * Interface for Anomaly detection related configurations.
 *
 * @author liebald
 * @author aubet
 */
public interface VslAnomalyDetectionConfig {

    /**
     * Returns whether the Anomaly detection in KA communication is currently enabled or not.
     *
     * @return true if Anomaly detection is enabled, false if not.
     */
    @ConfigDescription(description = "Flag that specifies whether the anomaly "
            + "detection in KA communication is enabled (1) or not (0).", id = "security.sphinx.enabled", defaultValue = "0", restrictions = "0 or 1 (boolean)")
    boolean isAnomalyDetectionEnabled();

    /**
     * Returns whether the KA should block detections that are detected as anomalous by default.
     *
     * @return true if anomalous connections should be blocked, false if not.
     */
    @ConfigDescription(description = "Flag that specifies whether anomalous connections "
            + "should be blocked by the KA (1) or not (0).", id = "security.sphinx.blockOnAnomaly", defaultValue = "0", restrictions = "0 or 1 (boolean)")
    boolean isBlockOnAnomaly();

}
