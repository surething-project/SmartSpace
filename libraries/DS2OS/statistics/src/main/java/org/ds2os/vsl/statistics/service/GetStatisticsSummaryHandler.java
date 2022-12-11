package org.ds2os.vsl.statistics.service;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidValueException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.statistics.Statistics;
import org.ds2os.vsl.statistics.StatisticsDatapoint;
import org.ds2os.vsl.statistics.StatisticsProvider;

/**
 * VirtualNodeHandler responsible for calls to the getStatisticsSummaryFor virtual Node of the
 * statistics Service.
 *
 * @author liebald
 */
public class GetStatisticsSummaryHandler extends AbstractStatisticsHandler {

    /**
     * Constructor.
     *
     * @param nodeAddress
     *            The address of the virtualnode to extract parameters as suffix.
     * @param statisticsProvider
     *            The {@link StatisticsProvider} used to access KA statistics.
     * @param nodeFactory
     *            the {@link VslNodeFactory} used to construct results of the get operation.
     */
    public GetStatisticsSummaryHandler(final String nodeAddress,
            final StatisticsProvider statisticsProvider, final VslNodeFactory nodeFactory) {
        super(nodeAddress, statisticsProvider, nodeFactory);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String requestedStatistics = getVnodeParameter(address);
        final Statistics statistics = getStatistics(requestedStatistics);
        // TODO: there probably exist statistic classes that can compute stuff like
        // avg/min/max/median/var on arrays/lists/... try to use one of them for more stats
        // e.g. Apache Commons Math
        double sum = 0;
        int counter = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (final StatisticsDatapoint dp : statistics.getDatapoints()) {
            final double currentNano = dp.getDurationNano() / 1000000.0; // convert from nano to
            // milliseconds
            sum += currentNano;
            counter++;
            if (min > currentNano) {
                min = currentNano;
            }
            if (max < currentNano) {
                max = currentNano;
            }
        }

        if (counter == 0) {
            throw new InvalidValueException(
                    "No statistics datapoints found for " + requestedStatistics);
        }

        final VslMutableNode result = getNodeFactory().createMutableNode(requestedStatistics);
        result.putChild("sampleSize",
                getNodeFactory().createMutableNode(Integer.toString(counter)));
        result.putChild("minDurationMilli",
                getNodeFactory().createMutableNode(Double.toString(round(min, 3))));
        result.putChild("maxDurationMilli",
                getNodeFactory().createMutableNode(Double.toString(round(max, 3))));
        result.putChild("avgMilli",
                getNodeFactory().createMutableNode(Double.toString(round(sum / counter, 3))));
        return result;
    }

}
