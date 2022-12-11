package org.ds2os.vsl.statistics;

import org.ds2os.vsl.core.statistics.VslStatisticsDatapoint;

/**
 * Implementation of {@link VslStatisticsDatapoint}.
 *
 * @author liebald
 */
public class StatisticsDatapoint implements VslStatisticsDatapoint {

    /**
     * The time of the creation of this {@link StatisticsDatapoint}. UNIX time in nanoseconds.
     */
    private final long startTime;

    /**
     * System.nanoTime() of the creation of this {@link StatisticsDatapoint}. Only for comparison
     * with other nanoTimes, unit is relative to some unknown origin.
     */
    private final long startNano;

    /**
     * The time the {@link StatisticsDatapoint#end()} operation of this {@link StatisticsDatapoint}
     * was called. UNIX time in nanoseconds.
     */
    private long endTime;

    /**
     * System.nanoTime() of the call of the the {@link StatisticsDatapoint#end()} operation of this
     * {@link StatisticsDatapoint}. Only for comparison with other nanoTimes, unit is relative to
     * some unknown origin.
     */
    private long endNano;

    /**
     * Constructor.
     */
    public StatisticsDatapoint() {
        startTime = System.currentTimeMillis();
        startNano = System.nanoTime();
    }

    @Override
    public final void end() {
        endTime = System.currentTimeMillis();
        endNano = System.nanoTime();
    }

    /**
     * Returns the timestamp when this {@link StatisticsDatapoint} was created.
     *
     * @return Timestamp in UNIX time.
     */
    public final long getStartTime() {
        return startTime;
    }

    /**
     * Returns the timestamp when the {@link StatisticsDatapoint#end()} operation of this
     * {@link StatisticsDatapoint} was called.
     *
     * @return Timestamp in UNIX time.
     */
    public final long getEndTime() {
        return endTime;
    }

    /**
     * Returns the amount of milliseconds elapsed between creation of this
     * {@link StatisticsDatapoint} and the call to its {@link StatisticsDatapoint#end()} operation.
     *
     * @return elpased time in milliseconds.
     */
    public final long getDurationMilli() {
        return endTime - startTime;
    }

    /**
     * Returns the amount of nanoseconds elapsed between creation of this
     * {@link StatisticsDatapoint} and the call to its {@link StatisticsDatapoint#end()} operation.
     * Precision can depend of the local computer/jvm, but is at least as good as with the
     * {@link StatisticsDatapoint#getDurationMilli()}.
     *
     * @return elapsed time in nanoseconds.
     */
    public final long getDurationNano() {
        return endNano - startNano;
    }

}
