package org.ds2os.vsl.statistics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ds2os.vsl.core.config.VslStatisticsConfig;
import org.ds2os.vsl.core.statistics.VslStatistics;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.statistics.service.StatisticService;

/**
 * Implementation of {@link VslStatisticsProvider}.
 *
 * @author liebald
 */
public class StatisticsProvider implements VslStatisticsProvider {

    /**
     * Keeps track of all {@link VslStatistics} on objects created.
     */
    private final Map<String, Statistics> currentStatistics;

    /**
     * Constructor.
     */
    public StatisticsProvider() {
        currentStatistics = new HashMap<String, Statistics>();
    }

    /**
     * The configuration for the Statistics service and related classes. Dummy until changed to the
     * real config by the {@link StatisticService}.
     */
    private VslStatisticsConfig config = new VslStatisticsConfig() {
        @Override
        public int getStatisticsLimitDatapoints() {
            return 1000;
        }
    };

    @Override
    public final VslStatistics getStatistics(final Class<?> c, final String purpose) {
        final String statisticsName = c.getSimpleName() + "/" + purpose;
        synchronized (currentStatistics) {
            if (!currentStatistics.containsKey(statisticsName)) {
                currentStatistics.put(statisticsName, new Statistics(config));
            }
            return currentStatistics.get(statisticsName);
        }
    }

    /**
     * Returns the {@link Statistics} Object related to the given statisticsName.
     *
     * @param statisticsName
     *            Name of the desired {@link Statistics}
     * @return The {@link Statistics} or null if none belongs the given statisticsName.
     */
    public final Statistics getStatistics(final String statisticsName) {
        synchronized (currentStatistics) {
            return currentStatistics.get(statisticsName);
        }
    }

    /**
     * Returns the identifiers all currently available {@link Statistics}.
     *
     * @return all currently available {@link Statistics} identifiers.
     */
    public final Collection<String> getAvailableStatistics() {
        synchronized (currentStatistics) {
            if (currentStatistics.isEmpty()) {
                return Collections.emptySet();
            } else {
                return currentStatistics.keySet();
            }
        }
    }

    /**
     * This method can be used to hand over a {@link VslStatisticsConfig} that should be used by the
     * {@link StatisticsProvider} for configuration.
     *
     * @param newConfig
     *            The new {@link VslStatisticsConfig}.
     */
    public final void setConfig(final VslStatisticsConfig newConfig) {
        this.config = newConfig;
        synchronized (currentStatistics) {
            for (final Entry<String, Statistics> entry : currentStatistics.entrySet()) {
                entry.getValue().setConfig(newConfig);
            }
        }
    }
}
