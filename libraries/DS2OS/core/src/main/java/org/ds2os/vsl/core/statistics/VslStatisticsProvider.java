package org.ds2os.vsl.core.statistics;

/**
 * Interface for the Statistics provider classes. Allowes modules to request Statistics classes
 * which can be used to generate and store internal statistics.
 *
 * @author liebald
 */
public interface VslStatisticsProvider {

    /**
     * Creates new {@link VslStatistics} assigned to the given purpose which can be used by modules
     * to track their statistics. The {@link VslStatistics} is stored by the
     * {@link VslStatisticsProvider} and can be requested again for future use.
     *
     * @param purpose
     *            The purpose of the requested {@link VslStatistics}.
     * @param c
     *            The class of the object requesting the {@link VslStatistics} (to ensure different
     *            classes don't interfere).
     * @return The {@link VslStatistics} for the given purpose.
     */
    VslStatistics getStatistics(Class<?> c, String purpose);

}
